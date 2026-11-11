# Build & Development Environment Specification

Tài liệu đặc tả môi trường phát triển và cấu hình đường dẫn cho **Apple TV Launcher**.

---

## 🛠️ Cấu hình biến môi trường chuẩn (Environment Variables)

Hệ thống sử dụng các đường dẫn cố định đã cài đặt trên môi trường Linux:

```bash
# Java Development Kit (OpenJDK 17.0.21)
export JAVA_HOME=/opt/jdk17

# Android SDK
export ANDROID_HOME=/opt/android-sdk

# System PATH
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
```

---

## 📁 Cấu hình Project (`local.properties`)

Tệp `local.properties` tại thư mục gốc dự án phải trỏ đúng đến Android SDK:

```properties
sdk.dir=/opt/android-sdk
```

---

## 🚀 Kiểm tra môi trường & Lệnh biên dịch

### 1. Kiểm tra phiên bản Java và Android SDK
```bash
$JAVA_HOME/bin/java -version
# OpenJDK Runtime Environment (build 17.0.21+...)

adb version
# Android Debug Bridge version 1.0.41...
```

### 2. Biên dịch Release APK
```bash
./gradlew assembleRelease
```
Binary APK sau khi biên dịch nằm tại:
- `apps/launcher/build/outputs/apk/release/launcher-release.apk`
- `apps/parental-control/build/outputs/apk/release/parental-control-release.apk`

### Khóa ký chung

Launcher và FamilyTime (cả debug và release) dùng cùng cấu hình trong `gradle/unified-signing.gradle`, trỏ đến `apps/launcher/debug.keystore` hiện có. Giữ bản sao an toàn của chính tệp khóa này: tệp bị Git bỏ qua, và một khóa mới dù có cùng tên sẽ không cập nhật được các APK đã cài. Đây là khóa debug hiện có, không phải khóa phát hành riêng được bảo vệ.

FamilyTime đã cài bằng khóa debug cũ không thể cập nhật trực tiếp sang khóa launcher. Trên emulator, khi chấp nhận xóa dữ liệu FamilyTime, có thể build release rồi dùng `./tools/reinstall_familytime_emulator.sh DEVICE --erase-data`. Công cụ chỉ nhận emulator, lưu snapshot và APK cũ, sau đó cài lại và khôi phục trạng thái bật dịch vụ Accessibility trước đó. Cài lại sẽ xóa PIN, cài đặt và dữ liệu của FamilyTime. Trên TV thật, cần lập phương án di chuyển dữ liệu và ký phù hợp trước khi thay đổi.

Sau khi cài, `./tools/verify_unified_signing.sh DEVICE` tải APK đang cài về tạm thời và kiểm tra cả hai chứng chỉ với APK launcher vừa build. Trên WSL, sử dụng cùng ADB server và đường kết nối emulator đã chọn như trong `docs/SIMULATION.md`.

---

## 🧪 Môi trường Simulation & Giả lập

Chi tiết xem tại [`docs/SIMULATION.md`](SIMULATION.md):
- **Android TV Emulator (ADB):** `./tools/deploy.sh all [DEVICE_ID]`
