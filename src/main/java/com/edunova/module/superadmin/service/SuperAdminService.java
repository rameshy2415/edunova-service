package com.edunova.module.superadmin.service;


import com.edunova.common.dto.*;
import com.edunova.enums.BillingCycle;
import com.edunova.enums.SubscriptionStatus;
import com.edunova.enums.UserRole;
import com.edunova.exception.BusinessException;
import com.edunova.exception.ConflictException;
import com.edunova.exception.ResourceNotFoundException;
import com.edunova.module.superadmin.entity.*;
import com.edunova.module.superadmin.repository.*;
import com.edunova.notification.email.EmailNotificationService;
import com.edunova.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAdminService {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SchoolOnboardingLogRepository onboardingLogRepository;
    private final PasswordEncoder             passwordEncoder;
    private final EmailNotificationService emailService;
    private final JwtUtil jwtUtil;

    @Value("${app.jwt.set-password-token-expiry-ms}")
    long setPasswordTokenExpiryMs;

    // ════════════════════════════════════════════════════════════
    //  DASHBOARD STATS
    // ════════════════════════════════════════════════════════════

    public PlatformStatsResponse getPlatformStats() {
        long total      = schoolRepository.count();
        long active     = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
        long trial      = subscriptionRepository.countByStatus(SubscriptionStatus.TRIAL);
        long expired    = subscriptionRepository.countByStatus(SubscriptionStatus.EXPIRED);

        // Rough student / teacher counts via user roles
        long students   = userRepository.countByRole(UserRole.STUDENT.name());
        long teachers   = userRepository.countByRole(UserRole.TEACHER.name());

        // Estimated MRR: sum effective monthly amounts of active subscriptions
       /* BigDecimal mrr = subscriptionRepository.findAll().stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE
                        || s.getStatus() == SubscriptionStatus.TRIAL)
                .map(this::effectiveMonthlyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);*/
        BigDecimal mrr = subscriptionRepository.findAll().stream()
                .filter(s -> SubscriptionStatus.ACTIVE.name().equals(s.getStatus())
                        || SubscriptionStatus.TRIAL.name().equals(s.getStatus()))
                .map(this::effectiveMonthlyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PlatformStatsResponse(total, active, trial, expired,
                students, teachers, mrr);
    }



    // ════════════════════════════════════════════════════════════
    //  SCHOOLS
    // ════════════════════════════════════════════════════════════
    //TODO need re-write to many Repo is getting called
    public PagedResponse<SchoolSummaryResponse> getSchools(
            String search, Boolean isActive, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<School> schoolPage = schoolRepository.findAllFiltered(search, isActive, pageable);

        List<SchoolSummaryResponse> content = schoolPage.getContent().stream()
                .map(this::toSchoolSummary)
                .toList();

        return new PagedResponse<>(content, page, size,
                schoolPage.getTotalElements(), schoolPage.getTotalPages(),
                schoolPage.isLast());
    }

    public SchoolDetailsResponse getSchoolById(UUID schoolId) {
        School school = findSchool(schoolId);
        Subscription subscription = subscriptionRepository.findBySchool_Id(schoolId).orElseThrow(()-> new RuntimeException("Subscription not found"));
        var adminUser = userRepository.findAdminsBySchoolId(schoolId);

        return new SchoolDetailsResponse(
                toSchoolResponse(school),
                toSubscriptionResponse(subscription),
                toAdminResponse(adminUser.get(0)));
       // return toSchoolResponse(school);
    }

    @Transactional
    public OnboardSchoolResponse onboardSchool(OnboardSchoolRequest request, User performedBy) {

        // 1. Guard: email must be unique
        if (schoolRepository.existsByEmail(request.school().email())) {
            throw new ConflictException(
                    "A school with email " + request.school().email() + " already exists.");
        }
        if (userRepository.existsByEmailAndSchool_Id(
                request.admin().email(), null)) {
            // admin email could clash with a superadmin account
            throw new ConflictException(
                    "Email " + request.admin().email() + " is already registered.");
        }

        // 2. Create School
        School school = School.builder()
                .name(request.school().name())
                .board(request.school().board())
                .address(request.school().address())
                .city(request.school().city())
                .state(request.school().state())
                .pincode(request.school().pincode())
                .phone(request.school().phone())
                .email(request.school().email())
                .website(request.school().website())
                .principalName(request.school().principalName())
                .establishedYear(request.school().establishedYear())
                .affiliationNo(request.school().affiliationNo())
                .isActive(true)
                .build();
        school = schoolRepository.save(school);



        // 3. Create Admin User for the school
        String[] name = request.admin().name().split(" ");
        User adminUser = User.builder()
                .school(school)
                .firstName(name[0])
                .lastName(name[1])
                .email(request.admin().email())
                //.passwordHash(passwordEncoder.encode(request.admin().tempPassword()))
                //.passwordResetToken(setPasswordToken)
                .passwordResetExpires(Instant.now().plusMillis(setPasswordTokenExpiryMs))
                .role(UserRole.SCHOOL_ADMIN.name())
                .isActive(true)
                .build();

        // 2. Create admin user — NO password; generate 2-hour set-password token
        String setPasswordToken =  jwtUtil.generateSetPasswordToken(adminUser);
        adminUser.setPasswordResetToken(setPasswordToken);

        adminUser = userRepository.save(adminUser);

        // 4. Create Subscription
        SubscriptionPlan plan = planRepository.findByNameIgnoreCase(request.subscription().plan())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subscription plan not found: " + request.subscription().planId()));

        LocalDate today    = LocalDate.now();
        int trialDays      = request.subscription().trialDays() != null ? request.subscription().trialDays() : 0;
        SubscriptionStatus initStatus = trialDays > 0 ? SubscriptionStatus.TRIAL : SubscriptionStatus.ACTIVE;
        LocalDate periodEnd = trialDays > 0 ? today.plusDays(trialDays) : periodEnd(today, request.subscription().billingCycle());

        Subscription subscription = Subscription.builder()
                .school(school)
                .plan(plan)
                .status(initStatus.getValue())
                //.billingCycle(request.subscription().billingCycle() != null ? request.subscription().billingCycle().name() : BillingCycle.ANNUAL.name())
                .billingCycle(BillingCycle.Annual)
                .amountOverride(request.subscription().amountOverride())
                .discountPct(request.subscription().discountPct() != null ? request.subscription().discountPct() : BigDecimal.ZERO)
                .trialEndsAt(trialDays > 0 ? today.plusDays(trialDays).atStartOfDay(ZoneOffset.UTC).toInstant() : null)
                .currentPeriodStart(today)
                .currentPeriodEnd(periodEnd)
                .internalNotes(request.subscription().notes())
                .createdBy(performedBy)
                .build();
        subscription = subscriptionRepository.save(subscription);

        // 5. Audit / onboarding log
        boolean emailSent = false;
        if (request.admin().sendWelcomeEmail()) {
            emailService.sendAdminWelcomeEmail(
                    adminUser.getEmail(),
                    request.admin().name(),
                    school.getName(),
                   //request.admin().tempPassword(),
                    setPasswordToken           // the link, NOT a password
            );
            emailSent = true;
        }

        SchoolOnboardingLog logEntry = SchoolOnboardingLog.builder()
                .school(school)
                .onboardedBy(performedBy)
                .plan(plan)
                .adminUser(adminUser)
                .adminEmail(adminUser.getEmail())
                .adminName(request.admin().name())
                .welcomeEmailSent(emailSent)
                .welcomeEmailSentAt(emailSent ? Instant.now() : null)
                .notes(request.subscription().notes())
                .build();
        onboardingLogRepository.save(logEntry);

        log.info("School '{}' onboarded by superadmin '{}'. Admin: {}",
                school.getName(), performedBy.getEmail(), adminUser.getEmail());

        return new OnboardSchoolResponse(
                toSchoolResponse(school),
                toSubscriptionResponse(subscription),
                toAdminResponse(adminUser),
                emailSent,
                "School onboarded. Welcome email with password-setup link sent to "
                        + adminUser.getEmail() + ". Link expires in 2 hours."
        );
    }

    @Transactional
    public SchoolResponse updateSchool(UUID schoolId, UpdateSchoolRequest request) {
        School school = findSchool(schoolId);
        if (request.name()            != null) school.setName(request.name());
        if (request.board()           != null) school.setBoard(request.board());
        if (request.address()         != null) school.setAddress(request.address());
        if (request.city()            != null) school.setCity(request.city());
        if (request.state()           != null) school.setState(request.state());
        if (request.pincode()         != null) school.setPincode(request.pincode());
        if (request.phone()           != null) school.setPhone(request.phone());
        if (request.email()           != null) school.setEmail(request.email());
        if (request.website()         != null) school.setWebsite(request.website());
        if (request.principalName()   != null) school.setPrincipalName(request.principalName());
        if (request.establishedYear() != null) school.setEstablishedYear(request.establishedYear());
        if (request.affiliationNo()   != null) school.setAffiliationNo(request.affiliationNo());
        if (request.isActive()        != null) school.setIsActive(request.isActive());
        return toSchoolResponse(schoolRepository.save(school));
    }

    @Transactional
    public void suspendSchool(UUID schoolId, String reason) {
        School school = findSchool(schoolId);
        school.setIsActive(false);
        schoolRepository.save(school);

        // Also suspend the subscription
        subscriptionRepository.findBySchool_Id(schoolId).ifPresent(sub -> {
            sub.setStatus(SubscriptionStatus.SUSPENDED.getValue());
            subscriptionRepository.save(sub);
        });

        // Disable all school users
        userRepository.findBySchool_IdAndRole(schoolId, null).forEach(u -> {
            if (!UserRole.SUPER_ADMIN.name().equals(u.getRole())) {
                u.setIsActive(false);
                userRepository.save(u);
            }
        });
        log.info("School {} suspended. Reason: {}", schoolId, reason);
    }

    @Transactional
    public void reinstateSchool(UUID schoolId) {
        School school = findSchool(schoolId);
        school.setIsActive(true);
        schoolRepository.save(school);

        subscriptionRepository.findBySchool_Id(schoolId).ifPresent(sub -> {
            sub.setStatus(SubscriptionStatus.ACTIVE.getValue());
            subscriptionRepository.save(sub);
        });

        // Re-enable admin users
        userRepository.findBySchool_IdAndRole(schoolId, UserRole.SCHOOL_ADMIN).forEach(u -> {
            u.setIsActive(true);
            userRepository.save(u);
        });
        log.info("School {} reinstated.", schoolId);
    }

    @Transactional
    public void deleteSchool(UUID schoolId) {
        School school = findSchool(schoolId);
        // Cascade handled by DB FK constraints
        schoolRepository.delete(school);
        log.warn("School {} permanently deleted.", schoolId);
    }

    // ════════════════════════════════════════════════════════════
    //  SUBSCRIPTIONS
    // ════════════════════════════════════════════════════════════

    public PagedResponse<SubscriptionResponse> getSubscriptions(
            SubscriptionStatus status, UUID planId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Subscription> subPage = subscriptionRepository.findAllFiltered(
                status, planId, pageable);

        List<SubscriptionResponse> content = subPage.getContent().stream()
                .map(this::toSubscriptionResponse)
                .toList();

        return new PagedResponse<>(content, page, size,
                subPage.getTotalElements(), subPage.getTotalPages(), subPage.isLast());
    }

    public SubscriptionResponse getSubscriptionById(UUID id) {
        return toSubscriptionResponse(findSubscription(id));
    }

    public SubscriptionResponse getSubscriptionBySchool(UUID schoolId) {
        Subscription sub = subscriptionRepository.findBySchool_Id(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No subscription found for school " + schoolId));
        return toSubscriptionResponse(sub);
    }

    @Transactional
    public SubscriptionResponse updateSubscription(
            UUID subscriptionId, UpdateSubscriptionRequest request) {

        Subscription sub = findSubscription(subscriptionId);

        if (request.planId() != null) {
            SubscriptionPlan plan = planRepository.findById(request.planId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Plan not found: " + request.planId()));
            sub.setPlan(plan);
        }
        if (request.billingCycle()    != null) sub.setBillingCycle(request.billingCycle());
        if (request.amountOverride()  != null) sub.setAmountOverride(request.amountOverride());
        if (request.discountPct()     != null) sub.setDiscountPct(request.discountPct());
        if (request.notes()           != null) sub.setInternalNotes(request.notes());

        return toSubscriptionResponse(subscriptionRepository.save(sub));
    }

    @Transactional
    public SubscriptionResponse renewSubscription(
            UUID subscriptionId, RenewSubscriptionRequest request) {

        Subscription sub = findSubscription(subscriptionId);
        sub.setCurrentPeriodStart(LocalDate.now());
        sub.setCurrentPeriodEnd(request.newPeriodEnd());
        sub.setStatus(SubscriptionStatus.ACTIVE.getValue());
        sub.setCancelledAt(null);
        sub.setCancellationReason(null);
        if (request.notes() != null) sub.setInternalNotes(request.notes());
        return toSubscriptionResponse(subscriptionRepository.save(sub));
    }

    @Transactional
    public void cancelSubscription(UUID subscriptionId, String reason) {
        Subscription sub = findSubscription(subscriptionId);
        sub.setStatus(SubscriptionStatus.CANCELLED.getValue());
        sub.setCancelledAt(Instant.now());
        sub.setCancellationReason(reason);
        subscriptionRepository.save(sub);
        log.info("Subscription {} cancelled. Reason: {}", subscriptionId, reason);
    }

    public List<SubscriptionPlanResponse> getPlans() {
        return planRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream().map(this::toPlanResponse).toList();
    }

    // ════════════════════════════════════════════════════════════
    //  SCHOOL_ADMIN ACCOUNTS
    // ════════════════════════════════════════════════════════════

    public PagedResponse<AdminUserResponse> getAdmins(
            UUID schoolId, String search, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        // Simple in-memory filter for now — replace with a repository query if needed
        List<User> admins = schoolId != null
                ? userRepository.findAdminsBySchoolId(schoolId)
                : userRepository.findAll().stream()
                .filter(u ->  UserRole.SCHOOL_ADMIN.equals(u.getRole()))
                .toList();

        if (search != null && !search.isBlank()) {
            String lc = search.toLowerCase();
            admins = admins.stream()
                    .filter(u -> u.getEmail().toLowerCase().contains(lc)
                            || (u.getSchool() != null
                            && u.getSchool().getName().toLowerCase().contains(lc)))
                    .toList();
        }

        int total = admins.size();
        int from  = page * size;
        int to    = Math.min(from + size, total);
        List<AdminUserResponse> content = admins.subList(from > total ? total : from, to)
                .stream().map(this::toAdminResponse).toList();

        int totalPages = (int) Math.ceil((double) total / size);
        return new PagedResponse<>(content, page, size, total, totalPages,
                (page + 1) >= totalPages);
    }

    public AdminUserResponse getAdminById(UUID adminId) {
        User admin = findUser(adminId);
        ensureRole(admin, UserRole.SCHOOL_ADMIN);
        return toAdminResponse(admin);
    }

    @Transactional
    public AdminUserResponse createAdmin(CreateAdminRequest request) {
        School school = findSchool(request.schoolId());

        if (userRepository.existsByEmailAndSchool_Id(request.email(), school.getId())) {
            throw new ConflictException("Email " + request.email()
                    + " is already registered for this school.");
        }

        User admin = User.builder()
                .school(school)
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.tempPassword()))
                .role(UserRole.SCHOOL_ADMIN.name())
                .isActive(true)
                .build();
        admin = userRepository.save(admin);

        if (request.sendWelcomeEmail()) {
            emailService.sendAdminWelcomeEmail(
                    admin.getEmail(), request.name(),
                    school.getName(), request.tempPassword());
        }
        log.info("Admin account created for school '{}': {}", school.getName(), admin.getEmail());
        return toAdminResponse(admin);
    }

    @Transactional
    public AdminUserResponse updateAdmin(UUID adminId, UpdateAdminRequest request) {
        User admin = findUser(adminId);
        ensureRole(admin, UserRole.SCHOOL_ADMIN);
        if (request.email() != null) admin.setEmail(request.email());
        return toAdminResponse(userRepository.save(admin));
    }

    @Transactional
    public void resetAdminPassword(UUID adminId) {
        User admin = findUser(adminId);
        ensureRole(admin, UserRole.SCHOOL_ADMIN);
        String token = UUID.randomUUID().toString();
        admin.setPasswordResetToken(token);
        admin.setPasswordResetExpires(Instant.now().plusSeconds(3600));
        userRepository.save(admin);
        emailService.sendPasswordResetEmail(admin.getEmail(), admin.getEmail(), token);
        log.info("Password reset triggered for admin {}", admin.getEmail());
    }

    @Transactional
    public void resendWelcomeEmail(UUID adminId) {
        User admin = findUser(adminId);
        ensureRole(admin, UserRole.SCHOOL_ADMIN);
        String tempPwd = "TempPass@" + (int)(Math.random() * 9000 + 1000);
        admin.setPasswordHash(passwordEncoder.encode(tempPwd));
        userRepository.save(admin);
        String schoolName = admin.getSchool() != null ? admin.getSchool().getName() : "EduNova";
        emailService.sendAdminWelcomeEmail(admin.getEmail(), admin.getEmail(), schoolName, tempPwd);
    }

    @Transactional
    public void disableAdmin(UUID adminId) {
        User admin = findUser(adminId);
        ensureRole(admin, UserRole.SCHOOL_ADMIN);
        admin.setIsActive(false);
        userRepository.save(admin);
        log.info("Admin {} disabled.", adminId);
    }

    @Transactional
    public void enableAdmin(UUID adminId) {
        User admin = findUser(adminId);
        ensureRole(admin, UserRole.SCHOOL_ADMIN);
        admin.setIsActive(true);
        userRepository.save(admin);
        log.info("Admin {} enabled.", adminId);
    }

    // ════════════════════════════════════════════════════════════
    //  ANALYTICS
    // ════════════════════════════════════════════════════════════

    public PlatformStatsResponse getAnalyticsOverview() {
        return getPlatformStats();
    }

    public List<MonthlyRevenuePoint> getRevenueData(LocalDate from, LocalDate to) {
        // For production: aggregate fee_payments by month from DB.
        // Stub returning mock data for demo.
        List<MonthlyRevenuePoint> points = new ArrayList<>();
        LocalDate cursor = from.withDayOfMonth(1);
        while (!cursor.isAfter(to.withDayOfMonth(1))) {
            long activeSubs = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
            BigDecimal rev  = BigDecimal.valueOf(activeSubs).multiply(BigDecimal.valueOf(9999));
            points.add(new MonthlyRevenuePoint(
                    cursor.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                    cursor.getYear(), rev, activeSubs));
            cursor = cursor.plusMonths(1);
        }
        return points;
    }

    public List<GrowthDataPoint> getGrowthData() {
        List<GrowthDataPoint> points = new ArrayList<>();
        LocalDate start  = LocalDate.now().minusMonths(5).withDayOfMonth(1);
        long totalSchools = schoolRepository.count();
        for (int i = 0; i <= 5; i++) {
            LocalDate month = start.plusMonths(i);
            points.add(new GrowthDataPoint(
                    month.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                    month.getYear(),
                    totalSchools - (5 - i) * 2,   // mock regression
                    i == 0 ? 0 : 2,
                    userRepository.countByRole(UserRole.STUDENT.name())
            ));
        }
        return points;
    }

    // ════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════

    private School findSchool(UUID id) {
        return schoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School not found: " + id));
    }

    private Subscription findSubscription(UUID id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + id));
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private void ensureRole(User user, UserRole required) {
        if ( !required.name().equals(user.getRole())) {
            throw new BusinessException("WRONG_ROLE",
                    "Expected role " + required + " but got " + user.getRole());
        }
    }

    private LocalDate periodEnd(LocalDate start, BillingCycle cycle) {
        if (cycle == null) return start.plusYears(1);
        return switch (cycle) {
            case MONTHLY   -> start.plusMonths(1);
            case QUARTERLY -> start.plusMonths(3);
            case Annual -> start.plusYears(1);
            case CUSTOM    -> start.plusYears(1);
        };
    }

    private BigDecimal effectiveMonthlyAmount(Subscription sub) {
        BigDecimal base = sub.getAmountOverride() != null
                ? sub.getAmountOverride()
                : (sub.getPlan().getPriceMonthly() != null
                ? sub.getPlan().getPriceMonthly() : BigDecimal.ZERO);
        if (sub.getDiscountPct() != null && sub.getDiscountPct().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = base.multiply(sub.getDiscountPct())
                    .divide(BigDecimal.valueOf(100));
            base = base.subtract(discount);
        }
        return base;
    }

    // ── Mappers ────────────────────────────────────────────────

    private SchoolResponse toSchoolResponse(School s) {
        return new SchoolResponse(
                s.getId(), s.getName(), s.getBoard(), s.getCity(), s.getState(),
                s.getPincode(), s.getPhone(), s.getEmail(), s.getWebsite(),
                s.getPrincipalName(), s.getEstablishedYear(), s.getAffiliationNo(),
                s.getAddress(), s.getIsActive(), s.getCreatedAt(),2840,100);
    }

    private SchoolSummaryResponse toSchoolSummary(School s) {
        String planName   = null;
        String planStatus = null;
        Optional<Subscription> sub = subscriptionRepository.findBySchool_Id(s.getId());
        if (sub.isPresent()) {
            planName   = sub.get().getPlan().getName();
            planStatus = sub.get().getStatus();
        }
        List<User> admins     = userRepository.findAdminsBySchoolId(s.getId()); //TODO Need to Revisit
        String adminName  = admins.isEmpty() ? null : admins.get(0).getFullName();
        String adminEmail = admins.isEmpty() ? null : admins.get(0).getEmail();

        return new SchoolSummaryResponse(
                s.getId(), s.getName(), s.getCity(), s.getState(), s.getIsActive(), planName, planStatus,
                userRepository.countBySchool_IdAndRole(s.getId(), UserRole.STUDENT.name()),
                userRepository.countBySchool_IdAndRole(s.getId(), UserRole.TEACHER.name()),
                adminName, adminEmail, s.getCreatedAt(), s.getCreatedAt(),s.getCreatedAt());
    }

    private SubscriptionResponse toSubscriptionResponse(Subscription sub) {
        return new SubscriptionResponse(
                sub.getId(),
                sub.getSchool().getId(),
                sub.getSchool().getName(),
                toPlanResponse(sub.getPlan()),
                sub.getStatus(),
                sub.getBillingCycle(),
                sub.getAmountOverride(),
                sub.getDiscountPct(),
                effectiveMonthlyAmount(sub),
                sub.getTrialEndsAt(),
                sub.getCurrentPeriodStart(),
                sub.getCurrentPeriodEnd(),
                sub.getCancelledAt(),
                sub.getCancellationReason(),
                sub.getInternalNotes(),
                sub.getCreatedAt(),
                sub.getUpdatedAt());
    }

    private SubscriptionPlanResponse toPlanResponse(SubscriptionPlan p) {
        return new SubscriptionPlanResponse(
                p.getId(), p.getName(), p.getMaxStudents(), p.getMaxTeachers(),
                p.getPriceMonthly(), p.getPriceAnnually(), p.getFeatures(), p.getIsActive());
    }

    private AdminUserResponse toAdminResponse(User u) {
        return new AdminUserResponse(
                u.getId(), u.getFullName(), u.getEmail(), u.getMobile(),
                u.getSchool() != null ? u.getSchool().getId()   : null,
                u.getSchool() != null ? u.getSchool().getName() : null,
                u.getIsActive(), u.getLastLoginAt(), u.getCreatedAt());
    }
}
