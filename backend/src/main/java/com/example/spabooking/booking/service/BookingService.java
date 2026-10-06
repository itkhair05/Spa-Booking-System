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
import com.example.spabooking.payment.entity.Payment;
import com.example.spabooking.payment.repository.PaymentRepository;

import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final ServiceRepository serviceRepository;
    private final TenantRepository tenantRepository;
    private final PaymentRepository paymentRepository;

    @Autowired
    public BookingService(BookingRepository bookingRepository,
                          CustomerRepository customerRepository,
                          StaffRepository staffRepository,
                          ServiceRepository serviceRepository,
                          TenantRepository tenantRepository,
                          PaymentRepository paymentRepository) {
        this.bookingRepository = bookingRepository;
        this.customerRepository = customerRepository;
        this.staffRepository = staffRepository;
        this.serviceRepository = serviceRepository;
        this.tenantRepository = tenantRepository;
        this.paymentRepository = paymentRepository;
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
        Long tenantId = TenantContext.requireTenantId();
        Payment payment = paymentRepository.findByBookingIdAndTenantId(booking.getId(), tenantId).orElse(null);
        return BookingDetailResponse.fromEntity(booking, payment, isOwner);
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
    public Booking confirm(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Booking booking = bookingRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        ensureStaffAccess(booking, null);

        BookingStatus current = booking.getStatus();
        if (current == BookingStatus.CONFIRMED) {
            return booking;
        }
        if (current != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Invalid status transition to CONFIRMED from " + current);
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        if (booking.getConfirmedAt() == null) {
            booking.setConfirmedAt(LocalDateTime.now());
        }
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking checkIn(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Booking booking = bookingRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        ensureStaffAccess(booking, null);

        BookingStatus current = booking.getStatus();
        if (current == BookingStatus.CHECKED_IN) {
            return booking;
        }
        if (current != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Invalid status transition to CHECKED_IN from " + current);
        }

        booking.setStatus(BookingStatus.CHECKED_IN);
        if (booking.getCheckedInAt() == null) {
            booking.setCheckedInAt(LocalDateTime.now());
        }
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking start(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Booking booking = bookingRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        ensureStaffAccess(booking, null);

        BookingStatus current = booking.getStatus();
        if (current == BookingStatus.IN_PROGRESS) {
            return booking;
        }
        if (current != BookingStatus.CHECKED_IN) {
            throw new IllegalArgumentException("Invalid status transition to IN_PROGRESS from " + current);
        }

        booking.setStatus(BookingStatus.IN_PROGRESS);
        if (booking.getStartedAt() == null) {
            booking.setStartedAt(LocalDateTime.now());
        }
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking complete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Booking booking = bookingRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        ensureStaffAccess(booking, null);

        BookingStatus current = booking.getStatus();
        if (current == BookingStatus.COMPLETED) {
            return booking;
        }
        if (current != BookingStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Invalid status transition to COMPLETED from " + current);
        }

        booking.setStatus(BookingStatus.COMPLETED);
        if (booking.getCompletedAt() == null) {
            booking.setCompletedAt(LocalDateTime.now());
        }
        if (booking.getCustomer() != null) {
            booking.getCustomer().setLastVisit(LocalDateTime.now());
        }

        // Settle payment if Pay at Spa and unpaid
        paymentRepository.findByBookingIdAndTenantId(booking.getId(), tenantId).ifPresent(payment -> {
            if (payment.getPaymentMethod() == com.example.spabooking.payment.enums.PaymentMethod.PAY_AT_SPA
                    && payment.getStatus() == com.example.spabooking.payment.enums.PaymentStatus.UNPAID) {
                payment.setStatus(com.example.spabooking.payment.enums.PaymentStatus.PAID);
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);
            }
        });

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking cancel(Long id, String reason) {
        Long tenantId = TenantContext.requireTenantId();
        Booking booking = bookingRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        ensureStaffAccess(booking, null);

        BookingStatus current = booking.getStatus();
        if (current == BookingStatus.CANCELLED) {
            return booking;
        }
        if (current != BookingStatus.PENDING && current != BookingStatus.CONFIRMED && current != BookingStatus.CHECKED_IN) {
            throw new IllegalArgumentException("Cannot cancel booking with status " + current);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        if (booking.getCancelledAt() == null) {
            booking.setCancelledAt(LocalDateTime.now());
        }
        if (reason != null && !reason.trim().isEmpty()) {
            booking.setCancellationReason(reason.trim());
        }
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking noShow(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Booking booking = bookingRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        ensureStaffAccess(booking, null);

        BookingStatus current = booking.getStatus();
        if (current == BookingStatus.NO_SHOW) {
            return booking;
        }
        if (current != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Cannot mark no-show for booking with status " + current);
        }

        booking.setStatus(BookingStatus.NO_SHOW);
        if (booking.getNoShowAt() == null) {
            booking.setNoShowAt(LocalDateTime.now());
        }
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking reschedule(Long id, com.example.spabooking.booking.dto.RescheduleBookingRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Booking booking = bookingRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Cannot reschedule booking with status " + booking.getStatus());
        }

        if (request == null || request.getStartTime() == null) {
            throw new IllegalArgumentException("Start time is required for rescheduling");
        }

        Long targetStaffId = request.getStaffId() != null ? request.getStaffId() : booking.getStaff().getId();
        ensureStaffAccess(booking, targetStaffId);

        Staff staff = staffRepository.findByIdAndTenantIdAndIsActiveTrueForUpdate(targetStaffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found or inactive"));

        LocalDateTime endTime = request.getStartTime().plusMinutes(booking.getService().getDurationMinutes());
        checkOverlaps(tenantId, staff.getId(), booking.getCustomer().getId(), request.getStartTime(), endTime, booking.getId());

        booking.setStartTime(request.getStartTime());
        booking.setEndTime(endTime);
        booking.setStaff(staff);
        booking.setIsReminded(false);

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking updateStatus(Long id, UpdateBookingStatusRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new IllegalArgumentException("Status is required");
        }
        return switch (request.getStatus()) {
            case CONFIRMED -> confirm(id);
            case CHECKED_IN -> checkIn(id);
            case IN_PROGRESS -> start(id);
            case COMPLETED -> complete(id);
            case CANCELLED -> cancel(id, null);
            case NO_SHOW -> noShow(id);
            default -> throw new IllegalArgumentException("Unsupported status transition to " + request.getStatus());
        };
    }

    private void ensureStaffAccess(Booking booking, Long targetStaffId) {
        Optional<CustomUserDetails> userDetailsOpt = getCurrentUserDetails();
        if (userDetailsOpt.isPresent() && userDetailsOpt.get().isStaff()) {
            CustomUserDetails userDetails = userDetailsOpt.get();
            Long linkedStaffId = userDetails.getStaffId();
            if (linkedStaffId != null) {
                if (booking.getStaff() == null || !linkedStaffId.equals(booking.getStaff().getId())) {
                    throw new AccessDeniedException("Staff cannot modify bookings assigned to another staff member");
                }
                if (targetStaffId != null && !targetStaffId.equals(linkedStaffId)) {
                    throw new AccessDeniedException("Staff cannot reassign booking to another staff member");
                }
            }
        }
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
