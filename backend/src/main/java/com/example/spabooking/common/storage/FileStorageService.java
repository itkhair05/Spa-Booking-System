package com.example.spabooking.common.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_CATEGORIES = Set.of("avatars", "services", "articles");
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-]+\\.(jpg|jpeg|png|webp)$");

    private final Path rootLocation;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
            Files.createDirectories(this.rootLocation.resolve("avatars"));
            Files.createDirectories(this.rootLocation.resolve("services"));
            Files.createDirectories(this.rootLocation.resolve("articles"));
            logger.info("Upload storage initialized at: {} (writable: {})",
                    this.rootLocation, Files.isWritable(this.rootLocation));
        } catch (IOException e) {
            logger.warn("Could not pre-create upload directories under {}: {}", this.rootLocation, e.getMessage());
        }
    }

    public String storeFile(MultipartFile file, String category) {
        if (!ALLOWED_CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("Danh mục tải lên không hợp lệ: " + category);
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Tệp tải lên không được để trống");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Kích thước tệp vượt quá giới hạn cho phép (tối đa 5MB)");
        }

        String extension = validateAndDetectImageExtension(file);
        String safeFileName = UUID.randomUUID().toString() + extension;

        try {
            Path categoryDir = this.rootLocation.resolve(category).normalize();
            if (!categoryDir.startsWith(this.rootLocation)) {
                throw new SecurityException("Phát hiện đường dẫn lưu trữ không an toàn");
            }
            Files.createDirectories(categoryDir);

            Path destinationPath = categoryDir.resolve(safeFileName).normalize();
            if (!destinationPath.startsWith(categoryDir)) {
                throw new SecurityException("Phát hiện tên tệp không an toàn");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return "/api/v1/uploads/" + category + "/" + safeFileName;
        } catch (IOException e) {
            logger.error("Failed to store file: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể lưu trữ tệp tải lên: " + e.getMessage());
        }
    }

    public Resource loadFileAsResource(String category, String filename) {
        if (!ALLOWED_CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("Danh mục không hợp lệ: " + category);
        }

        if (filename == null || !SAFE_FILENAME_PATTERN.matcher(filename).matches()) {
            throw new IllegalArgumentException("Tên tệp không hợp lệ: " + filename);
        }

        try {
            Path filePath = this.rootLocation.resolve(category).resolve(filename).normalize();
            if (!filePath.startsWith(this.rootLocation)) {
                throw new SecurityException("Không được phép truy cập ngoài thư mục lưu trữ");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IllegalArgumentException("Không tìm thấy tệp: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Lỗi định dạng đường dẫn tệp", e);
        }
    }

    public void deleteFileByUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/api/v1/uploads/")) {
            return;
        }

        String sub = fileUrl.substring("/api/v1/uploads/".length());
        String[] parts = sub.split("/");
        if (parts.length != 2) return;

        String category = parts[0];
        String filename = parts[1];

        if (!ALLOWED_CATEGORIES.contains(category) || !SAFE_FILENAME_PATTERN.matcher(filename).matches()) {
            return;
        }

        try {
            Path filePath = this.rootLocation.resolve(category).resolve(filename).normalize();
            if (filePath.startsWith(this.rootLocation)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            logger.warn("Could not delete file {}: {}", fileUrl, e.getMessage());
        }
    }

    private String validateAndDetectImageExtension(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header);
            if (read < 4) {
                throw new IllegalArgumentException("Tệp ảnh không hợp lệ");
            }

            // JPEG magic bytes: FF D8 FF
            if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
                return ".jpg";
            }

            // PNG magic bytes: 89 50 4E 47
            if ((header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
                return ".png";
            }

            // WEBP magic bytes: RIFF .... WEBP
            if (read >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
                return ".webp";
            }

            throw new IllegalArgumentException("Chỉ chấp nhận tệp hình ảnh định dạng JPEG, PNG hoặc WEBP");
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể đọc tệp để xác thực: " + e.getMessage());
        }
    }
}
