# Thiết kế hệ sinh thái Nova Suite

## Mục tiêu

Xây dựng nhận diện thống nhất, tối giản và gần gũi với gia đình cho các ứng dụng Android TV, đồng thời giữ từng ứng dụng trong repository độc lập. Mỗi ứng dụng có thể có màu và đặc điểm riêng trong một ngôn ngữ thiết kế chung.

## Cấu trúc

`nova-suite` là repository điều phối: chứa tài liệu hệ sinh thái và tham chiếu Git submodule tới bảy repository:

| Submodule | Vai trò | Tên hiển thị dự kiến |
| --- | --- | --- |
| `nova-home` | Launcher | Nova Home |
| `nova-family` | Kiểm soát gia đình | Nova Family |
| `nova-ss-clock` | Screensaver đồng hồ | Nova Clock |
| `nova-ss-landscapes` | Screensaver phong cảnh | Nova Landscapes |
| `nova-quiz` | Học và trắc nghiệm | Nova Quiz |
| `nova-kara` | Karaoke | Nova Karaoke |
| `nova-core` | Contract và thư viện dùng chung | Không áp dụng |

Submodule ghi nhận commit cụ thể của từng repo; mỗi repo có thể phát triển và phát hành độc lập. Không tạo `nova-platform` riêng.

## Phương án và quyết định

Đã cân nhắc giữ repository hiện tại, gộp thành monorepo và giữ repo độc lập dưới một repo điều phối. Chọn phương án cuối để vừa quản lý hệ sinh thái tại một nơi vừa giữ vòng đời độc lập cho từng ứng dụng.

Các repo được tách từ repository hiện tại bắt đầu lịch sử Git mới theo lựa chọn của người dùng. VocaTV và QuizNova tiếp tục là repo riêng; việc chuyển chỗ và đổi tên không tự nó đòi hỏi xóa lịch sử hiện có.

Các ứng dụng cần thư viện chung tham chiếu mã nguồn `../nova-core` bằng cấu hình Gradle phù hợp. Phương án phát hành thư viện theo phiên bản được để lại cho tương lai. Bước đầu chỉ chuyển `core/contract` hiện có; chỉ tách thêm thành phần khi xác nhận có nhu cầu chung.

## Triển khai theo giai đoạn

1. Khảo sát trạng thái Git, cấu hình build, công cụ và các phụ thuộc giữa module; xác định phương án chuyển an toàn trước khi thay đổi cấu trúc.
2. Tách Home, Family, hai screensaver và core thành các repo độc lập; chuẩn bị Quiz và Karaoke, rồi cấu hình submodule và build từ cấu trúc mới.
3. Thống nhất tài liệu thương hiệu, tên hiển thị, biểu tượng, màu sắc và trải nghiệm điều khiển TV; triển khai lần lượt trên từng ứng dụng.
4. Đánh giá các thành phần giao diện và kiến trúc thực sự trùng nhau trước khi đưa vào `nova-core`.

## Ràng buộc

- Giữ ứng dụng nhẹ, dễ đọc trên TV, dễ điều khiển bằng remote và phù hợp gia đình. Tôn trọng các giới hạn kỹ thuật riêng của từng repo, đặc biệt giới hạn Java và dung lượng của VocaTV.
- Giữ định danh Android, khả năng cập nhật, dữ liệu người dùng và các hợp đồng giao tiếp hiện có trừ khi một thay đổi cụ thể được thiết kế và chấp thuận riêng.
- Bảo toàn các cơ chế an toàn hiện tại của launcher và FamilyTime, bao gồm ranh giới Kid Mode và phím remote được bảo vệ.
- Không triển khai lên TV hoặc thay đổi trạng thái thiết bị trong phạm vi thiết kế này.
- Màu sắc và thiết kế icon nêu trong thảo luận là ý tưởng ban đầu, chưa phải thông số đã chốt.

## Kiểm chứng

Mỗi repo phải có hướng dẫn clone/build và kiểm thử phù hợp. Kiểm tra clone kèm submodule, build độc lập trong workspace đầy đủ, các bài test liên quan, và tương tác Home–Family sau khi tách contract. Việc chuyển cần giữ bản gốc hoặc phương án khôi phục cho đến khi xác minh hoàn tất.
