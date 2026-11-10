# Hướng dẫn Triển khai & Kiểm thử Môi trường Simulation (Giả lập)

Tài liệu này chuẩn hóa toàn bộ quy trình và lệnh chạy cho môi trường **Simulation & Emulator** của dự án `nova-tv-launcher` và `apps/parental-control`, phục vụ cho cả Developer và AI Agents.

---

## 1. Web Simulation tương tác (`docs/preview/index.html`)

Dự án cung cấp một bản mô phỏng trực quan chuẩn tvOS 18 chạy hoàn toàn trên nền Web (HTML5/CSS3/Vanilla JS) với độ phân giải mô phỏng 4K UHD (`3840x2160` / `1920x1080`), hỗ trợ đầy đủ điều hướng Remote/D-pad và các tính năng kiểm soát phụ huynh FamilyTime.

### 🚀 Lệnh khởi động Web Server
Chạy lệnh HTTP server nội bộ (không phụ thuộc external framework):

```bash
# Khởi động server tại cổng 8888
python3 -m http.server 8888 --directory docs/preview
```

### 🌐 Truy cập Web Simulation
- **URL Local:** [http://localhost:8888/](http://localhost:8888/) hoặc [http://127.0.0.1:8888/index.html](http://127.0.0.1:8888/index.html)
- **Nếu chạy trên WSL2 / Remote Dev:** Mở trình duyệt trên máy Host tại địa chỉ `http://localhost:8888/` (WSL tự động ánh xạ port).

### 🎮 Phím tắt điều khiển Remote trên Web Simulation
| Phím trên Bàn phím | Nút tương ứng trên TV Remote | Chức năng |
| :--- | :--- | :--- |
| **Mũi tên (← ↑ → ↓)** | D-Pad Navigation | Di chuyển focus giữa các icon app, platter dock và top bar pill |
| **Enter** / **Space** | OK / Select | Chọn mở app, kích hoạt pill, bấm phím số PIN/OTP |
| **Escape** / **Backspace** | Back | Thoát modal popup, đóng Control Center, lùi menu |
| **H** | Home | Quay về màn hình chính Apple TV Launcher |
| **Lâu giữ Enter (Long OK)** | Long-Press OK | Mở menu ngữ cảnh tvOS (Pin/Unpin, Reorder, Hide App, Info) |

### 🛡️ Các kịch bản kiểm thử mô phỏng có sẵn
- **Chuyển đổi Profile Mode:** Click vào Pill `[ 🔒 Parent ]` hoặc `[ 🧒 12m ]` trên thanh Top Bar để đổi chế độ hoặc mở Cài đặt FamilyTime.
- **Bàn phím nhập mã OTP/PIN:** Mô phỏng D-pad keypad nhập 6 số Google Authenticator TOTP với nút auto-fill để test nhanh.
- **Eye-Rest Quiz Break:** Tự động kích hoạt màn hình đếm ngược nghỉ mắt và câu hỏi kiểm tra sau chu kỳ xem TV.
- **Control Center & Quick Settings:** Thử nghiệm Sleep Timer, Night Dimmer overlay, và chuyển đổi hình nền 4K.

---

## 2. Android TV Emulator / Thiết bị Giả lập qua ADB

Khi chạy máy ảo Android TV (ví dụ AVD `MyGoogleTV` trên Android Studio / QEMU Windows):

### 💡 Đặc thù môi trường WSL2 & Windows Host:
- Máy ảo chạy trên Windows Host (`D:\Android\emulator\emulator.exe -avd MyGoogleTV`) và lắng nghe cổng `emulator-5554`.
- Công cụ ADB trên Windows nằm tại: `/mnt/d/Android/platform-tools/adb.exe` (hoặc `D:\Android\platform-tools\adb.exe`).
- Script `./tools/deploy.sh` đã được cấu hình **tự động phát hiện** `emulator-5554` qua Windows ADB và cài đặt trực tiếp mà không cần can thiệp thủ công!

### 🛠️ Thiết lập biến môi trường chuẩn (trên WSL)
```bash
export JAVA_HOME="/opt/jdk17"
export ANDROID_HOME="/opt/android-sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

### 🔌 Kiểm tra & Khởi động kết nối Emulator
Nếu máy ảo vừa bật mà ADB daemon chưa nhận:
```bash
# Khởi động adb daemon trên host và kiểm tra danh sách thiết bị:
/mnt/d/Android/platform-tools/adb.exe devices
# Kết quả hiển thị: emulator-5554   device
```

### 📦 Lệnh Build & Deploy APKs 1-Click lên Máy ảo
Script `./tools/deploy.sh` sẽ tự động nhận diện `emulator-5554`:

```bash
# Triển khai cả 2 ứng dụng (Launcher + Parental Control) lên máy ảo:
./tools/deploy.sh all

# Triển khai riêng Launcher:
./tools/deploy.sh launcher

# Triển khai riêng Parental Control:
./tools/deploy.sh parental-control
```
```
*(Nếu không truyền `TARGET_DEVICE`, script sẽ tự động lấy thiết bị đầu tiên tìm thấy từ `adb devices`).*

### ⚡ Lệnh kiểm tra & khởi chạy Activity trực tiếp qua ADB
```bash
# Mở Apple TV Launcher
adb -s <TARGET> shell am start -n com.appletv.launcher/.MainActivity

# Mở FamilyTime Parental Control Settings
adb -s <TARGET> shell am start -n vn.familytv.parentalcontrol/.MainActivity

# Kích hoạt màn hình nhập mã xác thực OTP/PIN
adb -s <TARGET> shell am start -a vn.familytv.parentalcontrol.ACTION_AUTHENTICATE

# Khóa ngay về Kid Mode qua broadcast IPC
adb -s <TARGET> shell am broadcast -a vn.familytv.parentalcontrol.ACTION_LOCK_KID -p vn.familytv.parentalcontrol
```

---

## 3. Triển khai lên TV thật (Hardware Target)

Để đối chiếu và triển khai thực tế trên phần cứng Android TV (Sony, TCL, Xiaomi, Shield, Chromecast):

```bash
# Target IP TV TCL mặc định:
./tools/deploy.sh all 192.168.1.7:5555

# Target IP TV phụ:
./tools/deploy.sh all 192.168.1.10:5555
```
