package com.example.spabooking.booking.service;

import com.example.spabooking.auth.security.CustomUserDetails;
import com.example.spabooking.booking.dto.BookingDetailResponse;
import com.example.spabooking.booking.dto.CreateBookingRequest;
import com.example.spabooking.booking.dto.UpdateBookingRequest;
import com.example.spabooking.booking.dto.UpdateBookingStatusRequest;
import com.example.spabooking.booking.entity.Booking;
import com.example.spabooking.booking.enums.BookingStatus;
import com.example.spabooking.booking.exception.BookingConflictException;
import com.example.spabooking.booking.repository.BookingRepository;
import com.example.spabooking.common.exception.ResourceNotFoundException;
import com.example.spabooking.customer.entity.Customer;
import com.example.spabooking.customer.repository.CustomerRepository;
import com.example.spabooking.service.repository.ServiceRepository;
import com.example.spabooking.staff.entity.Staff;
import com.example.spabooking.staff.repository.StaffRepository;
import com.example.spabooking.tenant.context.TenantContext;
import com.example.spabooking.tenant.entity.Tenant;
import com.example.spabooking.tenant.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final ServiceRepository serviceRepository;
    private final TenantRepository tenantRepository;

    @Autowired
    public BookingService(BookingRepository bookingRepository,
                          CustomerRepository customerRepository,
                          StaffRepository staffRepository,
                          ServiceRepository serviceRepository,
                          TenantRepository tenantRepository) {
        this.bookingRepository = bookingRepository;
        this.customerRepository = customerRepository;
        this.staffRepository = staffRepository;
        this.serviceRepository = serviceRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public List<Booking> findAll(Long requestedStaffId, LocalDateTime startDate, LocalDateTime endDate, BookingStatus status) {
        Long tenantId = TenantContext.requireTenantId();
        Long effectiveStaffId = requestedStaffId;

        Optional<CustomUserDetails> userDetailsOpt = getCurrentUserDetails();
        if (userDetailsOpt.isPresent() && userDetailsOpt.get().isStaff()) {
            CustomUserDetails userDetails = userDetailsOpt.get();
            Long linkedStaffId = userDetails.getStaffId();
            if (linkedStaffId != null) {
                if (requestedStaffId != null && !requestedStaffId.equals(linkedStaffId)) {
                    throw new AccessDeniedException("Staff can only view their own bookings");
                }
                effectiveStaffId = linkedStaffId;
            }
        }

        if (startDate != null && endDate != null) {
            if (endDate.isBefore(startDate)) {
                throw new IllegalArgumentException("endDate must not be before startDate");
            }
            if (java.time.Duration.between(startDate, endDate).toDays() > 90) {
                throw new IllegalArgumentException("Date range must not exceed 90 days");
            }
        } else if (startDate != null || endDate != null) {
            throw new IllegalArgumentException("Both startDate and endDate must be provided together");
        }

        return bookingRepository.findAllByFilters(tenantId, effectiveStaffId, startDate, endDate, status);
    }

    @Transactional(readOnly = true)
    public Optional<Booking> findById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Optional<Booking> bookingOpt = bookingRepository.findByIdAndTenantId(id, tenantId);
        if (bookingOpt.isEmpty()) {
            return Optional.empty();
        }

        Booking booking = bookingOpt.get();
        Optional<CustomUserDetails> userDetailsOpt = getCurrentUserDetails();
        if (userDetailsOpt.isPresent() && userDetailsOpt.get().isStaff()) {
            CustomUserDetails userDetails = userDetailsOpt.get();
            Long linkedStaffId = userDetails.getStaffId();
            if (linkedStaffId != null) {
                if (booking.getStaff() == null || !linkedStaffId.equals(booking.getStaff().getId())) {
                    throw new AccessDeniedException("Staff cannot access bookings assigned to another staff member");
                }
            }
        }
        return Optional.of(booking);
    }

    @Transactional(readOnly = true)
    public BookingDetailResponse getBookingDetail(Long id) {
        Booking booking = findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        boolean isOwner = getCurrentUserDetails().map(CustomUserDetails::isOwner).orElse(false);
        return BookingDetailResponse.fromEntity(booking, isOwner);
    }

    @Transactional
    public Booking create(CreateBookingRequest request) {
        Long tenantId = TenantContext.requireTenantId();

        Optional<CustomUserDetails> userDetailsOpt = getCurrentUserDetails();
        if (userDetailsOpt.isPresent() && userDetailsOpt.get().isStaff()) {
            CustomUserDetails userDetails = userDetailsOpt.get();
            Long linkedStaffId = userDetails.getStaffId();
            if (linkedStaffId != null) {
                if (request.getStaffId() != null && !request.getStaffId().equals(linkedStaffId)) {
                    throw new AccessDeniedException("Staff can only create bookings for themselves");
                }
                request.setStaffId(linkedStaffId);
            }
        }

        // Lock ordering: Customer -> Staff
        Customer customer = customerRepository.findByIdAndTenantIdAndIsActiveTrueForUpdate(request.getCustomerId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Staff staff = staffRepository.findByIdAndTenantIdAndIsActiveTrueForUpdate(request.getStaffId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        com.example.spabooking.service.entity.Service service = serviceRepository.findByIdAndTenantId(request.getServiceId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        if (!service.getIsActive()) {
            throw new IllegalArgumentException("Service must be active");
        }

        // endTime is derived from the service duration; any client-supplied value is ignored.
        LocalDateTime endTime = request.getStartTime().plusMinutes(service.getDurationMinutes());

        checkOverlaps(tenantId, staff.getId(), customer.getId(), request.getStartTime(), endTime, null);

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        Booking booking = new Booking();
        booking.setTenant(tenant);
        booking.setCustomer(customer);
        booking.setStaff(staff);
        booking.setService(service);
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(endTime);
        booking.setStatus(BookingStatus.PENDING);
        booking.setPrice(service.getPrice()); // Snapshot price

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking update(Long id, UpdateBookingRequest request) {
        Long tenantId = TenantContext.requireTenantId();

        Booking existingBooking = bookingRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        Optional<CustomUserDetails> userDetailsOpt = getCurrentUserDetails();
        if (userDetailsOpt.isPresent() && userDetailsOpt.get().isStaff()) {
            CustomUserDetails userDetails = userDetailsOpt.get();
            Long linkedStaffId = userDetails.getStaffId();
            if (linkedStaffId != null) {
                if (existingBooking.getStaff() == null || !linkedStaffId.equals(existingBooking.getStaff().getId())) {
                    throw new AccessDeniedException("Staff cannot update bookings assigned to another staff member");
                }
                if (request.getStaffId() != null && !request.getStaffId().equals(linkedStaffId)) {
                    throw new AccessDeniedException("Staff cannot reassign booking to another staff member");
                }
                request.setStaffId(linkedStaffId);
            }
        }

        // Lock ordering: Customer -> Staff
        Customer customer = customerRepository.findByIdAndTenantIdAndIsActiveTrueForUpdate(request.getCustomerId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Staff staff = staffRepository.findByIdAndTenantIdAndIsActiveTrueForUpdate(request.getStaffId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        com.example.spabooking.service.entity.Service service = serviceRepository.findByIdAndTenantId(request.getServiceId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        if (!service.getIsActive()) {
            throw new IllegalArgumentException("Service must be active");
        }

        // Don't modify if terminal
        if (existingBooking.getStatus() == BookingStatus.CANCELLED || existingBooking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot reschedule a cancelled or completed booking");
        }

        // endTime is derived from the (possibly changed) service duration; any client-supplied value is ignored.
        LocalDateTime endTime = request.getStartTime().plusMinutes(service.getDurationMinutes());

        checkOverlaps(tenantId, staff.getId(), customer.getId(), request.getStartTime(), endTime, id);

        existingBooking.setCustomer(customer);
        existingBooking.setStaff(staff);
        existingBooking.setService(service);
        existingBooking.setStartTime(request.getStartTime());
        existingBooking.setEndTime(endTime);
        // Do NOT update price on simple reschedule, it stays historical unless explicit pricing update.

        return bookingRepository.save(existingBooking);
    }

    @Transactional
    public Booking updateStatus(Long id, UpdateBookingStatusRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Booking existingBooking = bookingRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        Optional<CustomUserDetails> userDetailsOpt = getCurrentUserDetails();
        if (userDetailsOpt.isPresent() && userDetailsOpt.get().isStaff()) {
            CustomUserDetails userDetails = userDetailsOpt.get();
            Long linkedStaffId = userDetails.getStaffId();
            if (linkedStaffId != null) {
                if (existingBooking.getStaff() == null || !linkedStaffId.equals(existingBooking.getStaff().getId())) {
                    throw new AccessDeniedException("Staff cannot modify status of bookings assigned to another staff member");
                }
            }
        }

        BookingStatus current = existingBooking.getStatus();
        BookingStatus target = request.getStatus();

        if (current == BookingStatus.CANCELLED || current == BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot change status of a terminal booking");
        }

        if (current == BookingStatus.PENDING) {
            if (target != BookingStatus.CONFIRMED && target != BookingStatus.CANCELLED) {
                throw new IllegalArgumentException("Invalid status transition from PENDING");
            }
        } else if (current == BookingStatus.CONFIRMED) {
            if (target != BookingStatus.COMPLETED && target != BookingStatus.CANCELLED) {
                throw new IllegalArgumentException("Invalid status transition from CONFIRMED");
            }
        }

        existingBooking.setStatus(target);
        return bookingRepository.save(existingBooking);
    }

    @Transactional
    public Booking assignStaff(Long id, Long staffId) {
        Long tenantId = TenantContext.requireTenantId();

        Booking existingBooking = bookingRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (existingBooking.getStatus() == BookingStatus.CANCELLED || existingBooking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot reassign a terminal booking");
        }

        Staff newStaff = staffRepository.findByIdAndTenantIdAndIsActiveTrueForUpdate(staffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found or inactive"));

        long staffOverlaps = bookingRepository.countOverlappingStaffBookings(
                tenantId, newStaff.getId(), existingBooking.getStartTime(), existingBooking.getEndTime(), existingBooking.getId());
        if (staffOverlaps > 0) {
            throw new BookingConflictException("Staff member is already booked for this time slot");
        }

        existingBooking.setStaff(newStaff);
        return bookingRepository.save(existingBooking);
    }

    private void checkOverlaps(Long tenantId, Long staffId, Long customerId, LocalDateTime start, LocalDateTime end, Long excludeBookingId) {
        long staffOverlaps = bookingRepository.countOverlappingStaffBookings(tenantId, staffId, start, end, excludeBookingId);
        if (staffOverlaps > 0) {
            throw new BookingConflictException("Staff member is already booked for this time slot");
        }

        long customerOverlaps = bookingRepository.countOverlappingCustomerBookings(tenantId, customerId, start, end, excludeBookingId);
        if (customerOverlaps > 0) {
            throw new BookingConflictException("Customer is already booked for this time slot");
        }
    }

    private Optional<CustomUserDetails> getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return Optional.of(userDetails);
        }
        return Optional.empty();
    }
}
