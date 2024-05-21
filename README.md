# medical-booking-be-spring
# Hướng dẫn
- Clone project từ git:
  - Chạy lệnh "git clone https://github.com/professional-practice-haui/medical-booking-be-spring.git"
- Cài đặt Dependencies:
  - Mở terminal hoặc command prompt và vào thư mục dự án.
  - Chạy lệnh "mvn clean install" để tải và cài đặt các dependencies cần thiết.    
- Cấu hình Database (nếu cần):
  - Nếu dự án sử dụng database, bạn cần cấu hình kết nối đến database.
  - Thay đổi thông tin kết nối trong file application.properties hoặc application.yml (tùy theo cấu trúc dự án).
  - Ví dụ:
  spring.datasource.url=jdbc:mysql://localhost:3306/your_database_name
  spring.datasource.username=your_username
  spring.datasource.password=your_password
- Khởi động ứng dụng:
  - Chạy lệnh "mvn spring-boot:run" để khởi động ứng dụng.
  - Ứng dụng sẽ chạy trên port mặc định là 8080.
  - Nếu muốn thay đổi port, bạn có thể chỉnh sửa thuộc tính server.port trong file cấu hình.
- Truy cập ứng dụng:
  - Mở trình duyệt web và truy cập vào địa chỉ http://localhost:8080.
  - Ứng dụng sẽ được hiển thị trên trình duyệt.
- Lưu ý:
  - Bạn có thể cần cài đặt JDK và Maven trước khi chạy dự án.
  - File application.properties hoặc application.yml có thể chứa các cấu hình khác ngoài kết nối database, tùy thuộc vào dự án.
  - Bạn có thể tham khảo thêm tài liệu của dự án hoặc code để hiểu rõ hơn về cách hoạt động của nó.