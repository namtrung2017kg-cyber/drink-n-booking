Drink N Booking
Hệ thống web quản lý đặt lịch và cung cấp dịch vụ đồ uống trực tuyến.
> **Demo trực tuyến:** https://drink-n-booking.onrender.com
> (Web chạy trên gói miễn phí của Render. Nếu lâu không có người truy cập, web sẽ tạm ngủ, lần mở đầu tiên mất khoảng 1 phút để khởi động lại.)
---
1. Giới thiệu
Drink N Booking là đồ án thực tập 8 tuần. Hệ thống cho phép khách hàng xem thực đơn, chọn nhiều loại đồ uống vào giỏ hàng và đặt đơn trực tuyến. Cửa hàng nhận đơn, phân công nhân viên thực hiện và theo dõi trạng thái đơn đến khi hoàn thành.
Hệ thống chia đơn thành 2 loại:
Đơn thường (1 đến 30 ly): khách nhập địa chỉ giao và số điện thoại, cửa hàng báo giờ giao sau.
Đơn lớn (từ 31 ly, tối đa 200 ly): khách chọn ngày và giờ giao trước. Hệ thống kiểm tra điều kiện hợp lệ và trùng lịch. Mỗi đơn lớn có một nhân viên làm trưởng nhóm phụ trách.
Hệ thống có 3 vai trò: Khách hàng, Nhân viên và Quản trị viên (Admin).
---
2. Chức năng chính
Vai trò	Chức năng
Khách hàng	Đăng ký, đăng nhập, đăng xuất, cập nhật hồ sơ cá nhân
	Xem thực đơn: tìm kiếm, lọc theo loại, phân trang, xem chi tiết đồ uống
	Thêm nhiều loại đồ uống vào giỏ hàng, sửa số lượng, xóa món
	Đặt đơn thường hoặc đơn lớn (chọn ngày giờ giao)
	Xem "Đơn của tôi", hủy đơn khi còn đủ điều kiện
Nhân viên	Xác nhận đơn mới để nhận phụ trách đơn
	Xem danh sách "Đơn được giao"
	Cập nhật tiến độ: Bắt đầu, rồi Hoàn thành
Quản trị viên	Quản lý loại đồ uống (thêm, sửa, xóa)
	Quản lý đồ uống: thêm, sửa, xóa, upload ảnh (ảnh tự thu nhỏ còn 800px, lưu trong database)
	Quản lý tài khoản và nhân viên: tìm kiếm, lọc theo vai trò, phân trang, khóa/mở khóa, xóa
	Quản lý đơn: lọc theo trạng thái, loại đơn, ngày; xem chi tiết; đổi nhân viên phụ trách hoặc trưởng nhóm; đổi trạng thái
	Dashboard thống kê: thẻ số liệu, biểu đồ theo ngày/tháng, biểu đồ trạng thái đơn, top 5 đồ uống bán chạy
---
3. Công nghệ sử dụng
Thành phần	Công nghệ
Ngôn ngữ	Java (JDK 21)
Framework	Spring Boot 4
Giao diện	Thymeleaf, Bootstrap 5 (responsive), Chart.js
Bảo mật	Spring Security (đăng nhập form, phân quyền theo vai trò, mã hóa mật khẩu BCrypt)
Truy cập dữ liệu	Spring Data JPA (Hibernate)
Cơ sở dữ liệu	MySQL (máy cá nhân: XAMPP; trên Internet: TiDB Cloud, tương thích MySQL)
Build	Maven
Triển khai	Docker, Render
Quản lý mã nguồn	Git, GitHub
---
4. Yêu cầu trước khi cài đặt
JDK 21
XAMPP (dùng MySQL)
IntelliJ IDEA (bản Community là đủ)
Git hoặc GitHub Desktop
---
5. Tải mã nguồn
Cách 1, dùng GitHub Desktop: File > Clone repository > chọn repo `drink-n-booking` > chọn thư mục lưu > Clone.
Cách 2, dùng dòng lệnh:
```bash
git clone https://github.com/namtrung2017kg-cyber/drink-n-booking.git
```
Sau khi tải xong, mở IntelliJ > File > Open > chọn thư mục `drink-n-booking` (thư mục có file `pom.xml`). Chờ IntelliJ tải thư viện Maven xong.
---
6. Cấu hình cơ sở dữ liệu
Bước 1: Bật MySQL
Mở XAMPP Control Panel > bấm Start ở dòng MySQL.
Bước 2: Cấu hình kết nối
Ứng dụng đọc thông tin kết nối từ biến môi trường. Nếu không đặt biến nào, ứng dụng dùng giá trị mặc định cho XAMPP:
Biến môi trường	Giá trị mặc định
`DB_URL`	`jdbc:mysql://localhost:3306/drink_booking?...`
`DB_USERNAME`	`root`
`DB_PASSWORD`	(để trống)
Chạy trên máy với XAMPP mặc định thì không cần đặt biến nào. Chỉ cần tạo database `drink_booking`: vào http://localhost/phpmyadmin > New > gõ tên `drink_booking` > Create.
Bước 3: Tạo bảng và dữ liệu mẫu
Không cần chạy file SQL. Lần chạy đầu tiên, Hibernate tự tạo các bảng (`spring.jpa.hibernate.ddl-auto=update`). Lớp `DataInitializer` tự thêm:
Tài khoản quản trị và tài khoản nhân viên mẫu
3 loại đồ uống và 8 món mẫu (chỉ thêm khi bảng loại đồ uống đang trống)
> Tài khoản mẫu chỉ dùng khi chạy trên máy cá nhân. Trên bản demo trực tuyến, mật khẩu mặc định đã được đổi. Tài khoản kiểm thử sẽ được gửi riêng cho giảng viên.
---
7. Chạy hệ thống
Cách 1: Chạy bằng IntelliJ
Bật MySQL trong XAMPP.
Trong IntelliJ, mở file `...Application.java` trong package `com.drinknbooking.drinkbooking`.
Bấm nút Run (tam giác xanh).
Khi cửa sổ Run hiện dòng `Started ...Application`, mở trình duyệt vào http://localhost:8080
Cách 2: Chạy bằng dòng lệnh
Mở terminal tại thư mục gốc của project:
```bash
mvnw.cmd spring-boot:run
```
---
8. Triển khai lên Internet
Thành phần	Dịch vụ
Web	Render (Web Service, Docker, gói Free)
Database	TiDB Cloud Starter (tương thích MySQL, gói miễn phí)
Các bước chính:
File `Dockerfile` ở thư mục gốc build project bằng Maven JDK 21 và chạy bằng JRE 21, múi giờ `Asia/Ho_Chi_Minh`.
Trên TiDB Cloud: tạo instance Starter (region Singapore), chạy `CREATE DATABASE drink_booking;`.
Trên Render: tạo Web Service từ repo GitHub, chọn Language Docker, Instance Free, rồi đặt các biến môi trường:
Biến	Ví dụ
`DB_URL`	`jdbc:mysql://HOST:4000/drink_booking?sslMode=REQUIRED&useUnicode=true&characterEncoding=UTF-8`
`DB_USERNAME`	`xxxxxxxx.root`
`DB_PASSWORD`	(mật khẩu TiDB)
Mỗi lần push code lên nhánh `main`, Render tự build và deploy lại.
> Lớp `TiDBFriendlyDialect` thêm tùy chọn `AUTO_ID_CACHE=1` khi tạo bảng, để TiDB cấp ID liên tiếp (1, 2, 3...) giống MySQL. Khi chạy với MySQL XAMPP, tùy chọn này được bỏ qua.
---
9. Hướng dẫn sử dụng
Khách hàng
Bấm Đăng ký để tạo tài khoản, rồi Đăng nhập.
Vào Thực đơn: tìm theo tên, lọc theo loại, bấm vào món để xem chi tiết.
Bấm Thêm vào giỏ, có thể chọn nhiều món khác nhau.
Vào Giỏ hàng để chỉnh số lượng, rồi bấm Đặt hàng.
Tổng 1 đến 30 ly: nhập địa chỉ giao và số điện thoại 10 số.
Tổng từ 31 ly: chọn thêm ngày và giờ giao.
Vào Đơn của tôi để theo dõi trạng thái hoặc hủy đơn.
Nhân viên
Đăng nhập bằng tài khoản nhân viên.
Bấm Xác nhận ở đơn mới để nhận phụ trách đơn đó.
Vào Đơn được giao: bấm Bắt đầu khi bắt đầu làm, bấm Hoàn thành khi giao xong.
Quản trị viên
Đăng nhập bằng tài khoản quản trị.
Dashboard: xem doanh thu, số đơn, biểu đồ và top đồ uống.
Loại đồ uống / Đồ uống: thêm, sửa, xóa, upload ảnh.
Tài khoản: tạo nhân viên, khóa/mở khóa, xóa tài khoản.
Quản lý đơn: lọc đơn, xem chi tiết, đổi nhân viên phụ trách hoặc trưởng nhóm, đổi trạng thái đơn.
---
10. Quy tắc nghiệp vụ chính
Trạng thái đơn:
```
Mới tạo → Đã xác nhận → Đang thực hiện → Hoàn thành
   │            │
   └────────────┴──→ Đã hủy
```
Nhân viên xác nhận đơn mới thì trở thành người phụ trách đơn đó (đơn lớn: trưởng nhóm). Admin có thể đổi người phụ trách.
Chỉ nhân viên phụ trách hoặc admin được chuyển đơn sang Đang thực hiện và Hoàn thành.
Mỗi đơn tối đa 200 ly.
Đơn lớn (từ 31 ly):
Giờ giao theo khung 30 phút, từ 07:00 đến 20:30.
Phải đặt trước ít nhất 2 giờ và không quá 30 ngày.
Số đơn lớn cùng một khung giờ không được vượt số nhân viên đang hoạt động.
Một trưởng nhóm không được phụ trách 2 đơn lớn cùng khung giờ. Hệ thống kiểm tra cả lúc khách đặt và lúc admin phân công.
Hủy đơn: khách hủy được đơn đang ở trạng thái Mới tạo hoặc Đã xác nhận. Riêng đơn lớn phải còn ít nhất 60 phút trước giờ giao.
Thống kê: tính theo ngày đặt. Doanh thu chỉ tính đơn Hoàn thành. Số đơn không tính đơn đã hủy.
---
11. Cấu trúc repository
```
drink-n-booking/
├── Dockerfile
├── .dockerignore
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/
└── src/main/
    ├── java/com/drinknbooking/drinkbooking/
    │   ├── config/        SecurityConfig, DataInitializer, GlobalModelAttributes, TiDBFriendlyDialect
    │   ├── controller/    Auth, Page, Menu, Image, Cart, Booking, Staff, Admin*...
    │   ├── dto/           RegisterForm, UserForm, CheckoutForm
    │   ├── entity/        User, Role, Category, Drink, DrinkImage, Booking, BookingItem, BookingStatus
    │   ├── repository/    các interface Spring Data JPA
    │   └── service/       CustomUserDetailsService, BookingService, Cart
    └── resources/
        ├── application.properties
        └── templates/
            ├── fragments, home, login, register, profile, menu, menu-detail,
            │   cart, booking-form, my-booking
            ├── admin/     dashboard, categories, drinks, users, bookings, booking-detail...
            └── staff/     schedule
```
> Trong code, class `Booking` (bảng `orders`) tương ứng với "đơn hàng" trên giao diện.
---
12. Một số lỗi thường gặp
Lỗi	Nguyên nhân và cách xử lý
`Communications link failure` khi chạy trên máy	Chưa bật MySQL trong XAMPP. Bật MySQL rồi Run lại.
`Unknown database 'drink_booking'`	Chưa tạo database. Tạo `drink_booking` trong phpMyAdmin hoặc trên TiDB.
`Port 8080 was already in use`	Ứng dụng đang chạy ở cửa sổ khác. Bấm Stop (ô vuông đỏ) rồi Run lại.
Đăng nhập báo "Tài khoản của bạn đã bị khóa"	Tài khoản đã bị admin khóa. Admin vào Quản lý tài khoản để mở khóa.
Lần đầu mở web demo rất chậm	Render đang khởi động lại sau thời gian ngủ. Chờ khoảng 1 phút.
`Access denied for user` trên Render	Sai `DB_USERNAME` (phải có tiền tố `xxxx.root`) hoặc sai `DB_PASSWORD`.
---
13. Lưu ý bảo mật
Không ghi mật khẩu database vào code hay đẩy lên GitHub. Mật khẩu chỉ đặt bằng biến môi trường trên Render.
Đổi mật khẩu tài khoản mẫu ngay sau khi triển khai lên Internet.
Không commit các thư mục `target/`, `.idea/` và file `*.iml`.
Mật khẩu người dùng được mã hóa bằng BCrypt, không lưu dạng chữ thường.
---
14. Mục đích sử dụng
Dự án được thực hiện cho mục đích học tập, trong khuôn khổ đồ án thực tập 8 tuần.
Sinh viên thực hiện: HỌ TÊN – MSSV
Giảng viên hướng dẫn: TÊN GIẢNG VIÊN
