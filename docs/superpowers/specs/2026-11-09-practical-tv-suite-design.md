# Thiết kế Kiến trúc: Bộ Tiện ích Thực dụng Siêu nhẹ (Zero-Bloat Practical TV Suite)

* **Ngày lập:** 2026-11-09
* **Dự án:** `nova-tv-launcher`
* **Phạm vi tác động:** `apps/launcher` (`com.appletv.launcher`)
* **Mục tiêu dung lượng & hiệu năng:** Tăng thêm < 15 KB APK, 0 dịch vụ chạy ngầm mới, 0% CPU khi nhàn rỗi, bảo toàn 100% tốc độ lặp phím phần cứng (`InputDispatcher`).

---

## 1. Bối cảnh & Mục tiêu (Background & Goals)

### 1.1. Bối cảnh
`apps/launcher` hiện là một Launcher chuẩn Apple TV tvOS 18 siêu nhẹ (~218 KB), khởi động tức thì và tích hợp chặt chẽ với `apps/parental-control` thông qua `core/contract`. Tuy nhiên, trong quá trình sử dụng hàng ngày bằng điều khiển từ xa (Remote D-Pad), người dùng gặp một số giới hạn:
1. Muốn ghim/bỏ ghim ứng dụng trên thanh Dock, đổi vị trí ứng dụng trên Dock hoặc ẩn ứng dụng ít dùng phải chỉnh sửa file `config.json` qua máy tính (ADB).
2. Nhấn giữ `OK` trên ô ứng dụng hiện tại mở thẳng trang *System App Info* của Android, dẫn đến nguy cơ trẻ nhỏ trong `Kid Mode` có thể bấm *Force Stop* hoặc *Uninstall* ứng dụng.
3. Trung tâm điều khiển nhanh (`QuickSettingsActivity`) mới chỉ có 4 lối tắt tĩnh, thiếu các tiện ích thiết thực vào buổi tối như **Hẹn giờ tắt TV (Sleep Timer)**, **Chế độ dịu mắt ban đêm (Night Dimmer)**, **Mở nhanh Bluetooth/Âm thanh** và **Bật màn hình chờ (Screensaver)**.
4. Khi cắm ổ USB/ổ cứng ngoài vào TV để xem phim, không có chỉ báo trực quan hay lối tắt mở nhanh trình phát video (`Nova Video Player`) và trình tháo USB an toàn.
5. Việc gán lại chức năng cho các phím cứng trên Remote đang đòi hỏi người dùng biết dùng `adb logcat` để tìm `keyCode`/`scanCode`.

### 1.2. Mục tiêu thiết kế
1. **Menu Ngữ cảnh Nhấn giữ `OK` trên Ứng dụng (tvOS Long-Press Context Menu):** Cho phép Ghim/Bỏ ghim thanh Dock, Di chuyển Trái/Phải trên Dock, Ẩn ứng dụng và mở App Info trực tiếp bằng Remote.
2. **Nâng cấp Control Center (`QuickSettingsActivity`):** Tích hợp **Sleep Timer** (tự động dừng phát Media và tắt TV về Standby khi hết giờ), **Night Dimmer** (lớp phủ giảm chói ban đêm 25%/50%/70%), lối tắt **Âm thanh & Bluetooth**, và **Bật ngay Screensaver**.
3. **Nút `[ 💾 USB ]` Tự động trên Top Bar:** Tự động hiện trên thanh Menu Bar khi cắm USB/ổ cứng ngoài; bấm vào mở menu kính mờ chọn giữa **Xem phim (`Nova Video Player`)** và **Quản lý / Tháo USB an toàn (`Storage Settings`)**.
4. **Trình Tự học & Gán phím Remote (`Interactive Remote Key Mapper`) & Quản lý Ứng dụng đã ẩn:** Tích hợp trực tiếp vào `LauncherSettingsActivity`.
5. **Khóa bảo vệ toàn diện trong `Kid Mode`:** Toàn bộ tính năng chỉnh sửa bố cục (Menu nhấn giữ `OK`, Ghim/Di chuyển Dock, Ẩn/Hiện ứng dụng, Gán phím Remote, Cài đặt Launcher) **chỉ cho phép thực hiện khi đang ở `Parent Mode`**.

---

## 2. Kiến trúc Tổng thể & Tầng Lưu trữ (`Config` & `SharedPreferences`)

### 2.1. Hợp nhất Lưu trữ trong `SharedPreferences("launcher_settings")`
Không tạo cơ sở dữ liệu SQLite hay thư viện ngoài. Mở rộng `Config.java` để hợp nhất 3 tầng cấu hình theo thứ tự ưu tiên tăng dần:
1. Mặc định biên dịch (`res/values/config.xml`).
2. File cấu hình chủ đề (`config.json` trong `/sdcard/Android/data/com.appletv.launcher/files/`).
3. Tùy chỉnh trực tiếp bằng Remote trên TV (`SharedPreferences` tên `"launcher_settings"`):
   * `dock_apps_override` (`String`): Danh sách các package trên thanh Dock phân cách bằng dấu phẩy `,` (ví dụ: `"io.gh.reisxd.tizentube.cobalt,vn.mytvnet.b2cott,org.courville.nova"`).
   * `hidden_apps_override` (`String`): Danh sách các package bị ẩn phân cách bằng dấu phẩy `,`.
   * `remote_keys_override` (`String` dạng JSON gọn): Chuỗi ánh xạ `"keyCode:target"` phân cách bằng dấu chấm phẩy `;` hoặc `JSONObject` (ví dụ: `{"4062":"org.courville.nova","190":"wallpaper_cycle"}`).
   * `night_dimmer_level` (`int`): Mức độ làm tối màn hình (`0`, `25`, `50`, `70`).
   * `sleep_timer_end_ms` (`long`): Mốc thời gian (`SystemClock.elapsedRealtime()` hoặc `System.currentTimeMillis()`) sẽ tắt TV (`0` nếu đang tắt).

Khi người dùng bấm **"Follow Theme (Khôi phục theo giao diện gốc)"** trong `LauncherSettingsActivity`, Launcher sẽ xóa đồng thời `dock_apps_override`, `hidden_apps_override` và `remote_keys_override` để đưa mọi thứ về đúng chuẩn ban đầu.

### 2.2. Tận dụng `HomeKeyService` làm Nhân Điều phối Hệ thống
`HomeKeyService` là một `AccessibilityService` luôn hoạt động khi người dùng bật tính năng phím tắt Remote:
* Có sẵn quyền hiển thị lớp phủ `WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY` trên toàn hệ thống mà không cần xin quyền `SYSTEM_ALERT_WINDOW` phức tạp.
* Có sẵn quyền gọi `performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)` (Android 9.0+ API 28+) để đưa TV vào trạng thái **Standby (Tắt màn hình/nguồn chờ)** khi hết giờ Sleep Timer.
* Giao tiếp từ `QuickSettingsActivity` sang `HomeKeyService` thông qua `Intent` nội bộ hoặc `SharedPreferences.OnSharedPreferenceChangeListener` kết hợp Action trong `HomeKeyService`.

---

## 3. Thiết kế Chi tiết Từng Tính năng

### 3.1. Tính năng 1: Menu Ngữ cảnh Nhấn giữ `OK` trên Ứng dụng (`MainActivity` & `AppAdapter`)

#### Phân quyền Chế độ (`Kid Mode` vs `Parent Mode`)
* Khi người dùng nhấn giữ `OK` (`OnLongClickListener`) trên một ô ứng dụng trong `AppAdapter`:
  * Kiểm tra `ModeManager.isKidMode(ctx)`:
    * Nếu đang ở **`Kid Mode`**: Chặn hoàn toàn thao tác nhấn giữ (trả về `true`, không mở menu, không mở System App Info).
    * Nếu đang ở **`Parent Mode`**: Gọi callback `TileListener.onTileLongPressed(int position, View anchor, Entry entry)` về `MainActivity`.

#### Giao diện Menu Ngữ cảnh (`glassPopup`)
* `MainActivity` tái sử dụng hàm `glassPopup()` và `dropIn()` để hiển thị menu kính mờ ngay tại ô ứng dụng đang chọn:
  1. **📌 Ghim lên thanh Dock** *(Hiển thị khi ứng dụng chưa nằm trong `dockApps` và số lượng ứng dụng trên Dock `< columns`)* **HOẶC** **📌 Bỏ ghim khỏi thanh Dock** *(Hiển thị khi ứng dụng đang nằm trong `dockApps`)*.
  2. **⬅️ Di chuyển sang Trái** *(Hiển thị khi ứng dụng nằm trên thanh Dock ở vị trí `index > 0`)*.
  3. **➡️ Di chuyển sang Phải** *(Hiển thị khi ứng dụng nằm trên thanh Dock ở vị trí `index < dockedCount - 1`)*.
  4. **🙈 Ẩn ứng dụng** *(Không hiển thị đối với ứng dụng Quản lý Trẻ em `vn.familytv.parentalcontrol` hoặc chính Launcher để tránh khóa nhầm)*.
  5. **ℹ️ Thông tin ứng dụng (App Info)** *(Mở `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` cho package đó để người dùng có thể Force Stop hoặc Xóa Cache khi cần)*.
* Sau mỗi thao tác Ghim / Bỏ ghim / Di chuyển / Ẩn:
  * Cập nhật `SharedPreferences("launcher_settings")` qua helper trong `Config`.
  * Gọi `reloadTiles()` và giữ nguyên focus tại ứng dụng vừa thao tác (hoặc ô kế cận nếu vừa ẩn ứng dụng).

---

### 3.2. Tính năng 2 & 3: Trung tâm Tiện ích (`QuickSettingsActivity`) — Sleep Timer, Night Dimmer, Bluetooth, Screensaver

#### Danh sách các dòng trong `QuickSettingsActivity`
1. **🌙 Hẹn giờ tắt TV (Sleep Timer):**
   * Trạng thái xoay vòng khi bấm `OK`: `Tắt` ➔ `15 phút` ➔ `30 phút` ➔ `60 phút` ➔ `90 phút` ➔ `Tắt`.
   * Nhãn trạng thái bên phải (`R.id.status`): Hiển thị `Tắt` hoặc số phút còn lại (`Còn 28 phút`).
   * **Cơ chế thực thi trong `HomeKeyService`:**
     * Khi đặt hẹn giờ `M` phút, lưu `sleep_timer_end_ms = System.currentTimeMillis() + M * 60_000L` và gửi Action `ACTION_UPDATE_OVERLAY_AND_TIMER` tới `HomeKeyService`.
     * `HomeKeyService` đặt `Handler.postDelayed` đến đúng thời điểm hẹn giờ.
     * Khi hết giờ:
       1. Gửi sự kiện phím `KeyEvent.KEYCODE_MEDIA_STOP` qua `AudioManager.dispatchMediaKeyEvent()` để dừng mọi video/nhạc đang phát.
       2. Đưa TV về màn hình chính (`Intent.ACTION_MAIN` + `CATEGORY_HOME`).
       3. Sau 800ms, gọi `performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)` để tắt TV về chế độ chờ (Standby). Nếu phần cứng TV không hỗ trợ khóa màn hình, kích hoạt `ClockDream` / màn hình đen tuyền làm phương án dự phòng.
2. **🔅 Dịu mắt ban đêm (Night Dimmer):**
   * Trạng thái xoay vòng khi bấm `OK`: `Tắt` ➔ `25%` ➔ `50%` ➔ `70%` ➔ `Tắt`.
   * **Cơ chế thực thi trong `HomeKeyService`:**
     * Khi mức độ `> 0`: Tạo một `View` đơn giản có màu nền `#000000` với `alpha = level / 100f`, gắn vào `WindowManager` với:
       * `type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY` (fallback sang `TYPE_APPLICATION_OVERLAY` nếu cần).
       * `flags = FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCHABLE | FLAG_LAYOUT_IN_SCREEN | FLAG_LAYOUT_NO_LIMITS`.
       * `format = PixelFormat.TRANSLUCENT`.
     * Khi mức độ `= 0`: Gỡ `View` khỏi `WindowManager` và giải phóng tham chiếu (`dimmerView = null`), tiêu thụ **0 byte RAM**.
3. **🎧 Âm thanh & Bluetooth:**
   * Bấm `OK` mở nhanh cài đặt phụ kiện/Bluetooth theo chuỗi fallback an toàn:
     1. `Intent(Settings.ACTION_BLUETOOTH_SETTINGS)`
     2. Component `com.android.tv.settings/.accessories.AddAccessoryActivity` hoặc `com.android.tv.settings/.MainSettings`
     3. `Intent(Settings.ACTION_SOUND_SETTINGS)` / `Intent(Settings.ACTION_SETTINGS)`.
4. **✨ Bật màn hình chờ (Start Screensaver):**
   * Bấm `OK` kích hoạt ngay lập tức chế độ Screensaver hệ thống (`Intent(Intent.ACTION_MAIN).setClassName("com.android.systemui", "com.android.systemui.Somnambulator")`), nếu ROM TV đã gỡ `Somnambulator` thì mở trực tiếp màn hình đồng hồ tối của Launcher.
5. **🖼️ Hình nền 4K (`ACTION_SHOW_WALLPAPERS`):** Giữ nguyên.
6. **📺 Chế độ Hình ảnh (`Settings.ACTION_DISPLAY_SETTINGS`):** Giữ nguyên.
7. **⚙️ Cài đặt Launcher (`ACTION_LAUNCHER_SETTINGS`):** Chỉ hiển thị khi đang ở **`Parent Mode`** (`!ModeManager.isKidMode(this)`).
8. **🔧 Cài đặt Hệ thống TV (`Settings.ACTION_SETTINGS`):** Giữ nguyên.

> *Lưu ý trải nghiệm:* Đối với 2 dòng xoay vòng tại chỗ (**Hẹn giờ tắt TV** và **Dịu mắt ban đêm**), khi bấm `OK` sẽ cập nhật trạng thái ngay trên dòng đó mà **không đóng bảng Quick Settings**, giúp người dùng bấm chọn mức mong muốn dễ dàng. Các dòng mở màn hình khác sẽ đóng Quick Settings sau khi mở.

---

### 3.3. Tính năng 4: Nút `[ 💾 USB ]` Tự động trên Top Bar (`MainActivity`)

#### Phát hiện thiết bị lưu trữ USB
* Thêm nút `btnUsb` (`style="@style/TopButton"`, `android:visibility="gone"`, icon `R.drawable.ic_usb` vector gọn nhẹ < 500 bytes) nằm ngay bên trái nút `btnInputs` trên thanh Menu Bar góc phải của `activity_main.xml`.
* Hàm `updateUsbButton()` kiểm tra thông qua `StorageManager`:
  * Duyệt danh sách `StorageManager.getStorageVolumes()`.
  * Nếu có bất kỳ `StorageVolume` nào thỏa mãn `volume.isRemovable() && Environment.MEDIA_MOUNTED.equals(volume.getState())`:
    * Hiển thị `btnUsb.setVisibility(View.VISIBLE)`.
    * Nhãn nút: `USB` (hoặc tên ngắn gọn nếu muốn, mặc định giữ chữ `USB` cho đồng bộ với `Inputs`).
  * Nếu không có USB nào đang gắn: `btnUsb.setVisibility(View.GONE)`.
* Lắng nghe `BroadcastReceiver` cho các action `Intent.ACTION_MEDIA_MOUNTED`, `ACTION_MEDIA_UNMOUNTED`, `ACTION_MEDIA_EJECT`, `ACTION_MEDIA_REMOVED` (với `addDataScheme("file")`) khi `MainActivity` đang chạy (`onStart`/`onStop`) để nút `[ 💾 USB ]` tự động hiện lên ngay lập tức khi vừa cắm USB và biến mất ngay khi rút ra.

#### Menu Ngữ cảnh khi bấm vào `[ 💾 USB ]`
* Bấm `OK` vào `btnUsb` sẽ xổ xuống `glassPopup` ngay dưới nút `btnUsb` (tương tự `showInputs()`) gồm 2 dòng:
  1. **🎬 Xem phim & Video (`Nova Video Player`)**:
     * Ưu tiên mở `org.courville.nova` (Nova Video Player).
     * Nếu máy không cài Nova, tự động tìm ứng dụng xem video/file phổ biến (`org.videolan.vlc`, `org.xbmc.kodi`, `com.ghisler.android.TotalCommander`) hoặc fallback về `Settings.ACTION_INTERNAL_STORAGE_SETTINGS`.
  2. **⏏️ Bộ nhớ & Tháo USB an toàn**:
     * Mở `new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)` (fallback sang `Settings.ACTION_MEMORY_CARD_SETTINGS` / `Settings.ACTION_SETTINGS`) để người dùng xem dung lượng hoặc chọn *Eject / Tháo an toàn*.

---

### 3.4. Tính năng 5: Quản lý Ứng dụng Đã ẩn & Gán phím Remote Trực tiếp (`LauncherSettingsActivity`)

#### Bảo vệ `Parent Mode`
* Trong `LauncherSettingsActivity.onCreate()`, kiểm tra `if (ModeManager.isKidMode(this)) { finish(); return; }` để đảm bảo trẻ em không bao giờ truy cập được vào màn hình cài đặt này.

#### Mục "Ứng dụng đã ẩn (Hidden Apps)"
* Thêm một hàng `hiddenAppsChoice` trong `activity_launcher_settings.xml` hiển thị: `Ứng dụng đã ẩn: N`.
* Khi bấm vào:
  * Nếu `N == 0`: Hiện thông báo ngắn *"Chưa có ứng dụng nào bị ẩn (Nhấn giữ OK trên ứng dụng ở màn hình chính để ẩn)"*.
  * Nếu `N > 0`: Hiển thị `AlertDialog` danh sách các ứng dụng đang ẩn (hiển thị tên ứng dụng + package). Bấm `OK` vào ứng dụng nào sẽ lập tức **Bỏ ẩn (Unhide)** ứng dụng đó và cập nhật lại màn hình chính.

#### Mục "Gán phím điều khiển (Remote Key Mapper)"
* Thêm một hàng `remoteKeysChoice` trong `activity_launcher_settings.xml` hiển thị: `Gán phím điều khiển (Remote)`.
* Khi bấm vào, mở hộp thoại quản lý phím tắt gồm:
  * **➕ Thêm / Gán phím mới:**
    1. Mở hộp thoại lắng nghe phím: *"Hãy bấm phím trên điều khiển mà bạn muốn gán chức năng (Nhấn Back để hủy)..."*
    2. Bắt sự kiện trong `OnKeyListener`:
       * **Danh sách phím bảo vệ không bao giờ cho ghi đè:** `KEYCODE_DPAD_UP`, `KEYCODE_DPAD_DOWN`, `KEYCODE_DPAD_LEFT`, `KEYCODE_DPAD_RIGHT`, `KEYCODE_DPAD_CENTER`, `KEYCODE_ENTER`, `KEYCODE_BACK`, `KEYCODE_HOME`, `KEYCODE_VOLUME_UP`, `KEYCODE_VOLUME_DOWN`, `KEYCODE_VOLUME_MUTE`, `KEYCODE_POWER`.
       * Với mọi phím khác (ví dụ phím Netflix `4062`, YouTube `190`, VTVgo `4168`, phím màu Đỏ/Xanh/Vàng/Lam, phím kênh...): Ghi nhận mã phím (`keyCode > 0 ? keyCode : scanCode`).
    3. Hiển thị danh sách hành động để chọn cho phím vừa bấm:
       * 🖼️ **Đổi hình nền 4K tiếp theo** (`wallpaper_cycle`)
       * 🎨 **Mở bộ chọn hình nền 4K** (`wallpaper_picker`)
       * ⚡ **Mở Trung tâm điều khiển nhanh** (`quick_settings`)
       * 🔄 **Mở Đa nhiệm (Recent Apps)** (`recent_apps`)
       * 🔌 **Mở danh sách Cổng kết nối (HDMI / Inputs)** (`inputs`)
       * 📱 **Mở ứng dụng: [Danh sách các ứng dụng đang cài trên TV]** (`<package_name>`)
       * 🗑️ **Xóa gán phím này (Về mặc định)**
  * **📋 Danh sách các phím đang gán:** Hiển thị rõ `Phím #190 ➔ YouTube`, `Phím #4001 ➔ Đổi hình nền`... để người dùng dễ kiểm tra hoặc bấm vào để sửa/xóa.
* Ngay sau khi lưu vào `SharedPreferences`, `HomeKeyService` sẽ tự động áp dụng ngay lập tức mà không cần khởi động lại TV.

---

## 4. Kế hoạch Kiểm thử & Xác minh (Verification Plan)

1. **Unit Tests (`./gradlew :apps:launcher:testDebugUnitTest`):**
   * `ConfigOverrideTest`: Kiểm thử lưu/đọc/xóa `dock_apps_override`, `hidden_apps_override`, `remote_keys_override`, và thao tác dịch chuyển vị trí Trái/Phải trên thanh Dock.
   * `QuickSettingsCycleTest`: Kiểm thử chu kỳ xoay vòng mức **Sleep Timer** (`0 -> 15 -> 30 -> 60 -> 90 -> 0`) và **Night Dimmer** (`0 -> 25 -> 50 -> 70 -> 0`).
   * `RemoteKeyGuardTest`: Kiểm thử bộ lọc phím bảo vệ (đảm bảo D-Pad, OK, Back, Home, Volume không bao giờ bị ghi đè bởi trình học phím).
   * `KidModeProtectionTest`: Kiểm thử khóa chỉnh sửa bố cục khi ở `Kid Mode`.
2. **Build Verification (`./gradlew assembleRelease`):**
   * Kiểm tra toàn bộ Monorepo build thành công, APK `apps/launcher` giữ dung lượng siêu nhẹ (~220-230 KB).
3. **On-Device Verification (trên TV thực tế):**
   * Kiểm tra nhấn giữ `OK` trên ứng dụng ở `Parent Mode` vs `Kid Mode`.
   * Kiểm tra bật thử **Night Dimmer** và hẹn giờ **Sleep Timer** trong `QuickSettingsActivity`.
   * Kiểm tra hộp thoại học phím Remote và quản lý ứng dụng ẩn trong `LauncherSettingsActivity`.
