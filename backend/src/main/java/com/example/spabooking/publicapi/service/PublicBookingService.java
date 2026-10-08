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
    private final com.example.spabooking.service.repository.ServiceCategoryRepository serviceCategoryRepository;
    private final StaffRepository staffRepository;
    private final CustomerRepository customerRepository;
    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final com.example.spabooking.article.service.ArticleService articleService;
    private final com.example.spabooking.review.service.ReviewService reviewService;
    private final com.example.spabooking.payment.service.PaymentService paymentService;
    private final com.example.spabooking.payment.repository.PaymentRepository paymentRepository;
    private final com.example.spabooking.payment.repository.RefundRepository refundRepository;
    private final com.example.spabooking.payment.service.VNPayService vnPayService;
    private final com.example.spabooking.staff.service.StaffScheduleService staffScheduleService;

    @Autowired
    public PublicBookingService(TenantRepository tenantRepository,
                                ServiceRepository serviceRepository,
                                com.example.spabooking.service.repository.ServiceCategoryRepository serviceCategoryRepository,
                                StaffRepository staffRepository,
                                CustomerRepository customerRepository,
                                BookingService bookingService,
                                BookingRepository bookingRepository,
                                com.example.spabooking.article.service.ArticleService articleService,
                                com.example.spabooking.review.service.ReviewService reviewService,
                                com.example.spabooking.payment.service.PaymentService paymentService,
                                com.example.spabooking.payment.repository.PaymentRepository paymentRepository,
                                com.example.spabooking.payment.repository.RefundRepository refundRepository,
                                com.example.spabooking.payment.service.VNPayService vnPayService,
                                com.example.spabooking.staff.service.StaffScheduleService staffScheduleService) {
        this.tenantRepository = tenantRepository;
        this.serviceRepository = serviceRepository;
        this.serviceCategoryRepository = serviceCategoryRepository;
        this.staffRepository = staffRepository;
        this.customerRepository = customerRepository;
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
        this.articleService = articleService;
        this.reviewService = reviewService;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.vnPayService = vnPayService;
        this.staffScheduleService = staffScheduleService;
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

    public List<PublicServiceResponse> getFeaturedServices() {
        Long tenantId = TenantContext.requireTenantId();
        return serviceRepository.findAllByTenantIdAndIsActiveTrueAndIsFeaturedTrue(tenantId)
                .stream()
                .map(PublicServiceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<PublicCategoryResponse> getActiveCategories() {
        Long tenantId = TenantContext.requireTenantId();
        return serviceCategoryRepository.findAllByTenantIdAndIsActiveTrueOrderByDisplayOrderAsc(tenantId)
                .stream()
                .map(PublicCategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<PublicStaffResponse> getActiveStaff() {
        Long tenantId = TenantContext.requireTenantId();
        return staffRepository.findAllByTenantIdAndIsActiveTrueAndIsDeletedFalseAndShowOnWebsiteTrue(tenantId)
                .stream()
                .map(PublicStaffResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<com.example.spabooking.article.dto.PublicArticleResponse> getPublishedArticles() {
        return articleService.findAllPublished()
                .stream()
                .map(com.example.spabooking.article.dto.PublicArticleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public com.example.spabooking.article.dto.PublicArticleResponse getPublishedArticle(String slugOrId) {
        return com.example.spabooking.article.dto.PublicArticleResponse.fromEntity(
                articleService.findPublishedBySlugOrId(slugOrId)
        );
    }

    public List<com.example.spabooking.review.dto.PublicReviewResponse> getPublishedReviews() {
        return reviewService.findAllPublished()
                .stream()
                .map(com.example.spabooking.review.dto.PublicReviewResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<LocalDateTime> getAvailability(Long serviceId, LocalDate date, Long staffId) {
        Long tenantId = TenantContext.requireTenantId();

        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Date cannot be in the past");
        }

        return staffScheduleService.getAvailableSlots(tenantId, serviceId, date, staffId);
    }

    @Transactional
    public PublicBookingResponse createBooking(CreatePublicBookingRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        
        Service service = serviceRepository.findByIdAndTenantId(request.getServiceId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        // Resolve or create Customer within tenant
        Customer customer;
        Optional<Customer> existingCustomer = customerRepository.findFirstByPhoneAndTenantIdOrderByIdAsc(request.getCustomerPhone(), tenantId);
        if (existingCustomer.isPresent()) {
            customer = existingCustomer.get();
            // Ensure customer is active so booking can be created
            if (Boolean.FALSE.equals(customer.getIsActive())) {
                customer.setIsActive(true);
            }
            // Preserve existing customer data: only set name if previously empty, or update email if existing email was blank
            if (customer.getName() == null || customer.getName().trim().isEmpty()) {
                customer.setName(request.getCustomerName());
            }
            if ((customer.getEmail() == null || customer.getEmail().trim().isEmpty())
                    && request.getCustomerEmail() != null && !request.getCustomerEmail().trim().isEmpty()) {
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

        // Process payment
        com.example.spabooking.payment.enums.PaymentMethod method =
                "VNPAY".equalsIgnoreCase(request.getPaymentMethod())
                        ? com.example.spabooking.payment.enums.PaymentMethod.VNPAY
                        : com.example.spabooking.payment.enums.PaymentMethod.PAY_AT_SPA;

        com.example.spabooking.payment.enums.PaymentProvider provider =
                (method == com.example.spabooking.payment.enums.PaymentMethod.VNPAY)
                        ? com.example.spabooking.payment.enums.PaymentProvider.VNPAY
                        : com.example.spabooking.payment.enums.PaymentProvider.LOCAL;

        com.example.spabooking.payment.entity.Payment payment =
                paymentService.createPaymentForBooking(booking, method, provider);

        PublicBookingResponse response = PublicBookingResponse.fromEntity(booking);
        response.setPaymentMethod(payment.getPaymentMethod().name());
        response.setPaymentStatus(payment.getStatus().name());

        if (method == com.example.spabooking.payment.enums.PaymentMethod.VNPAY) {
            String paymentUrl = vnPayService.createPaymentUrl(payment, null, null, null);
            response.setPaymentUrl(paymentUrl);
        }

        return response;
    }

    @Transactional(readOnly = true)
    public PublicBookingDetailResponse getBookingByCode(String bookingCode) {
        Long tenantId = TenantContext.requireTenantId();
        Booking booking = bookingRepository.findByBookingCodeAndTenantId(bookingCode, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        com.example.spabooking.payment.entity.Payment payment =
                paymentRepository.findByBookingIdAndTenantId(booking.getId(), tenantId).orElse(null);
        com.example.spabooking.payment.entity.Refund refund = payment != null
                ? refundRepository.findByPaymentIdAndTenantId(payment.getId(), tenantId).orElse(null)
                : null;
        return PublicBookingDetailResponse.fromEntity(booking, payment, refund);
    }

    @Transactional
    public com.example.spabooking.payment.dto.VNPayCallbackResult processVNPayCallback(java.util.Map<String, String> params) {
        return paymentService.processVNPayCallback(params);
    }

    @Transactional
    public com.example.spabooking.payment.dto.VNPayIpnResponse processVNPayIpn(java.util.Map<String, String> params) {
        return paymentService.processVNPayIpn(params);
    }
}
