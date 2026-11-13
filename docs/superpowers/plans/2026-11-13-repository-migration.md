# Nova Suite Repository Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Steps use checkbox syntax.

**Goal:** Tách hệ sinh thái thành các repository độc lập, liên kết bằng Git submodule và giữ nguyên hành vi ứng dụng.

**Architecture:** `nova-suite` điều phối bảy submodule. Bốn ứng dụng và `nova-core` được tách từ workspace hiện tại với lịch sử mới; Quiz và Karaoke giữ lịch sử riêng. Các app cần contract tham chiếu mã nguồn `../nova-core` bằng Gradle.

**Tech Stack:** Git submodule, Gradle 8 Android plugin 8.5.2, Java, Android SDK.

**Spec:** `docs/superpowers/specs/2026-11-13-nova-suite-design.md`

## Global Constraints

- Bảo toàn mọi thay đổi chưa commit, file chưa track, commit chưa push và repository gốc; không reset hoặc xóa chúng.
- Giữ application ID, signing, IPC, dữ liệu và hành vi; không deploy lên TV.
- Không thay đổi các ràng buộc Kid Mode, phím remote được bảo vệ, hoặc giới hạn của từng dự án.
- Không commit các thay đổi chưa commit của người dùng vào repo mới khi chưa xác định nội dung được phép công bố.
- Remote submodule phải dùng URL có thể clone, không trỏ vào đường dẫn cục bộ của máy này.

## Review Focus

- Workspace bẩn: xác minh snapshot bao gồm cả file untracked và trạng thái xóa.
- Quiz có commit chưa push: bảo toàn toàn bộ refs và 30 commit.
- Gradle và contract: Home và Family build và chạy unit test từ workspace submodule.
- Công cụ deploy và tài liệu: kiểm tra đường dẫn mới và không thực hiện hành động trên TV.
- Fresh clone: `git submodule update --init --recursive` lấy đúng commit và build được.

## Task 1: Kiểm kê và chuẩn bị có thể khôi phục

**Files:** ba repo hiện tại, bản snapshot ngoài workspace, tài liệu thiết kế này.

- [ ] Ghi `git status`, danh sách refs, remote, diff tracked và untracked của từng repo; tạo snapshot đầy đủ không thay đổi nguồn.
- [ ] Xác minh snapshot bao gồm các file bị xóa hoặc chưa track và refs của Quiz; giữ nguyên các repo gốc.
- [ ] Ghi lại cấu hình Gradle, signing, manifest, contract và các đường dẫn được dùng trong `tools/`.

## Task 2: Tạo các repo ứng dụng và core trong workspace staging

**Files:** các repo `nova-home`, `nova-family`, `nova-ss-clock`, `nova-ss-landscapes`, `nova-core` trong staging.

- [ ] Sao chép các module, cấu hình Gradle, wrapper, tài nguyên và test cần thiết; không di chuyển hoặc ghi đè nguồn gốc.
- [ ] Thiết lập `nova-core` là Android library; chuyển `core/contract` và test của nó mà không đổi API hay IPC.
- [ ] Cấu hình Home và Family tham chiếu `../nova-core`; bảo đảm các module còn lại build độc lập.
- [ ] Giữ nguyên application ID và signing; so sánh manifest và cấu hình với nguồn.
- [ ] Chạy unit test và release build cho từng repo, ghi lại kết quả và APK size; xử lý mọi lỗi trước khi chuyển bước.

## Task 3: Chuẩn bị Quiz và Karaoke

**Files:** bản sao repo `quiznova`, `vocatv` trong staging.

- [ ] Giữ nguyên refs, commit, thay đổi tracked và untracked khi chuẩn bị bản sao thành `nova-quiz` và `nova-kara`.
- [ ] Chạy test và build thích hợp, ghi lại lỗi có sẵn; xác minh không thay đổi package ID, signing hoặc YouTube origin của Karaoke.

## Task 4: Repository điều phối và submodule

**Files:** `nova-suite/.gitmodules`, `nova-suite/README.md`, tài liệu và script hệ sinh thái.

- [ ] Chuẩn bị URL remote hợp lệ và commit các snapshot đã được phép đưa lên; không xuất bản thay đổi chưa commit của người dùng mặc định.
- [ ] Tạo repo điều phối chứa tài liệu, công cụ phù hợp và bảy submodule; cập nhật đường dẫn công cụ deploy và backup mà không tương tác với TV.
- [ ] Clone mới với `--recurse-submodules`; xác nhận cả bảy submodule đúng commit, chạy build/test phù hợp và kiểm tra hướng dẫn setup.
- [ ] Chỉ sau khi xác minh và thống nhất chuyển đổi workspace, bảo toàn bản gốc và đưa cấu trúc mới vào vị trí sử dụng.

## Task 5: Nhận diện

- [ ] Lập kế hoạch riêng cho thiết kế biểu tượng, màu sắc và thay tên hiển thị; các chi tiết thị giác chưa được chốt trong spec.
- [ ] Kiểm tra từng ứng dụng trên TV hoặc emulator và giữ nguyên các giới hạn kỹ thuật của từng repo.
