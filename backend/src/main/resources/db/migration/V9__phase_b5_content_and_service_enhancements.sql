-- Migration V9: Phase B.5 Content, Service Enhancements, Categories, Articles and Reviews

-- 1. Create service_categories table
CREATE TABLE service_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    display_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_service_categories_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_service_categories_tenant ON service_categories(tenant_id, is_active, display_order);

-- 2. Alter services table to support categories, featured status and process steps
ALTER TABLE services
    ADD COLUMN category_id BIGINT NULL,
    ADD COLUMN is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN process_steps TEXT NULL,
    ADD CONSTRAINT fk_services_category FOREIGN KEY (category_id) REFERENCES service_categories(id) ON DELETE SET NULL;

CREATE INDEX idx_services_featured ON services(tenant_id, is_active, is_featured);

-- 3. Alter staff table to support public website visibility
ALTER TABLE staff
    ADD COLUMN show_on_website BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_staff_website_visibility ON staff(tenant_id, is_active, is_deleted, show_on_website);

-- 4. Create articles table for "Góc chăm sóc"
CREATE TABLE articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    read_time VARCHAR(50),
    excerpt TEXT,
    content MEDIUMTEXT NOT NULL,
    cover_image VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_articles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_articles_tenant_status ON articles(tenant_id, status, published_at DESC);

-- 5. Create reviews table for customer-facing testimonials & moderation
CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    rating INT NOT NULL DEFAULT 5,
    comment TEXT NOT NULL,
    service_name VARCHAR(255),
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    is_demo BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_reviews_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_reviews_tenant_published ON reviews(tenant_id, is_published, display_order ASC, created_at DESC);

-- 6. Initial Seed Data for TIKEY SPA (if tenant exists)
-- Categories
INSERT INTO service_categories (tenant_id, name, description, display_order, is_active)
SELECT id, 'Chăm sóc da', 'Các liệu trình làm sạch sâu, thanh lọc lỗ chân lông và cấp ẩm tầng sâu cho da mặt.', 1, TRUE
FROM tenants WHERE slug = 'tikey-spa';

INSERT INTO service_categories (tenant_id, name, description, display_order, is_active)
SELECT id, 'Massage & Trị liệu', 'Liệu pháp xoa bóp, ấn huyệt giải tỏa căng cơ, lưu thông khí huyết và phục hồi thể trạng.', 2, TRUE
FROM tenants WHERE slug = 'tikey-spa';

INSERT INTO service_categories (tenant_id, name, description, display_order, is_active)
SELECT id, 'Thư giãn & Dưỡng sinh', 'Liệu pháp chăm sóc toàn diện kết hợp tinh dầu thảo mộc và không gian thiền định.', 3, TRUE
FROM tenants WHERE slug = 'tikey-spa';

-- Link existing services to categories, set featured status and process steps
UPDATE services s
JOIN tenants t ON s.tenant_id = t.id AND t.slug = 'tikey-spa'
LEFT JOIN service_categories sc ON sc.tenant_id = t.id AND sc.name = 'Chăm sóc da'
SET s.category_id = sc.id,
    s.is_featured = TRUE,
    s.process_steps = '["1. Tẩy trang và làm sạch da mặt bằng sữa rửa mặt thảo mộc dịu nhẹ", "2. Xông hơi tinh dầu sả chanh mở lỗ chân lông & tẩy tế bào chết", "3. Hút bã nhờn và cặn bẩn bề mặt bằng đầu chân không vô trùng", "4. Massage bấm huyệt mặt thư giãn & kích hoạt lưu thông tuần hoàn máu", "5. Đắp mặt nạ thảo dược thiên nhiên làm dịu và cấp ẩm sâu", "6. Khóa ẩm bằng tinh chất dưỡng da và thoa kem chống nắng bảo vệ"]'
WHERE s.name LIKE '%Chăm sóc da mặt cơ bản%';

UPDATE services s
JOIN tenants t ON s.tenant_id = t.id AND t.slug = 'tikey-spa'
LEFT JOIN service_categories sc ON sc.tenant_id = t.id AND sc.name = 'Chăm sóc da'
SET s.category_id = sc.id,
    s.is_featured = TRUE,
    s.process_steps = '["1. Khám da và tư vấn phác đồ thanh lọc phù hợp", "2. Làm sạch kép với tinh dầu hạnh nhân và sữa rửa mặt chuyên sâu", "3. Xông hơi thảo dược & tẩy tế bào chết enzyme sinh học", "4. Sủi da bằng sóng siêu âm lấy đi dầu thừa và mụn cám nhẹ nhàng", "5. Điện di ion lạnh tinh chất vitamin C & Acid Hyaluronic tầng sâu", "6. Chiếu ánh sáng sinh học LED giảm đỏ và kích thích tăng sinh collagen", "7. Đắp mặt nạ thạch dừa collagen và dưỡng ẩm chuyên sâu"]'
WHERE s.name LIKE '%Làm sạch sâu da mặt%';

UPDATE services s
JOIN tenants t ON s.tenant_id = t.id AND t.slug = 'tikey-spa'
LEFT JOIN service_categories sc ON sc.tenant_id = t.id AND sc.name = 'Massage & Trị liệu'
SET s.category_id = sc.id,
    s.is_featured = TRUE,
    s.process_steps = '["1. Ngâm chân muối khoáng thảo dược và thư giãn khởi đầu", "2. Khởi động làm ấm các nhóm cơ toàn thân", "3. Miết dọc đường kinh lạc vùng lưng, vai gáy giải phóng co cứng", "4. Massage cánh tay, mu bàn tay và lòng bàn chân", "5. Bấm huyệt thái dương và xoa bóp đầu thư giãn tinh thần", "6. Thưởng thức trà thảo mộc ấm ấm sau trị liệu"]'
WHERE s.name LIKE '%Massage thư giãn cơ bản%';

UPDATE services s
JOIN tenants t ON s.tenant_id = t.id AND t.slug = 'tikey-spa'
LEFT JOIN service_categories sc ON sc.tenant_id = t.id AND sc.name = 'Massage & Trị liệu'
SET s.category_id = sc.id,
    s.is_featured = FALSE,
    s.process_steps = '["1. Ngâm chân nước ấm gừng tươi và thảo mộc khử hàn khí", "2. Thoa tinh dầu hữu cơ ấm toàn thân nhập khẩu từ Pháp", "3. Kỹ thuật miết cơ sâu Thụy Điển giải tỏa căng thẳng bó cơ", "4. Chườm đá nóng bazan tự nhiên giữ nhiệt sâu dọc cột sống", "5. Trị liệu chuyên sâu thắt lưng, hông và chân giảm nhức mỏi", "6. Massage bấm huyệt vùng đầu mặt và đánh thức cơ thể nhẹ nhàng"]'
WHERE s.name LIKE '%Massage toàn thân nâng cao%';

-- Seed 4 initial published articles
INSERT INTO articles (tenant_id, title, slug, category, read_time, excerpt, content, status, published_at)
SELECT id, 
       'Bí quyết phục hồi năng lượng và giải tỏa căng cơ sau tuần làm việc',
       'bi-quyet-phuc-hoi-nang-luong-va-giai-toa-cang-co',
       'Massage & Thư giãn',
       '5 phút đọc',
       'Lắng nghe cơ thể khi các nhóm cơ vai gáy bắt đầu báo động. Khám phá cách các chuyển động ấn huyệt kích hoạt tuần hoàn máu và tái tạo năng lượng.',
       'Cuộc sống hiện đại với nhiều giờ ngồi trước màn hình máy tính khiến vùng cổ, vai gáy và cột sống thắt lưng chịu áp lực rất lớn. Khi cơ bắp co cứng kéo dài, lưu thông máu lên não giảm, dẫn đến đau đầu, mệt mỏi và suy giảm chất lượng giấc ngủ.\n\nLiệu pháp massage trị liệu không chỉ đơn thuần là xoa bóp ngoài da. Bằng kỹ thuật miết dọc theo dải cơ và kích thích các điểm áp lực, các kỹ thuật viên giúp giải phóng axit lactic tích tụ, làm giãn bó cơ sâu và đưa cơ thể vào trạng thái thư giãn tối đa.\n\nMột liệu trình đều đặn mỗi 1–2 tuần là liều thuốc tự nhiên tuyệt vời để giữ cho tinh thần minh mẫn và cơ thể dẻo dai.',
       'PUBLISHED',
       NOW()
FROM tenants WHERE slug = 'tikey-spa';

INSERT INTO articles (tenant_id, title, slug, category, read_time, excerpt, content, status, published_at)
SELECT id, 
       'Quy trình chăm sóc da chuyên sâu: Tái sinh làn da mệt mỏi',
       'quy-trinh-cham-soc-da-chuyen-sau-tai-sinh-lan-da-met-moi',
       'Chăm sóc da',
       '4 phút đọc',
       'Tại sao làm sạch bề mặt là chưa đủ? Tìm hiểu quy trình làm sạch sâu, thanh lọc lỗ chân lông và cấp ẩm tầng sâu với dưỡng chất thảo mộc tại spa.',
       'Bụi mịn, ánh nắng mặt trời và mỹ phẩm trang điểm hàng ngày tích tụ sâu trong lỗ chân lông mà các bước tẩy trang thông thường khó lòng làm sạch triệt để. Theo thời gian, da trở nên xỉn màu, bít tắc và nhanh lão hóa.\n\nQuy trình chăm sóc da chuyên sâu tại TIKEY SPA kết hợp liệu pháp xông hơi thảo dược mở lỗ chân lông, làm sạch bã nhờn bằng sóng siêu âm nhẹ nhàng, và điện di tinh chất collagen cùng acid hyaluronic vào tầng trung bì.\n\nLàn da sau liệu trình không chỉ sáng mịn tức thì mà còn tăng cường hàng rào bảo vệ tự nhiên, giúp chống lại các tác nhân ô nhiễm môi trường.',
       'PUBLISHED',
       NOW()
FROM tenants WHERE slug = 'tikey-spa';

INSERT INTO articles (tenant_id, title, slug, category, read_time, excerpt, content, status, published_at)
SELECT id, 
       'Những lưu ý quan trọng trước và sau buổi trị liệu tại spa',
       'nhung-luu-y-quan-trong-truoc-va-sau-buoi-tri-lieu-tai-spa',
       'Tips dịch vụ',
       '3 phút đọc',
       'Để đạt hiệu quả tối đa cho mỗi buổi trị liệu, bạn nên chuẩn bị những gì và chăm sóc bản thân ra sao sau khi rời khỏi spa?',
       'Một buổi trị liệu hiệu quả bắt đầu từ việc chuẩn bị đúng cách. Trước khi đến spa 60 phút, bạn nên tránh ăn quá no hoặc sử dụng các chất kích thích như cà phê, rượu bia vì chúng làm tăng nhịp tim và khiến cơ thể khó chìm vào trạng thái tĩnh tại.\n\nHãy cởi mở chia sẻ với kỹ thuật viên về tình trạng sức khỏe hiện tại: bạn có vết thương hở, đang mang thai, hoặc đặc biệt nhạy cảm với vùng cơ nào không. Điều này giúp chuyên viên điều chỉnh lực bấm và liệu pháp phù hợp nhất với thể trạng của bạn.\n\nSau buổi trị liệu, cảm giác hơi lâng lâng hoặc buồn ngủ nhẹ là hoàn toàn bình thường khi cơ thể bắt đầu quá trình tự phục hồi.',
       'PUBLISHED',
       NOW()
FROM tenants WHERE slug = 'tikey-spa';

INSERT INTO articles (tenant_id, title, slug, category, read_time, excerpt, content, status, published_at)
SELECT id, 
       'Hương liệu pháp (Aromatherapy): Thư thái tâm trí từ tinh dầu thiên nhiên',
       'huong-lieu-phap-aromatherapy-thu-thai-tam-tri-tu-tinh-dau-thien-nhien',
       'Wellness',
       '6 phút đọc',
       'Hương thơm từ sả chanh, oải hương hay vỏ bưởi tác động thế nào đến sóng não và cảm xúc? Khám phá sức mạnh trị liệu của mùi hương.',
       'Khứu giác là giác quan duy nhất có đường dẫn thần kinh trực tiếp đến hệ viền — trung tâm điều khiển cảm xúc, trí nhớ và nhịp sinh học của não bộ. Đây là lý do một mùi hương quen thuộc có thể ngay lập tức xoa dịu lo âu.\n\nTại TIKEY SPA, chúng tôi tuyển chọn những loại tinh dầu nguyên chất chiết xuất tự nhiên: Tinh dầu Oải hương Pháp giúp xoa dịu hệ thần kinh và đưa giấc ngủ sâu; Tinh dầu Sả chanh & Vỏ bưởi Việt Nam thanh lọc không khí, khử khuẩn và nâng cao sinh khí.\n\nKhi kết hợp cùng hơi ấm từ đá bazan và đôi bàn tay ấm áp của chuyên viên, hương liệu pháp tạo nên một trải nghiệm đa giác quan khó quên.',
       'PUBLISHED',
       NOW()
FROM tenants WHERE slug = 'tikey-spa';

-- Seed initial reviews / showroom testimonials
INSERT INTO reviews (tenant_id, customer_name, rating, comment, service_name, is_published, is_demo, display_order)
SELECT id,
       'Chị Minh Anh',
       5,
       'Không gian tĩnh lặng vô cùng, bước vào là ngửi thấy mùi thảo mộc sả chanh dịu nhẹ. Kỹ thuật viên thao tác rất êm và có lực vừa phải, sau buổi massage cổ vai gáy của mình nhẹ nhõm hẳn.',
       'Massage Body Thụy Điển (60p)',
       TRUE,
       TRUE,
       1
FROM tenants WHERE slug = 'tikey-spa';

INSERT INTO reviews (tenant_id, customer_name, rating, comment, service_name, is_published, is_demo, display_order)
SELECT id,
       'Anh Tuấn Hùng',
       5,
       'Sau chuỗi ngày ngồi văn phòng đau thắt lưng, mình thử đặt lịch tại TIKEY SPA. Quy trình đặt lịch trên web rất nhanh không cần tạo tài khoản rườm rà. Chuyên viên rất hiểu huyệt đạo và tư vấn nhiệt tình.',
       'Trị Liệu Căng Cơ Chuyên Sâu (90p)',
       TRUE,
       TRUE,
       2
FROM tenants WHERE slug = 'tikey-spa';

INSERT INTO reviews (tenant_id, customer_name, rating, comment, service_name, is_published, is_demo, display_order)
SELECT id,
       'Chị Thu Thảo',
       5,
       'Dịch vụ chăm sóc da rất kỹ, các bước xông hơi và đắp mặt nạ thảo dược làm da mình mịn màng và sáng hẳn lên. Phòng ốc sạch sẽ, âm nhạc du dương tạo cảm giác an tâm tuyệt đối.',
       'Chăm Sóc Da Mặt Chuyên Sâu (60p)',
       TRUE,
       TRUE,
       3
FROM tenants WHERE slug = 'tikey-spa';

INSERT INTO reviews (tenant_id, customer_name, rating, comment, service_name, is_published, is_demo, display_order)
SELECT id,
       'Chị Hoàng Yến',
       5,
       'Rất ấn tượng với sự lễ phép và chu đáo của nhân viên. Nước gội nấu từ bồ kết và vỏ bưởi thật thơm, massage đầu rất đã giúp mình giảm hẳn triệu chứng mất ngủ đêm qua.',
       'Gội Đầu Dưỡng Sinh Thảo Mộc',
       TRUE,
       TRUE,
       4
FROM tenants WHERE slug = 'tikey-spa';
