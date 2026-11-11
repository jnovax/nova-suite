# Hướng dẫn Triển khai & Kiểm thử Môi trường Simulation (Giả lập)

Tài liệu này chuẩn hóa toàn bộ quy trình và lệnh chạy cho môi trường **Simulation & Emulator** của dự án `nova-tv-launcher` và `apps/parental-control`, phục vụ cho cả Developer và AI Agents.

---

## 1. Android TV Emulator / Thiết bị Giả lập qua ADB

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

## 2. Triển khai lên TV thật (Hardware Target)

Để đối chiếu và triển khai thực tế trên phần cứng Android TV (Sony, TCL, Xiaomi, Shield, Chromecast):

```bash
# Target IP TV TCL mặc định:
./tools/deploy.sh all 192.168.1.7:5555

# Target IP TV phụ:
./tools/deploy.sh all 192.168.1.10:5555
```
