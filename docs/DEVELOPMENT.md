# Hướng dẫn Phát triển và Cài đặt Local (Development Guide)

Tài liệu này hướng dẫn chi tiết từng bước cho lập trình viên mới để thiết lập môi trường, cấu hình biến môi trường, khởi chạy và kiểm thử hệ thống **TIKEY SPA** trên máy tính cá nhân (tối ưu hóa cho môi trường Windows, macOS và Linux).

---

## 1. Yêu cầu Môi trường (Prerequisites)

Trước khi bắt đầu, đảm bảo máy tính của bạn đã cài đặt các công cụ sau với phiên bản tương ứng:

| Công cụ | Phiên bản yêu cầu | Mục đích sử dụng | Lệnh kiểm tra |
| :--- | :--- | :--- | :--- |
| **Java JDK** | **17 LTS** (Eclipse Adoptium / Amazon Corretto) | Chạy Backend Spring Boot | `java -version` |
| **Node.js** | **18.x** trở lên (Khuyến nghị **20.x+**) | Môi trường runtime Frontend | `node -v` |
| **npm** | **9.x** trở lên | Trình quản lý gói thư viện Frontend | `npm -v` |
| **MySQL Server** | **8.0+** (hoặc thông qua Docker) | Cơ sở dữ liệu quan hệ chính | `mysql --version` |
| **Git** | **2.30+** | Quản lý mã nguồn | `git --version` |
| **Docker & Compose** *(Tùy chọn)* | Docker Engine 24+ & Compose v2+ | Khởi chạy nhanh toàn bộ hệ thống bằng container | `docker compose version` |

---

## 2. Chuẩn bị Mã nguồn và Biến Môi trường

### Bước 1: Clone kho mã nguồn
Mở terminal (PowerShell trên Windows hoặc Bash trên Linux/macOS):
```bash
git clone https://github.com/itkhair05/Spa-Booking-System.git
cd Spa-Booking-System
```

### Bước 2: Khởi tạo file biến môi trường gốc
Sao chép file mẫu `.env.example` thành `.env` tại thư mục gốc của repository:
```bash
# Trên Windows PowerShell:
Copy-Item .env.example .env

# Trên Linux/macOS:
cp .env.example .env
```

Mở file `.env` và thiết lập các giá trị an toàn cho môi trường phát triển local:
```env
# Mật khẩu tài khoản MySQL root
DB_PASSWORD=LocalDevPassword123!

# Khóa bí mật ký JWT (Bắt buộc tối thiểu 256 bits - tương đương 32 ký tự ngẫu nhiên)
JWT_SECRET=super_secret_jwt_key_must_be_at_least_256_bits_long_for_dev_only

# Mật khẩu tài khoản mẫu được DevDataSeeder tự động tạo khi chạy dev
DEV_OWNER_PASSWORD=Password123!
DEV_STAFF_PASSWORD=Password123!

# Cấu hình cổng thanh toán VNPay Sandbox
VNPAY_TMN_CODE=VMWY8Z1F
VNPAY_HASH_SECRET=test_sandbox_secret_for_local_development_only
```

---

## 3. Lựa chọn Phương án Khởi chạy

Bạn có thể lựa chọn 1 trong 2 phương án bên dưới tùy theo nhu cầu:
- **Phương án A (Khuyến nghị cho kiểm thử nhanh)**: Chạy toàn bộ ứng dụng qua **Docker Compose**.
- **Phương án B (Khuyến nghị cho lập trình & debug)**: Chạy trực tiếp từng service trên máy host (**Native Development**).

---

### Phương án A: Chạy toàn bộ bằng Docker Compose

Phương án này tự động khởi tạo 3 container: MySQL 8, Backend Spring Boot và Frontend Nginx.

1. **Build và khởi động containers**:
   ```bash
   docker compose build
   docker compose up -d
   ```

2. **Kiểm tra trạng thái các container**:
   ```bash
   docker compose ps
   ```
   Cả 3 service `spabooking-mysql`, `spabooking-backend` và `spabooking-frontend` phải hiển thị trạng thái `Up` (hoặc `healthy`).

3. **Xem log hoạt động trực tiếp**:
   ```bash
   docker compose logs -f backend
   ```

4. **Dừng hệ thống**:
   ```bash
   # Dừng nhưng giữ nguyên dữ liệu database và ảnh upload:
   docker compose down

   # Dừng và xóa toàn bộ dữ liệu (Reset sạch sẽ):
   docker compose down -v
   ```

---

### Phương án B: Chạy trực tiếp trên máy host (Native Development)

#### 1. Khởi tạo Cơ sở dữ liệu MySQL Local
Nếu bạn đã có MySQL cài đặt sẵn trên máy:
```sql
CREATE DATABASE IF NOT EXISTS spabooking_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

*(Hoặc bạn có thể chỉ khởi chạy riêng container MySQL bằng lệnh: `docker compose up -d mysql`)*.

#### 2. Khởi chạy Backend (Spring Boot)
Mở cửa sổ Terminal thứ nhất, điều hướng vào thư mục `backend`:
```bash
cd backend
```

Thiết lập các biến môi trường cần thiết trên terminal:

**Trên Windows PowerShell:**
```powershell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/spabooking_dev?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true"
$env:SPRING_DATASOURCE_USERNAME="root"
$env:DB_PASSWORD="LocalDevPassword123!"
$env:JWT_SECRET="super_secret_jwt_key_must_be_at_least_256_bits_long_for_dev_only"
$env:VNPAY_HASH_SECRET="test_sandbox_secret_for_local_development_only"
```

**Khởi động Backend bằng Maven Wrapper:**
```powershell
.\mvnw.cmd spring-boot:run
```
*(Trên Linux/macOS sử dụng: `./mvnw spring-boot:run`)*.

Khi backend khởi động thành công, bạn sẽ thấy thông báo:
```text
Tomcat started on port 8080 (http) with context path '/'
Started SpaBookingApplication in X.XXX seconds
```
Flyway sẽ tự động kích hoạt và thực thi tất cả migration từ `V1` đến `V15`. Đồng thời, `DevDataSeeder` sẽ nạp dữ liệu mẫu ban đầu.

#### 3. Khởi chạy Frontend (React + Vite)
Mở cửa sổ Terminal thứ hai, điều hướng vào thư mục `frontend`:
```bash
cd frontend
```

Cài đặt các gói phụ thuộc (Dependencies):
```bash
npm install
```

Tạo file biến môi trường local `.env.local` trong thư mục `frontend`:
```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

Khởi chạy Vite Development Server:
```bash
npm run dev
```
Trình duyệt sẽ thông báo máy chủ phát triển đang lắng nghe tại: `http://localhost:5173`.

---

## 4. Tài khoản Đăng nhập Mẫu (Seeded Demo Accounts)

Khi chạy ở chế độ **Development (`SPRING_PROFILES_ACTIVE=dev`)**, hệ thống tự động khởi tạo sẵn các tài khoản demo:

| Nhóm tài khoản | Tên đăng nhập (Username) | Mật khẩu mặc định | Quyền hạn và Phạm vi |
| :--- | :--- | :--- | :--- |
| **Chủ Spa (OWNER)** | `owner@demo.local` | Giá trị trong `DEV_OWNER_PASSWORD` (`Password123!`) | Toàn quyền quản trị spa: Dashboard doanh thu, dịch vụ, nhân viên, khách hàng, hoàn tiền, xuất Excel. |
| **Nhân viên (STAFF)** | `staff@demo.local` | Giá trị trong `DEV_STAFF_PASSWORD` (`Password123!`) | Quản lý ca làm việc cá nhân, xem và cập nhật lịch hẹn được phân công. Doanh thu tài chính bị ẩn. |
| **Khách hàng vãng lai** | *Không cần tài khoản* | *Không cần mật khẩu* | Trải nghiệm trực tiếp trang chủ, chọn dịch vụ, kiểm tra giờ trống và đặt lịch tại `http://localhost:5173/`. |

---

## 5. Quy trình Kiểm thử và Đảm bảo Chất lượng (Testing & QA)

Trước khi tạo Pull Request, lập trình viên bắt buộc phải chạy và vượt qua toàn bộ các bài kiểm tra tự động:

### 5.1. Kiểm tra Frontend
Thực hiện tại thư mục `frontend`:
```bash
# 1. Kiểm tra quy chuẩn cú pháp ESLint (Yêu cầu 0 errors, 0 warnings):
npm run lint

# 2. Kiểm tra biên dịch TypeScript và đóng gói bản phát hành Vite:
npm run build
```

### 5.2. Kiểm tra Backend
Thực hiện tại thư mục `backend`:
```bash
# 1. Chạy bài kiểm tra tối ưu hóa hiệu năng và hồi quy:
.\mvnw.cmd test -Dtest=PublicBookingPerformanceOptimizationTest

# 2. Chạy toàn bộ test suite backend (502 tests, kiểm toán bảo mật, logic đặt lịch, thanh toán):
.\mvnw.cmd test
```

### 5.3. Kiểm tra Định dạng Git
Thực hiện tại thư mục gốc repository:
```bash
git diff --check
```
Đảm bảo không phát sinh lỗi ký tự xuống dòng (CRLF/LF) hoặc khoảng trắng thừa ở cuối file.

---

## 6. Quy tắc Đóng góp Mã nguồn (Coding & Git Standards)

1. **Tuyệt đối không commit file chứa secret**: File `.env`, file cấu hình chứa mật khẩu thật, khóa ký VNPay live hoặc JWT secret thật không bao giờ được đưa vào Git.
2. **Quy chuẩn thông điệp Commit**: Áp dụng chuẩn [Conventional Commits](https://www.conventionalcommits.org/):
   - `feat(...)`: Thêm tính năng mới.
   - `fix(...)`: Sửa lỗi phần mềm.
   - `perf(...)`: Cải tiến hiệu năng tải trang hoặc truy vấn CSDL.
   - `docs(...)`: Bổ sung hoặc chỉnh sửa tài liệu kỹ thuật.
   - `refactor(...)`: Tái cấu trúc code nhưng không thay đổi hành vi nghiệp vụ.
   - `test(...)`: Bổ sung hoặc cập nhật unit test / integration test.
3. **Bảo toàn ranh giới Tenant Isolation**: Mọi truy vấn mới vào database phải luôn chứa điều kiện lọc `b.tenant.id = :tenantId`.
4. **Phòng tránh N+1 Query**: Khi viết các hàm truy vấn JPA mới cần nạp các quan hệ ManyToOne, luôn sử dụng mệnh đề `JOIN FETCH` hoặc `@EntityGraph`.

---

Tài liệu liên quan:
- [Kiến trúc hệ thống (docs/ARCHITECTURE.md)](ARCHITECTURE.md)
- [Thiết kế Cơ sở Dữ liệu (docs/DATABASE.md)](DATABASE.md)
- [Danh mục API (docs/API.md)](API.md)
- [Bảo mật & Phân quyền (docs/SECURITY.md)](SECURITY.md)
- [Hướng dẫn Triển khai Production (docs/DEPLOYMENT.md)](DEPLOYMENT.md)
