package com.example.spabooking.tenant.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testSetAndGetTenantId() {
        TenantContext.setTenantId(123L);
        assertEquals(123L, TenantContext.getTenantId());
    }

    @Test
    void testGetTenantIdWhenNotSet() {
        assertNull(TenantContext.getTenantId());
    }

    @Test
    void testRequireTenantIdWhenNotSet() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, TenantContext::requireTenantId);
        assertEquals("TenantContext cannot be resolved: tenant_id is missing", exception.getMessage());
    }

    @Test
    void testRequireTenantIdWhenSet() {
        TenantContext.setTenantId(456L);
        assertEquals(456L, TenantContext.requireTenantId());
    }

    @Test
    void testClear() {
        TenantContext.setTenantId(789L);
        TenantContext.clear();
        assertNull(TenantContext.getTenantId());
    }

    @Test
    void testSetClearGet() {
        TenantContext.setTenantId(999L);
        TenantContext.clear();
        assertNull(TenantContext.getTenantId());
    }

    @Test
    void testThreadLocalIsolation() throws InterruptedException {
        TenantContext.setTenantId(111L);

        AtomicReference<Long> thread1Tenant = new AtomicReference<>();
        AtomicReference<Long> thread2Tenant = new AtomicReference<>();

        CountDownLatch latch = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            TenantContext.setTenantId(222L);
            thread1Tenant.set(TenantContext.getTenantId());
            latch.countDown();
        });

        Thread t2 = new Thread(() -> {
            TenantContext.setTenantId(333L);
            thread2Tenant.set(TenantContext.getTenantId());
            latch.countDown();
        });

        t1.start();
        t2.start();
        latch.await();

        assertEquals(111L, TenantContext.getTenantId());
        assertEquals(222L, thread1Tenant.get());
        assertEquals(333L, thread2Tenant.get());
    }
}
