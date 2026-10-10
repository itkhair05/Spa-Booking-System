# Danh mục API RESTful TIKEY SPA (REST API Reference)

Tài liệu này cung cấp đặc tả chi tiết toàn bộ các điểm cuối API (Endpoints), phương thức HTTP, yêu cầu phân quyền, định dạng dữ liệu truyền tải (Request / Response Payload) và mã lỗi chuẩn mực của nền tảng **TIKEY SPA**.

---

## 1. Tiêu chuẩn và Quy ước Chung

- **Base URL API Direct**: `http://localhost:8080/api/v1` (Local) hoặc `https://api.example.com/api/v1` (Production).
- **Định dạng dữ liệu**: `Content-Type: application/json; charset=UTF-8` (ngoại trừ các endpoint tải file / xuất Excel).
- **Xác thực yêu cầu**: Đính kèm JWT token vào HTTP Header:
  ```http
  Authorization: Bearer <access_token>
  ```
- **Mã phản hồi HTTP chuẩn mực**:
  - `200 OK`: Xử lý thành công, trả về dữ liệu.
  - `201 CREATED`: Tạo mới tài nguyên thành công.
  - `400 BAD REQUEST`: Dữ liệu đầu vào sai định dạng hoặc vi phạm quy tắc Bean Validation.
  - `401 UNAUTHORIZED`: Thiếu JWT token, token không hợp lệ hoặc đã hết hạn.
  - `403 FORBIDDEN`: Không đủ quyền hạn truy cập tài nguyên (ví dụ STAFF cố gắng truy cập dữ liệu của OWNER hoặc nhân viên khác).
  - `404 NOT FOUND`: Không tìm thấy tài nguyên theo mã ID yêu cầu.
  - `409 CONFLICT`: Xung đột dữ liệu nghiệp vụ (ví dụ trùng giờ hẹn nhân viên, tài khoản đã tồn tại).
  - `429 TOO MANY REQUESTS`: Vượt quá giới hạn tần suất yêu cầu (Rate Limiting).
  - `500 INTERNAL SERVER ERROR`: Lỗi nội bộ phía máy chủ.

---

## 2. Xác thực và Phân quyền (Authentication API)

### 2.1. Đăng nhập hệ thống (Login)
- **Phương thức & Đường dẫn**: `POST /api/v1/auth/login`
- **Quyền hạn**: Công khai (`permitAll()`). Áp dụng Rate Limiting bảo vệ chống dò mật khẩu brute-force.
- **Request Body**:
  ```json
  {
    "username": "owner@demo.local",
    "password": "Password123!"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsIn...",
    "type": "Bearer",
    "username": "owner@demo.local",
    "role": "ROLE_OWNER",
    "staffId": null,
    "tenantId": 1
  }
  ```
- **Lỗi thường gặp**:
  - `400 Bad Request`: `username` hoặc `password` để trống.
  - `401 Unauthorized`: Sai tên đăng nhập hoặc mật khẩu.

### 2.2. Đổi mật khẩu cá nhân (Change Password)
- **Phương thức & Đường dẫn**: `POST /api/v1/auth/change-password`
- **Quyền hạn**: Yêu cầu xác thực (`hasAnyRole('OWNER', 'STAFF')`).
- **Request Body**:
  ```json
  {
    "oldPassword": "Password123!",
    "newPassword": "NewStrongPassword456!"
  }
  ```
- **Response (200 OK)**: Trả về thông báo đổi mật khẩu thành công.

---

## 3. Khách hàng Công khai (Public SPA Endpoints)

Các endpoint công khai phục vụ giao diện trang chủ đặt lịch của khách hàng vãng lai. Tham số `{slug}` đại diện cho mã định danh thương hiệu (mặc định: `tikey-spa`).

### 3.1. Thông tin chung của Spa
- `GET /api/v1/public/spas/{slug}`: Lấy thông tin giới thiệu, địa chỉ, hotline, giờ mở cửa.
- `GET /api/v1/public/spas/{slug}/services`: Danh sách dịch vụ đang hoạt động.
- `GET /api/v1/public/spas/{slug}/services/featured`: Danh sách dịch vụ nổi bật hiển thị ở trang chủ.
- `GET /api/v1/public/spas/{slug}/service-categories`: Danh sách các nhóm danh mục dịch vụ.
- `GET /api/v1/public/spas/{slug}/staff`: Danh sách chuyên viên trị liệu cho phép hiển thị công khai.
- `GET /api/v1/public/spas/{slug}/reviews`: Danh sách đánh giá của khách hàng đã được duyệt xuất bản.
- `GET /api/v1/public/spas/{slug}/articles`: Danh sách bài viết cẩm nang làm đẹp đã xuất bản.
- `GET /api/v1/public/spas/{slug}/articles/{articleSlug}`: Chi tiết bài viết theo đường dẫn thân thiện.

### 3.2. Tra cứu khung giờ trống (Availability Lookup)
- **Phương thức & Đường dẫn**: `GET /api/v1/public/spas/{slug}/availability`
- **Quyền hạn**: Công khai.
- **Query Parameters**:
  - `serviceId` (bắt buộc, Long): ID của dịch vụ cần đặt.
  - `date` (bắt buộc, ISO Date `YYYY-MM-DD`): Ngày muốn kiểm tra lịch (không được chọn ngày trong quá khứ).
  - `staffId` (tùy chọn, Long): ID của nhân viên mong muốn (nếu không truyền, hệ thống tìm khung giờ có ít nhất 1 nhân viên phù hợp).
- **Response (200 OK)**:
  ```json
  [
    "2026-10-15T09:00:00",
    "2026-10-15T09:30:00",
    "2026-10-15T11:00:00",
    "2026-10-15T14:30:00"
  ]
  ```

### 3.3. Đặt lịch hẹn mới (Create Public Booking)
- **Phương thức & Đường dẫn**: `POST /api/v1/public/spas/{slug}/bookings`
- **Quyền hạn**: Công khai (Khách vãng lai không cần tạo tài khoản).
- **Request Body**:
  ```json
  {
    "serviceId": 1,
    "staffId": 2,
    "startTime": "2026-10-15T09:00:00",
    "customerName": "Nguyễn Thị Mai",
    "customerPhone": "0987654321",
    "customerEmail": "mai.nguyen@example.com",
    "paymentMethod": "PAY_AT_SPA"
  }
  ```
  *(Giá trị `paymentMethod` có thể là `PAY_AT_SPA` hoặc `VNPAY`).*
- **Response (201 Created)**:
  ```json
  {
    "id": 105,
    "bookingCode": "BK-261015-8AB9",
    "serviceName": "Massage Cổ Vai Gáy Chuyên Sâu",
    "staffName": "Trần Thị Lan",
    "startTime": "2026-10-15T09:00:00",
    "endTime": "2026-10-15T10:00:00",
    "price": 350000.00,
    "status": "PENDING",
    "paymentMethod": "PAY_AT_SPA",
    "paymentStatus": "UNPAID",
    "paymentUrl": null
  }
  ```
  *(Nếu chọn `paymentMethod = "VNPAY"`, trường `paymentUrl` sẽ chứa đường dẫn thanh toán trực tiếp sang cổng VNPay).*

### 3.4. Tra cứu lịch hẹn (Lookup Booking by Code)
- **Phương thức & Đường dẫn**: `GET /api/v1/public/spas/{slug}/bookings/{bookingCode}`
- **Quyền hạn**: Công khai (Bảo vệ thông tin cá nhân: Khách hàng cần cung cấp số điện thoại đối soát nếu yêu cầu).
- **Response (200 OK)**: Trả về chi tiết lịch hẹn, thông tin thanh toán và lịch sử hoàn tiền (nếu có).

### 3.5. Hủy lịch hẹn công khai (Cancel Booking)
- **Phương thức & Đường dẫn**: `POST /api/v1/public/spas/{slug}/bookings/{bookingCode}/cancel`
- **Request Body**:
  ```json
  {
    "phone": "0987654321",
    "reason": "Bận việc gia đình đột xuất"
  }
  ```

### 3.6. Gửi góp ý phản hồi (Submit Feedback)
- **Phương thức & Đường dẫn**: `POST /api/v1/public/spas/{slug}/feedback`
- **Quyền hạn**: Công khai. Áp dụng Rate Limiting tối đa 5 yêu cầu / phút trên mỗi địa chỉ IP máy khách.

---

## 4. Quản lý Lịch hẹn (Booking Management API)

Các endpoint dành riêng cho người dùng nội bộ (`OWNER` và `STAFF`).

### 4.1. Lấy danh sách lịch hẹn
- **Phương thức & Đường dẫn**: `GET /api/v1/bookings`
- **Quyền hạn**: `ROLE_OWNER` hoặc `ROLE_STAFF`.
- **Cơ chế cô lập vai trò**:
  - `OWNER`: Xem toàn bộ lịch hẹn của spa, lọc theo `staffId`, khoảng thời gian `startDate` - `endDate`, và `status`.
  - `STAFF`: Tự động ép buộc chỉ xem các lịch hẹn được chỉ định cho chính nhân viên đó (`linkedStaffId`). Bị chặn truy cập nếu cố tình truyền `staffId` của người khác (`403 Forbidden`).

### 4.2. Chi tiết lịch hẹn
- **Phương thức & Đường dẫn**: `GET /api/v1/bookings/{id}`
- **Quyền hạn**: `ROLE_OWNER` hoặc `ROLE_STAFF` (chỉ xem được lịch của chính mình).
- **Response**: Trả về `BookingDetailResponse` bao gồm thông tin chi tiết dịch vụ, khách hàng, nhân viên, thanh toán và hoàn tiền.

### 4.3. Các thao tác chuyển đổi trạng thái vòng đời lịch hẹn
- `POST /api/v1/bookings/{id}/confirm`: Xác nhận lịch hẹn (`PENDING` $\rightarrow$ `CONFIRMED`).
- `POST /api/v1/bookings/{id}/check-in`: Khách đã đến cơ sở (`CONFIRMED` $\rightarrow$ `CHECKED_IN`).
- `POST /api/v1/bookings/{id}/start`: Bắt đầu thực hiện trị liệu (`CHECKED_IN` $\rightarrow$ `IN_PROGRESS`).
- `POST /api/v1/bookings/{id}/complete`: Hoàn thành buổi hẹn (`IN_PROGRESS` $\rightarrow$ `COMPLETED`).
- `POST /api/v1/bookings/{id}/cancel`: Hủy lịch hẹn (`CONFIRMED`/`PENDING` $\rightarrow$ `CANCELLED`).
- `POST /api/v1/bookings/{id}/no-show`: Đánh dấu khách không đến (`CONFIRMED` $\rightarrow$ `NO_SHOW`).
- `POST /api/v1/bookings/{id}/reschedule`: Đổi giờ hẹn mới (`CONFIRMED` $\rightarrow$ `CONFIRMED` với thời gian mới).
- `PATCH /api/v1/bookings/{id}/assign`: Chỉ định lại nhân viên thực hiện (Chỉ dành riêng cho `ROLE_OWNER`).

---

## 5. Bảng điều khiển Quản trị (Dashboard API)

### 5.1. Thống kê số liệu hoạt động (Metrics)
- **Phương thức & Đường dẫn**: `GET /api/v1/dashboard/metrics`
- **Quyền hạn**: `ROLE_OWNER` hoặc `ROLE_STAFF`.
- **Dữ liệu phân quyền**:
  - `OWNER`: Nhận đầy đủ chỉ số toàn spa bao gồm số lịch hẹn hôm nay, lịch sắp tới, lịch chờ duyệt, doanh thu thực tế hoàn thành (`todayCompletedRevenue`, `totalCompletedRevenue`), doanh thu dự kiến (`todayExpectedRevenue`), xu hướng 7 ngày và top 5 dịch vụ được đặt nhiều nhất.
  - `STAFF`: Chỉ nhận số lượng lịch hẹn cá nhân hôm nay và lịch hẹn sắp tới của mình; doanh thu bị ẩn hoàn toàn (trả về 0.00).

---

## 6. Quản lý Nhân viên & Lịch làm việc (Staff & Scheduling API)

### 6.1. Hồ sơ nhân viên
- `GET /api/v1/staff`: Danh sách toàn bộ nhân viên kèm trạng thái tài khoản (Chỉ `OWNER`).
- `POST /api/v1/staff`: Tạo mới hồ sơ nhân viên (Chỉ `OWNER`).
- `PUT /api/v1/staff/{id}`: Cập nhật thông tin nhân viên (Chỉ `OWNER`).
- `DELETE /api/v1/staff/{id}`: Xóa mềm nhân viên (`is_deleted = true`) (Chỉ `OWNER`).
- `GET /api/v1/staff/me`: Lấy thông tin hồ sơ của chính nhân viên đang đăng nhập (`STAFF`).
- `PUT /api/v1/staff/me`: Nhân viên tự cập nhật thông tin cá nhân (số điện thoại, email) (`STAFF`).

### 6.2. Tài khoản nhân viên
- `POST /api/v1/staff/{id}/account`: Khởi tạo tài khoản đăng nhập cho nhân viên (Chỉ `OWNER`).
- `PATCH /api/v1/staff/{id}/account/status`: Khóa hoặc kích hoạt tài khoản nhân viên (Chỉ `OWNER`).
- `POST /api/v1/staff/{id}/account/reset-password`: Đặt lại mật khẩu tài khoản nhân viên (Chỉ `OWNER`).

### 6.3. Giờ làm việc & Ngày nghỉ
- `GET /api/v1/staff/{staffId}/working-hours`: Xem giờ làm việc 7 ngày trong tuần của nhân viên.
- `PUT /api/v1/staff/{staffId}/working-hours`: Cập nhật cấu hình ca làm việc trong tuần (Chỉ `OWNER`).
- `GET /api/v1/staff/{staffId}/days-off`: Xem danh sách ngày nghỉ đăng ký.
- `POST /api/v1/staff/{staffId}/days-off`: Thêm ngày nghỉ cho nhân viên (Chỉ `OWNER`).
- `DELETE /api/v1/staff/{staffId}/days-off/{dayOffId}`: Xóa ngày nghỉ (Chỉ `OWNER`).
- `GET /api/v1/staff/daily-schedule`: Xem chi tiết lịch trình làm việc và các ca hẹn trong ngày cụ thể (`OWNER` hoặc `STAFF`).

---

## 7. Dịch vụ, Danh mục & Khách hàng (Catalog & Customer API)

- `GET /api/v1/services`: Danh sách dịch vụ (Kèm `category`).
- `POST /api/v1/services`: Tạo mới dịch vụ (`OWNER`).
- `PUT /api/v1/services/{id}`: Sửa dịch vụ (`OWNER`).
- `DELETE /api/v1/services/{id}`: Xóa dịch vụ (`OWNER`).
- `GET /api/v1/service-categories`: Danh sách danh mục dịch vụ.
- `POST /api/v1/service-categories`: Tạo danh mục mới (`OWNER`).
- `GET /api/v1/customers`: Danh sách khách hàng và lịch sử số lần ghé spa (`OWNER`).
- `GET /api/v1/customers/{id}`: Chi tiết hồ sơ khách hàng (`OWNER`).

---

## 8. Thanh toán, Webhook & Hoàn tiền (Payment & Refund API)

### 8.1. Cổng thanh toán VNPay IPN (Webhook)
- **Phương thức & Đường dẫn**: `GET` / `POST` `/api/v1/payments/vnpay-ipn`
- **Quyền hạn**: Công khai (`permitAll()`). Được bảo vệ bằng kiểm tra chữ ký số bảo mật HMAC-SHA512 kháng tấn công Timing Attack.
- **Query Parameters từ VNPay**: `vnp_TxnRef`, `vnp_Amount`, `vnp_ResponseCode`, `vnp_TransactionNo`, `vnp_SecureHash`, ...
- **Response trả về cho VNPay Server**:
  - Hợp lệ: `{"RspCode":"00","Message":"Confirm Success"}`
  - Giao dịch đã xử lý trước đó: `{"RspCode":"02","Message":"Order already confirmed"}`
  - Sai số tiền thanh toán: `{"RspCode":"04","Message":"Invalid Amount"}`
  - Sai chữ ký số: `{"RspCode":"97","Message":"Invalid Checksum"}`

### 8.2. Đối soát giao dịch QueryDR
- **Phương thức & Đường dẫn**: `POST /api/v1/payments/{paymentId}/reconcile`
- **Quyền hạn**: `ROLE_OWNER` duy nhất.

### 8.3. Quản lý Hoàn tiền (Refunds)
- `POST /api/v1/refunds`: Khởi tạo yêu cầu hoàn tiền cho lịch hẹn bị hủy (Chỉ `OWNER`). Tự động tính toán tỷ lệ hoàn tiền theo chính sách thời gian báo trước.
- `GET /api/v1/refunds`: Lấy danh sách lịch sử hoàn tiền (Chỉ `OWNER`).

---

## 9. Xuất dữ liệu & Tiện ích hệ thống

- `GET /api/v1/export/bookings`: Xuất danh sách lịch hẹn ra file Excel (`.xlsx`) theo bộ lọc ngày (`OWNER`).
- `GET /api/v1/export/revenue`: Xuất báo cáo doanh thu chi tiết ra file Excel (`OWNER`).
- `GET /api/v1/export/customers`: Xuất danh bạ khách hàng ra file Excel (`OWNER`).
- `POST /api/v1/uploads`: Upload hình ảnh đại diện nhân viên hoặc dịch vụ (Hỗ trợ định dạng PNG, JPG, WEBP dung lượng tối đa 5MB).
- `GET /actuator/health`: Kiểm tra trạng thái sống của dịch vụ và kết nối cơ sở dữ liệu (Healthcheck probe).

---

Tài liệu liên quan:
- [Kiến trúc hệ thống (docs/ARCHITECTURE.md)](ARCHITECTURE.md)
- [Bảo mật & Phân quyền (docs/SECURITY.md)](SECURITY.md)
- [Thiết kế Cơ sở Dữ liệu (docs/DATABASE.md)](DATABASE.md)
