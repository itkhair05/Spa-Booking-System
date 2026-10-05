package com.example.spabooking.customer.service;

import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.customer.dto.CustomerResponse;
import com.example.spabooking.customer.entity.Customer;
import com.example.spabooking.customer.repository.CustomerRepository;
import com.example.spabooking.tenant.context.TenantContext;
import com.example.spabooking.common.exception.ResourceNotFoundException;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final TenantRepository tenantRepository;
    private final BookingRepository bookingRepository;

    @Autowired
    public CustomerService(CustomerRepository customerRepository,
                           TenantRepository tenantRepository,
                           BookingRepository bookingRepository) {
        this.customerRepository = customerRepository;
        this.tenantRepository = tenantRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<Customer> findAll() {
        Long tenantId = TenantContext.requireTenantId();
        return customerRepository.findAllByTenantIdAndIsActiveTrue(tenantId);
    }

    public List<CustomerResponse> findAllWithStats(String search, String category) {
        Long tenantId = TenantContext.requireTenantId();
        String query = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String digitsOnly = (query != null) ? query.replaceAll("[^0-9]", "") : "";

        List<Customer> customers = customerRepository.searchCustomers(tenantId, query, digitsOnly);
        List<Object[]> statsList = bookingRepository.findCustomerBookingStatsByTenantId(tenantId);

        Map<Long, Long> countMap = new HashMap<>();
        Map<Long, LocalDateTime> lastBookingMap = new HashMap<>();
        for (Object[] row : statsList) {
            if (row != null && row.length >= 3 && row[0] != null) {
                Long custId = (Long) row[0];
                Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                LocalDateTime lastTime = (LocalDateTime) row[2];
                countMap.put(custId, count);
                lastBookingMap.put(custId, lastTime);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        LocalDateTime sevenDaysAgo = now.minusDays(7);

        return customers.stream()
                .map(c -> {
                    CustomerResponse resp = CustomerResponse.fromEntity(c);
                    Long bCount = countMap.getOrDefault(c.getId(), 0L);
                    LocalDateTime lastB = lastBookingMap.get(c.getId());
                    resp.setTotalBookings(bCount);
                    resp.setLastBookingAt(lastB);
                    return resp;
                })
                .filter(resp -> {
                    if (category == null || category.isBlank() || "all".equalsIgnoreCase(category)) {
                        return true;
                    }
                    if ("new".equalsIgnoreCase(category)) {
                        return resp.getTotalBookings() == 0 || (resp.getCreatedAt() != null && resp.getCreatedAt().isAfter(thirtyDaysAgo));
                    }
                    if ("has_bookings".equalsIgnoreCase(category)) {
                        return resp.getTotalBookings() > 0;
                    }
                    if ("recent".equalsIgnoreCase(category)) {
                        boolean recentVisit = resp.getLastVisit() != null && resp.getLastVisit().isAfter(thirtyDaysAgo);
                        boolean recentBooking = resp.getLastBookingAt() != null && resp.getLastBookingAt().isAfter(thirtyDaysAgo);
                        boolean recentlyCreated = resp.getCreatedAt() != null && resp.getCreatedAt().isAfter(sevenDaysAgo);
                        return recentVisit || recentBooking || recentlyCreated;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    public Optional<Customer> findById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        return customerRepository.findByIdAndTenantIdAndIsActiveTrue(id, tenantId);
    }

    public Customer create(Customer customer) {
        Long tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        customer.setTenant(tenant);
        return customerRepository.save(customer);
    }

    public Customer update(Long id, Customer updatedDetails) {
        Long tenantId = TenantContext.requireTenantId();
        Customer existingCustomer = customerRepository.findByIdAndTenantIdAndIsActiveTrue(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        existingCustomer.setName(updatedDetails.getName());
        existingCustomer.setPhone(updatedDetails.getPhone());
        existingCustomer.setEmail(updatedDetails.getEmail());
        // lastVisit is untouched here.

        return customerRepository.save(existingCustomer);
    }

    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Customer existingCustomer = customerRepository.findByIdAndTenantIdAndIsActiveTrue(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        existingCustomer.setIsActive(false);
        customerRepository.save(existingCustomer);
    }
}
