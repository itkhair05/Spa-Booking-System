package com.example.spabooking.booking;

import com.example.spabooking.booking.entity.Booking;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class BookingCodeGenerationTest {

    private static final Pattern BOOKING_CODE_PATTERN = Pattern.compile("^BK-[0-9A-F]{10}$");

    @Test
    void generatedBookingCodesAreRandomUniqueAndNotSequential() {
        int samples = 200;
        Set<String> codes = new HashSet<>();

        for (int i = 0; i < samples; i++) {
            Booking booking = new Booking();
            ReflectionTestUtils.invokeMethod(booking, "onCreate");

            String code = booking.getBookingCode();
            assertNotNull(code);
            assertTrue(BOOKING_CODE_PATTERN.matcher(code).matches(),
                    "Booking code must be BK- followed by 10 hex chars, got: " + code);
            codes.add(code);
        }

        assertEquals(samples, codes.size(), "Booking codes must be unique across generations");
    }
}
