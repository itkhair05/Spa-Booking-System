package com.example.spabooking.common.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    private static final byte[] JPEG_BYTES = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0, 1};
    private static final byte[] PNG_BYTES = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13};
    private static final byte[] WEBP_BYTES = "RIFF1234WEBPVP8 ".getBytes(StandardCharsets.US_ASCII);

    @Test
    @DisplayName("Initialization creates upload root and category subdirectories")
    void testInitializationCreatesUploadDirectories(@TempDir Path tempDir) {
        Path uploadRoot = tempDir.resolve("test-uploads");
        FileStorageService storageService = new FileStorageService(uploadRoot.toString());

        assertTrue(Files.exists(uploadRoot), "Root upload directory should exist");
        assertTrue(Files.exists(uploadRoot.resolve("avatars")), "avatars directory should exist");
        assertTrue(Files.exists(uploadRoot.resolve("services")), "services directory should exist");
        assertTrue(Files.exists(uploadRoot.resolve("articles")), "articles directory should exist");
    }

    @Test
    @DisplayName("Store valid JPEG image in services category")
    void testStoreFileJpegSuccess(@TempDir Path tempDir) throws IOException {
        FileStorageService storageService = new FileStorageService(tempDir.toString());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.jpg",
                "image/jpeg",
                JPEG_BYTES
        );

        String fileUrl = storageService.storeFile(file, "services");

        assertNotNull(fileUrl);
        assertTrue(fileUrl.startsWith("/api/v1/uploads/services/"));
        assertTrue(fileUrl.endsWith(".jpg"));

        String filename = fileUrl.substring("/api/v1/uploads/services/".length());
        Path storedPath = tempDir.resolve("services").resolve(filename);
        assertTrue(Files.exists(storedPath));
        assertArrayEquals(JPEG_BYTES, Files.readAllBytes(storedPath));
    }

    @Test
    @DisplayName("Store valid PNG image in avatars category")
    void testStoreFilePngSuccess(@TempDir Path tempDir) {
        FileStorageService storageService = new FileStorageService(tempDir.toString());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                PNG_BYTES
        );

        String fileUrl = storageService.storeFile(file, "avatars");

        assertNotNull(fileUrl);
        assertTrue(fileUrl.startsWith("/api/v1/uploads/avatars/"));
        assertTrue(fileUrl.endsWith(".png"));
    }

    @Test
    @DisplayName("Store valid WEBP image in articles category")
    void testStoreFileWebpSuccess(@TempDir Path tempDir) {
        FileStorageService storageService = new FileStorageService(tempDir.toString());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cover.webp",
                "image/webp",
                WEBP_BYTES
        );

        String fileUrl = storageService.storeFile(file, "articles");

        assertNotNull(fileUrl);
        assertTrue(fileUrl.startsWith("/api/v1/uploads/articles/"));
        assertTrue(fileUrl.endsWith(".webp"));
    }

    @Test
    @DisplayName("Reject invalid category")
    void testStoreFileInvalidCategoryThrows(@TempDir Path tempDir) {
        FileStorageService storageService = new FileStorageService(tempDir.toString());

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", JPEG_BYTES);

        assertThrows(IllegalArgumentException.class, () -> storageService.storeFile(file, "unknown_category"));
    }

    @Test
    @DisplayName("Reject empty file")
    void testStoreFileEmptyThrows(@TempDir Path tempDir) {
        FileStorageService storageService = new FileStorageService(tempDir.toString());

        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> storageService.storeFile(emptyFile, "services"));
    }

    @Test
    @DisplayName("Reject invalid image file format (spoofed extension)")
    void testStoreFileInvalidImageContentThrows(@TempDir Path tempDir) {
        FileStorageService storageService = new FileStorageService(tempDir.toString());

        MockMultipartFile fakeImage = new MockMultipartFile(
                "file",
                "malicious.jpg",
                "image/jpeg",
                "NOT_AN_IMAGE_CONTENT".getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(IllegalArgumentException.class, () -> storageService.storeFile(fakeImage, "services"));
    }

    @Test
    @DisplayName("Load file as resource successfully")
    void testLoadFileAsResource(@TempDir Path tempDir) {
        FileStorageService storageService = new FileStorageService(tempDir.toString());

        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", PNG_BYTES);
        String fileUrl = storageService.storeFile(file, "avatars");
        String filename = fileUrl.substring("/api/v1/uploads/avatars/".length());

        Resource resource = storageService.loadFileAsResource("avatars", filename);
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    @DisplayName("Load file with invalid filename or traversal pattern throws IllegalArgumentException")
    void testLoadFileInvalidFilenameThrows(@TempDir Path tempDir) {
        FileStorageService storageService = new FileStorageService(tempDir.toString());

        assertThrows(IllegalArgumentException.class, () -> storageService.loadFileAsResource("avatars", "../secret.txt"));
        assertThrows(IllegalArgumentException.class, () -> storageService.loadFileAsResource("avatars", "invalid-name.exe"));
    }

    @Test
    @DisplayName("Delete stored file by URL")
    void testDeleteFileByUrl(@TempDir Path tempDir) {
        FileStorageService storageService = new FileStorageService(tempDir.toString());

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", JPEG_BYTES);
        String fileUrl = storageService.storeFile(file, "services");
        String filename = fileUrl.substring("/api/v1/uploads/services/".length());
        Path storedFile = tempDir.resolve("services").resolve(filename);

        assertTrue(Files.exists(storedFile));

        storageService.deleteFileByUrl(fileUrl);
        assertFalse(Files.exists(storedFile));
    }
}
