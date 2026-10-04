package com.example.spabooking.export;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/v1/exports")
@PreAuthorize("hasRole('OWNER')")
public class ExportController {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExportService exportService;

    @Autowired
    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/bookings")
    public ResponseEntity<byte[]> exportBookings() {
        return xlsxResponse(exportService.exportBookings(), "bookings");
    }

    @GetMapping("/customers")
    public ResponseEntity<byte[]> exportCustomers() {
        return xlsxResponse(exportService.exportCustomers(), "customers");
    }

    @GetMapping("/revenue")
    public ResponseEntity<byte[]> exportRevenue() {
        return xlsxResponse(exportService.exportRevenue(), "revenue");
    }

    @GetMapping("/revenue/monthly")
    public ResponseEntity<byte[]> exportMonthlyRevenue(
            @org.springframework.web.bind.annotation.RequestParam("year") int year,
            @org.springframework.web.bind.annotation.RequestParam("month") int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Tháng phải từ 1 đến 12");
        }
        String filename = String.format("revenue-%04d-%02d.xlsx", year, month);
        ContentDisposition disposition = ContentDisposition.attachment().filename(filename).build();
        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(exportService.exportMonthlyRevenue(year, month));
    }

    private ResponseEntity<byte[]> xlsxResponse(byte[] body, String exportName) {
        String today = LocalDate.now(VIETNAM_ZONE).toString();
        String filename = exportName + "-" + today + ".xlsx";
        ContentDisposition disposition = ContentDisposition.attachment().filename(filename).build();
        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(body);
    }
}
