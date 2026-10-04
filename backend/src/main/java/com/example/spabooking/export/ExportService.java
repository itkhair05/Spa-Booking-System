package com.example.spabooking.export;

import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.booking.enums.BookingStatus;
import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.customer.entity.Customer;
import com.example.spabooking.customer.repository.CustomerRepository;
import com.example.spabooking.tenant.context.TenantContext;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ExportService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int REVENUE_WINDOW_DAYS = 30;

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;

    @Autowired
    public ExportService(BookingRepository bookingRepository, CustomerRepository customerRepository) {
        this.bookingRepository = bookingRepository;
        this.customerRepository = customerRepository;
    }

    public byte[] exportBookings() {
        Long tenantId = TenantContext.requireTenantId();
        List<Booking> bookings = bookingRepository.findAllByFilters(tenantId, null, null, null, null);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Lich hen");
            Styles styles = new Styles(workbook);
            String[] headers = {"Mã lịch hẹn", "Khách hàng", "Số điện thoại", "Dịch vụ", "Nhân viên",
                    "Ngày", "Giờ bắt đầu", "Giờ kết thúc", "Giá (VNĐ)", "Trạng thái", "Ngày tạo"};
            headerRow(sheet, headers, styles);

            int rowIdx = 1;
            for (Booking booking : bookings) {
                Row row = sheet.createRow(rowIdx++);
                textCell(row, 0, booking.getBookingCode());
                textCell(row, 1, booking.getCustomer().getName());
                textCell(row, 2, booking.getCustomer().getPhone());
                textCell(row, 3, booking.getService().getName());
                textCell(row, 4, booking.getStaff() != null ? booking.getStaff().getName() : null);
                dateCell(row, 5, booking.getStartTime() != null ? booking.getStartTime().toLocalDate() : null, styles);
                timeCell(row, 6, booking.getStartTime(), styles);
                timeCell(row, 7, booking.getEndTime(), styles);
                moneyCell(row, 8, booking.getPrice(), styles);
                textCell(row, 9, statusLabel(booking.getStatus()));
                dateTimeCell(row, 10, booking.getCreatedAt(), styles);
            }
            return toBytes(workbook, sheet, headers.length);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] exportCustomers() {
        Long tenantId = TenantContext.requireTenantId();
        List<Customer> customers = customerRepository.findAllByTenantIdAndIsActiveTrue(tenantId);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Khach hang");
            Styles styles = new Styles(workbook);
            String[] headers = {"Họ tên", "Số điện thoại", "Email", "Ngày tạo"};
            headerRow(sheet, headers, styles);

            int rowIdx = 1;
            for (Customer customer : customers) {
                Row row = sheet.createRow(rowIdx++);
                textCell(row, 0, customer.getName());
                textCell(row, 1, customer.getPhone());
                textCell(row, 2, customer.getEmail());
                dateTimeCell(row, 3, customer.getCreatedAt(), styles);
            }
            return toBytes(workbook, sheet, headers.length);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] exportRevenue() {
        Long tenantId = TenantContext.requireTenantId();
        LocalDate today = LocalDate.now(VIETNAM_ZONE);
        LocalDateTime windowStart = today.minusDays(REVENUE_WINDOW_DAYS - 1).atStartOfDay();
        LocalDateTime windowEnd = today.plusDays(1).atStartOfDay();

        Map<LocalDate, List<Object[]>> rowsByDate = bookingRepository
                .findCompletedRevenueRows(tenantId, windowStart, windowEnd)
                .stream()
                .collect(Collectors.groupingBy(
                        row -> ((LocalDateTime) row[0]).toLocalDate(),
                        TreeMap::new,
                        Collectors.toList()));

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Doanh thu");
            Styles styles = new Styles(workbook);
            String[] headers = {"Ngày", "Số lịch hẹn hoàn thành", "Doanh thu (VNĐ)"};
            headerRow(sheet, headers, styles);

            int rowIdx = 1;
            long totalCount = 0;
            BigDecimal totalRevenue = BigDecimal.ZERO;
            for (Map.Entry<LocalDate, List<Object[]>> entry : rowsByDate.entrySet()) {
                List<Object[]> dayRows = entry.getValue();
                BigDecimal dayRevenue = dayRows.stream()
                        .map(row -> (BigDecimal) row[1])
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                Row row = sheet.createRow(rowIdx++);
                dateCell(row, 0, entry.getKey(), styles);
                row.createCell(1).setCellValue(dayRows.size());
                moneyCell(row, 2, dayRevenue, styles);
                totalCount += dayRows.size();
                totalRevenue = totalRevenue.add(dayRevenue);
            }

            Row totalRow = sheet.createRow(rowIdx);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("Tổng cộng");
            totalLabel.setCellStyle(styles.bold);
            Cell totalCountCell = totalRow.createCell(1);
            totalCountCell.setCellValue(totalCount);
            totalCountCell.setCellStyle(styles.bold);
            Cell totalRevenueCell = totalRow.createCell(2);
            totalRevenueCell.setCellValue(totalRevenue.doubleValue());
            totalRevenueCell.setCellStyle(styles.bold);

            return toBytes(workbook, sheet, headers.length);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String statusLabel(BookingStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case PENDING -> "Chờ xác nhận";
            case CONFIRMED -> "Đã xác nhận";
            case COMPLETED -> "Hoàn thành";
            case CANCELLED -> "Đã hủy";
        };
    }

    private void headerRow(Sheet sheet, String[] headers, Styles styles) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.header);
        }
    }

    private byte[] toBytes(Workbook workbook, Sheet sheet, int columnCount) throws IOException {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    private void textCell(Row row, int col, String value) {
        if (value != null) {
            row.createCell(col).setCellValue(value);
        }
    }

    private void moneyCell(Row row, int col, BigDecimal value, Styles styles) {
        if (value != null) {
            Cell cell = row.createCell(col);
            cell.setCellValue(value.doubleValue());
            cell.setCellStyle(styles.money);
        }
    }

    private void dateCell(Row row, int col, LocalDate value, Styles styles) {
        if (value != null) {
            Cell cell = row.createCell(col);
            cell.setCellValue(value);
            cell.setCellStyle(styles.date);
        }
    }

    private void timeCell(Row row, int col, LocalDateTime value, Styles styles) {
        if (value != null) {
            Cell cell = row.createCell(col);
            cell.setCellValue(value);
            cell.setCellStyle(styles.time);
        }
    }

    private void dateTimeCell(Row row, int col, LocalDateTime value, Styles styles) {
        if (value != null) {
            Cell cell = row.createCell(col);
            cell.setCellValue(value);
            cell.setCellStyle(styles.dateTime);
        }
    }

    private static final class Styles {
        final CellStyle header;
        final CellStyle money;
        final CellStyle date;
        final CellStyle time;
        final CellStyle dateTime;
        final CellStyle bold;

        Styles(Workbook workbook) {
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            header = workbook.createCellStyle();
            header.setFont(headerFont);
            header.setFillForegroundColor(new XSSFColor(new byte[]{0x46, 0x5d, 0x4c}, null));
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            money = workbook.createCellStyle();
            money.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));

            date = workbook.createCellStyle();
            date.setDataFormat(workbook.createDataFormat().getFormat("dd/MM/yyyy"));

            time = workbook.createCellStyle();
            time.setDataFormat(workbook.createDataFormat().getFormat("HH:mm"));

            dateTime = workbook.createCellStyle();
            dateTime.setDataFormat(workbook.createDataFormat().getFormat("dd/MM/yyyy HH:mm"));

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            bold = workbook.createCellStyle();
            bold.setFont(boldFont);
        }
    }
}
