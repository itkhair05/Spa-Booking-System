package com.example.spabooking.publicapi.service;

import com.example.spabooking.booking.dto.CreateBookingRequest;
import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.booking.service.BookingService;
import com.example.spabooking.common.exception.ResourceNotFoundException;
import com.example.spabooking.customer.entity.Customer;
import com.example.spabooking.customer.repository.CustomerRepository;
import com.example.spabooking.publicapi.dto.*;
import com.example.spabooking.service.entity.Service;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffRepository;
import com.example.spabooking.tenant.context.TenantContext;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class PublicBookingService {

    private final TenantRepository tenantRepository;
    private final ServiceRepository serviceRepository;
    private final StaffRepository staffRepository;
    private final CustomerRepository customerRepository;
    private final BookingService bookingService;
    private final BookingRepository bookingRepository;

    @Autowired
    public PublicBookingService(TenantRepository tenantRepository,
                                ServiceRepository serviceRepository,
                                StaffRepository staffRepository,
                                CustomerRepository customerRepository,
                                BookingService bookingService,
                                BookingRepository bookingRepository) {
        this.tenantRepository = tenantRepository;
        this.serviceRepository = serviceRepository;
        this.staffRepository = staffRepository;
        this.customerRepository = customerRepository;
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
    }

    public PublicSpaInfoResponse getSpaInfo() {
        Long tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        return PublicSpaInfoResponse.fromEntity(tenant);
    }

    public List<PublicServiceResponse> getActiveServices() {
        Long tenantId = TenantContext.requireTenantId();
        return serviceRepository.findAllByTenantIdAndIsActiveTrue(tenantId)
                .stream()
                .map(PublicServiceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<PublicStaffResponse> getActiveStaff() {
        Long tenantId = TenantContext.requireTenantId();
        return staffRepository.findAllByTenantIdAndIsActiveTrue(tenantId)
                .stream()
                .map(PublicStaffResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<LocalDateTime> getAvailability(Long serviceId, LocalDate date, Long staffId) {
        Long tenantId = TenantContext.requireTenantId();
        
        Service service = serviceRepository.findByIdAndTenantId(serviceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
                
        if (!service.getIsActive()) {
            throw new IllegalArgumentException("Service is not active");
        }

        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Date cannot be in the past");
        }

        List<Staff> staffToCheck = new ArrayList<>();
        if (staffId != null) {
            Staff staff = staffRepository.findByIdAndTenantIdAndIsActiveTrue(staffId, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Staff not found or not active"));
            staffToCheck.add(staff);
        } else {
            staffToCheck = staffRepository.findAllByTenantIdAndIsActiveTrue(tenantId);
        }

        if (staffToCheck.isEmpty()) {
            return new ArrayList<>();
        }

        List<LocalDateTime> availableSlots = new ArrayList<>();
        
        // Simple business hours: 09:00 to 18:00
        LocalTime openTime = LocalTime.of(9, 0);
        LocalTime closeTime = LocalTime.of(18, 0);
        
        LocalDateTime startOfDay = date.atTime(openTime);
        LocalDateTime endOfDay = date.atTime(closeTime);
        
        // Check slots every 30 minutes
        LocalDateTime currentSlot = startOfDay;
        LocalDateTime now = LocalDateTime.now();

        while (currentSlot.plusMinutes(service.getDurationMinutes()).isBefore(endOfDay) || currentSlot.plusMinutes(service.getDurationMinutes()).equals(endOfDay)) {
            
            if (currentSlot.isBefore(now)) {
                currentSlot = currentSlot.plusMinutes(30);
                continue;
            }

            LocalDateTime slotEnd = currentSlot.plusMinutes(service.getDurationMinutes());
            
            boolean isAvailable = false;
            for (Staff staff : staffToCheck) {
                long overlaps = bookingRepository.countOverlappingStaffBookings(tenantId, staff.getId(), currentSlot, slotEnd, null);
                if (overlaps == 0) {
                    isAvailable = true;
                    break;
                }
            }
            
            if (isAvailable) {
                availableSlots.add(currentSlot);
            }
            
            currentSlot = currentSlot.plusMinutes(30);
        }

        return availableSlots;
    }

    @Transactional
    public PublicBookingResponse createBooking(CreatePublicBookingRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        
        Service service = serviceRepository.findByIdAndTenantId(request.getServiceId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        // Resolve or create Customer
        Customer customer;
        Optional<Customer> existingCustomer = customerRepository.findByPhoneAndTenantId(request.getCustomerPhone(), tenantId);
        if (existingCustomer.isPresent()) {
            customer = existingCustomer.get();
            if (!customer.getIsActive()) {
                throw new IllegalArgumentException("Customer account is inactive");
            }
            // Update name and email if provided
            customer.setName(request.getCustomerName());
            if (request.getCustomerEmail() != null && !request.getCustomerEmail().isEmpty()) {
                customer.setEmail(request.getCustomerEmail());
            }
            customer = customerRepository.save(customer);
        } else {
            customer = new Customer();
            customer.setName(request.getCustomerName());
            customer.setPhone(request.getCustomerPhone());
            customer.setEmail(request.getCustomerEmail());
            Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
            customer.setTenant(tenant);
            customer.setIsActive(true);
            customer = customerRepository.save(customer);
        }

        // Reuse BookingService creation to respect existing transactions and locking
        CreateBookingRequest internalRequest = new CreateBookingRequest();
        internalRequest.setCustomerId(customer.getId());
        internalRequest.setServiceId(service.getId());
        internalRequest.setStaffId(request.getStaffId());
        internalRequest.setStartTime(request.getStartTime());
        internalRequest.setEndTime(request.getStartTime().plusMinutes(service.getDurationMinutes()));

        Booking booking = bookingService.create(internalRequest);
        return PublicBookingResponse.fromEntity(booking);
    }

    @Transactional(readOnly = true)
    public PublicBookingDetailResponse getBookingByCode(String bookingCode) {
        Long tenantId = TenantContext.requireTenantId();
        Booking booking = bookingRepository.findByBookingCodeAndTenantId(bookingCode, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        return PublicBookingDetailResponse.fromEntity(booking);
    }
}
