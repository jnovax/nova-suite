# FamilyTime: phục hồi trạng thái screensaver sau khi service khởi động lại

## Mục tiêu và giới hạn

FamilyTime không trừ thời gian hoặc hiển thị Time-Lock trên screensaver đã xác nhận, và áp dụng lại giới hạn khi ứng dụng thuộc diện theo dõi thực sự trở lại. Việc service bị kill/restart, launcher bị kill, tắt màn hình, hoặc bỏ lỡ một broadcast không được tạo trạng thái tạm dừng vô hạn. Không can thiệp navigation/volume/power trong accessibility service.

Android 14 không cung cấp API công khai cho ứng dụng thường đọc trạng thái dream toàn hệ thống: `IDreamManager.isDreaming()` là API ẩn, yêu cầu `READ_DREAM_STATE` (`signature|privileged`). `PowerManager.isInteractive()` không phân biệt dream. Vì vậy không thể đảm bảo đồng thời cả hai điều kiện ở trên với mọi screensaver của hãng chỉ bằng hai APK thông thường. Giải pháp có thể đảm bảo cho screensaver của launcher; với screensaver bên thứ ba sau restart phải giữ trạng thái chưa biết hoặc cần thành phần có quyền hệ thống do OEM cung cấp. Không dùng root, shell ADB, reflection/API ẩn hoặc giữ cờ `dreaming=true` lâu dài làm nguồn chân lý.

## Phương án đề xuất

1. **Nguồn trạng thái của launcher.** Thêm hợp đồng IPC chỉ đọc, có phiên bản trong `core/contract`; launcher công bố trạng thái `ACTIVE`, `INACTIVE`, hoặc `UNKNOWN` cho cả `ScreenSaverActivity` và `ClockDream`, kèm loại nguồn, mã phiên và số thứ tự. Trạng thái nằm trong tiến trình sở hữu màn hình và gắn với lifecycle; khi mất tiến trình, kết nối IPC chết thì FamilyTime quay về `UNKNOWN`, không coi giá trị cũ là đang hoạt động. Không lưu cờ active làm sự thật qua reboot.
2. **Đồng bộ không mất sự kiện.** FamilyTime đăng ký observer/bind trước khi truy vấn snapshot; tất cả thay đổi và snapshot được tuần tự hóa, bỏ dữ liệu cũ theo phiên/số thứ tự. Nếu process chết, provider chưa sẵn sàng, hoặc kết quả mâu thuẫn, chuyển `UNKNOWN` và đồng bộ lại. Khi launcher khởi tạo process mới, trạng thái mặc định là `UNKNOWN` cho đến khi lifecycle được xác nhận; không suy ra `INACTIVE` chỉ từ việc provider mới khởi động. Chỉ sử dụng `ACTIVE` từ launcher khi màn hình tương ứng còn đang thực sự hiển thị.
3. **Giữ broadcast Android.** Khi service còn chạy, tiếp tục dùng `ACTION_DREAMING_STARTED/STOPPED` cho dream hệ thống, kiểm tra khả năng nhận trên emulator và thiết bị; khởi tạo lại là `UNKNOWN`, không tự diễn giải broadcast cũ. Phân biệt dream hệ thống với Activity của launcher để preview/idle không phụ thuộc broadcast. `SCREEN_OFF` là trạng thái riêng, không dùng `isInteractive()` để suy ra dream.
4. **Trạng thái UNKNOWN và bảo vệ giới hạn.** Không dùng `UNKNOWN` để miễn giới hạn: chỉ coi app được theo dõi là đang hiển thị khi có bằng chứng foreground cập nhật; khi đã xác nhận app theo dõi ở foreground, kiểm tra lại hạn mức ngay trước khi cho phép tiếp tục hoặc hiển thị Time-Lock. Không suy ra trạng thái chỉ từ tên package đã lưu hoặc một cửa sổ system/IME. Nếu chưa xác nhận được cửa sổ, báo trạng thái chưa xác định và không tuyên bố đo chính xác; không chặn vĩnh viễn hay cho phép xem vô hạn dựa trên dữ liệu cũ.
5. **Phạm vi hệ thống.** Để đảm bảo đầy đủ trên thiết bị thường, người dùng chọn dream của launcher trong cài đặt Android; không tự đổi cài đặt hệ thống. Nếu bắt buộc đảm bảo cả dream OEM không hợp tác, chỉ có phương án tích hợp OEM/system component có quyền đọc trạng thái dream và hợp đồng cập nhật tin cậy; không coi heuristic accessibility hay `isInteractive()` là tương đương. Trên ROM không có nguồn tin cậy, công bố giới hạn thay vì gọi giải pháp là triệt để.
6. **Bảo mật IPC.** IPC không cho ứng dụng khác ghi trạng thái. Xác thực định danh package/UID và chứng chỉ khi cần; tránh broadcast không bảo vệ có thể giả mạo để ngừng tính giờ. Hai ứng dụng hiện được cấu hình ký bằng cùng khóa launcher, cho phép thiết kế permission `signature` chung khi cài bản đã chuyển đổi. FamilyTime dùng khóa cũ phải được di chuyển/cài lại trước; không thay đổi chữ ký APK trên TV thật mà chưa có phương án dữ liệu phù hợp. Khóa hiện tại là khóa debug, do đó không coi nó là danh tính bảo mật phát hành mạnh.

## Thực hiện và nghiệm thu

- Tạo test trạng thái/IPC: start → restart FamilyTime trong lúc Activity hoặc `ClockDream` hiển thị → khôi phục active; stop → resume app theo dõi → trừ giờ/khóa đúng.
- Test các race: đăng ký observer đồng thời thay đổi trạng thái; launcher process chết; FamilyTime chết trong khi dream kết thúc; mất kết nối IPC; provider khởi động lại; thay đổi thứ tự sự kiện; reboot/screen-off; dữ liệu phiên cũ. Không được dùng cờ cũ để bỏ khóa vô hạn.
- Test app OEM/Google dream: broadcast start/stop khi service còn sống; restart trong lúc dream OEM đang chạy phải được đánh dấu `UNKNOWN` trừ khi có nguồn hệ thống đáng tin; xác nhận Time-Lock và tính giờ khi quay về ứng dụng thực sự theo dõi.
- Test Kid Mode, hạn mức bằng 0, overlay đã mở, parent mode, mất quyền accessibility và service được bật lại; nút điều hướng/âm lượng giữ repeat tự nhiên.
- Chạy toàn bộ unit test, build release, sau đó kiểm thử emulator và ít nhất một TV mục tiêu với snapshot; ghi kết quả, loại dream và ROM. Tích hợp OEM chỉ được tuyên bố hỗ trợ sau khi test quyền và vòng đời trên ROM cụ thể.

## Nguồn kỹ thuật

- Android 14 `DreamManagerService.isDreaming()` kiểm tra `READ_DREAM_STATE`: https://github.com/aosp-mirror/platform_frameworks_base/blob/android-14.0.0_r1/services/core/java/com/android/server/dreams/DreamManagerService.java
- `READ_DREAM_STATE` là `signature|privileged`: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android14-release/core/res/AndroidManifest.xml
- `isInteractive()` vẫn có thể true khi dreaming: https://developer.android.com/reference/android/os/PowerManager#isInteractive()
