package com.example.spabooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@SpringBootApplication
public class SpaBookingApplication {

	public static void main(String[] args) {
		loadDotenv();
		SpringApplication.run(SpaBookingApplication.class, args);
	}

	private static void loadDotenv() {
		String[] candidatePaths = {".env", "backend/.env", "../.env"};
		for (String path : candidatePaths) {
			File file = new File(path);
			if (file.exists() && file.isFile()) {
				try {
					List<String> lines = Files.readAllLines(file.toPath());
					for (String line : lines) {
						String trimmed = line.trim();
						if (trimmed.isEmpty() || trimmed.startsWith("#")) {
							continue;
						}
						int eqIdx = trimmed.indexOf('=');
						if (eqIdx > 0) {
							String key = trimmed.substring(0, eqIdx).trim();
							String value = trimmed.substring(eqIdx + 1).trim();
							if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
								value = value.substring(1, value.length() - 1);
							} else if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
								value = value.substring(1, value.length() - 1);
							}
							if (!value.isEmpty() && System.getProperty(key) == null && System.getenv(key) == null) {
								System.setProperty(key, value);
							}
						}
					}
					break;
				} catch (Exception ignored) {
				}
			}
		}
	}

}
