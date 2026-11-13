# Nova Suite

Nova Suite là hệ sinh thái ứng dụng Android TV dành cho gia đình. Repository này điều phối tài liệu, công cụ và các repository ứng dụng dưới dạng Git submodule.

| Repository | Chức năng |
| --- | --- |
| `nova-home` | Màn hình chính TV |
| `nova-family` | Quản lý thời gian và chế độ trẻ em |
| `nova-kara` | Karaoke |
| `nova-quiz` | Học và trắc nghiệm |
| `nova-ss-clock` | Screensaver đồng hồ |
| `nova-ss-landscapes` | Screensaver phong cảnh |
| `nova-core` | Contract và mã dùng chung |

## Lấy mã nguồn

```bash
git clone --recurse-submodules git@github.com:jnovax/nova-suite.git
```

Với bản clone đã có, chạy `git submodule update --init --recursive`. Mỗi submodule trỏ tới một commit cụ thể; thay đổi mã trong submodule cần được commit và push ở repository của nó trước khi cập nhật commit tương ứng trong Nova Suite.

Home và Family tham chiếu mã nguồn `../nova-core`; cần khởi tạo submodule đó trước khi build. Một số bản release cần khóa ký được cung cấp riêng, không lưu trong Git.

## Công cụ và tài liệu cũ

Tài liệu, `tools/` và `profiles/` từ launcher cũ được giữ lại trong quá trình chuyển đổi. Một số công cụ vẫn tham chiếu cấu trúc `apps/` cũ và chưa dùng được từ bản clone mới; xem [hướng dẫn launcher cũ](docs/legacy-launcher-readme.md) như tài liệu tham khảo, không coi đó là hướng dẫn build mới. Không dùng các công cụ đó để thay đổi TV trước khi xác minh đường dẫn và snapshot.
