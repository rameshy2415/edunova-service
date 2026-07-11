package com.edunova.module.superadmin.controller;

import com.edunova.common.dto.*;
import com.edunova.enums.SubscriptionStatus;
import com.edunova.module.superadmin.entity.User;
import com.edunova.module.superadmin.repository.UserRepository;
import com.edunova.module.superadmin.service.SuperAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Super Admin", description = "Platform management — schools, subscriptions, admins")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/superadmin")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequiredArgsConstructor
public class SuperAdminController {

    private final SuperAdminService superAdminService;
    private final UserRepository    userRepository;

    // ════════════════════════════════════════════════════════════
    //  DASHBOARD
    // ════════════════════════════════════════════════════════════

    @Operation(summary = "Platform-wide KPI stats")
    @GetMapping("/stats")
    public ResponseEntity<PlatformStatsResponse> getStats() {
        return ResponseEntity.ok(superAdminService.getPlatformStats());
    }

    // ════════════════════════════════════════════════════════════
    //  SCHOOLS
    // ════════════════════════════════════════════════════════════

    @Operation(summary = "List all schools (filterable, pageable)")
    @GetMapping("/schools")
    public ResponseEntity<PagedResponse<SchoolSummaryResponse>> listSchools(
            @Parameter(description = "Search by name or city")
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                superAdminService.getSchools(search, isActive, page, size));
    }

    @Operation(summary = "Get a single school by ID")
    @GetMapping("/schools/{id}")
    public ResponseEntity<SchoolDetailsResponse> getSchool(@PathVariable UUID id) {
        return ResponseEntity.ok(superAdminService.getSchoolById(id));
    }

    @Operation(
            summary = "Onboard a new school",
            description = "Creates school record + subscription + admin user in one atomic transaction. "
                    + "Optionally sends welcome email to admin."
    )
    @PostMapping("/schools/onboard")
    public ResponseEntity<OnboardSchoolResponse> onboardSchool(
            @Valid @RequestBody OnboardSchoolRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User superAdmin = resolveUser(userDetails);
        OnboardSchoolResponse result = superAdminService.onboardSchool(request, superAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    //TODO Admin Edit is not Working Request Does not hold Admin details which is coming from UI
    @Operation(summary = "Update school information")
    @PutMapping("/schools/{id}")
    public ResponseEntity<SchoolResponse> updateSchool(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSchoolRequest request
    ) {
        return ResponseEntity.ok(superAdminService.updateSchool(id, request));
    }

    @Operation(summary = "Suspend a school — disables all logins and sets subscription to SUSPENDED")
    @PostMapping("/schools/{id}/suspend")
    public ResponseEntity<NormalApiResponse> suspendSchool(
            @PathVariable UUID id,
            @RequestBody(required = false) java.util.Map<String, String> body
    ) {
        String reason = body != null ? body.getOrDefault("reason", "Suspended by super admin") : "";
        superAdminService.suspendSchool(id, reason);
        return ResponseEntity.ok(NormalApiResponse.ok("School suspended successfully."));
    }

    @Operation(summary = "Reinstate a previously suspended school")
    @PostMapping("/schools/{id}/reinstate")
    public ResponseEntity<NormalApiResponse> reinstateSchool(@PathVariable UUID id) {
        superAdminService.reinstateSchool(id);
        return ResponseEntity.ok(NormalApiResponse.ok("School reinstated successfully."));
    }

    @Operation(summary = "Permanently delete a school and all its data")
    @DeleteMapping("/schools/{id}")
    public ResponseEntity<NormalApiResponse> deleteSchool(@PathVariable UUID id) {
        superAdminService.deleteSchool(id);
        return ResponseEntity.ok(NormalApiResponse.ok("School deleted permanently."));
    }

    // ════════════════════════════════════════════════════════════
    //  SUBSCRIPTIONS
    // ════════════════════════════════════════════════════════════

    @Operation(summary = "List all subscriptions (filterable, pageable)")
    @GetMapping("/subscriptions")
    public ResponseEntity<PagedResponse<SubscriptionResponse>> listSubscriptions(
            @RequestParam(required = false) SubscriptionStatus status,
            @RequestParam(required = false) UUID planId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                superAdminService.getSubscriptions(status, planId, page, size));
    }

    @Operation(summary = "Get subscription by ID")
    @GetMapping("/subscriptions/{id}")
    public ResponseEntity<SubscriptionResponse> getSubscription(@PathVariable UUID id) {
        return ResponseEntity.ok(superAdminService.getSubscriptionById(id));
    }

    @Operation(summary = "Get subscription for a specific school")
    @GetMapping("/schools/{schoolId}/subscription")
    public ResponseEntity<SubscriptionResponse> getSchoolSubscription(
            @PathVariable UUID schoolId) {
        return ResponseEntity.ok(superAdminService.getSubscriptionBySchool(schoolId));
    }

    @Operation(summary = "Update subscription plan / billing / discount")
    @PutMapping("/subscriptions/{id}")
    public ResponseEntity<SubscriptionResponse> updateSubscription(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSubscriptionRequest request
    ) {
        return ResponseEntity.ok(superAdminService.updateSubscription(id, request));
    }

    @Operation(summary = "Renew subscription — extend period end date and set status to ACTIVE")
    @PostMapping("/subscriptions/{id}/renew")
    public ResponseEntity<SubscriptionResponse> renewSubscription(
            @PathVariable UUID id,
            @Valid @RequestBody RenewSubscriptionRequest request
    ) {
        return ResponseEntity.ok(superAdminService.renewSubscription(id, request));
    }

    @Operation(summary = "Cancel a subscription")
    @PostMapping("/subscriptions/{id}/cancel")
    public ResponseEntity<NormalApiResponse> cancelSubscription(
            @PathVariable UUID id,
            @Valid @RequestBody CancelSubscriptionRequest request
    ) {
        superAdminService.cancelSubscription(id, request.reason());
        return ResponseEntity.ok(NormalApiResponse.ok("Subscription cancelled."));
    }

    @Operation(summary = "List all available subscription plans")
    @GetMapping("/subscription-plans")
    public ResponseEntity<List<SubscriptionPlanResponse>> getPlans() {
        return ResponseEntity.ok(superAdminService.getPlans());
    }

    // ════════════════════════════════════════════════════════════
    //  ADMIN ACCOUNTS
    // ════════════════════════════════════════════════════════════

    @Operation(summary = "List all school admin accounts (filterable)")
    @GetMapping("/admins")
    public ResponseEntity<PagedResponse<AdminUserResponse>> listAdmins(
            @RequestParam(required = false) UUID schoolId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                superAdminService.getAdmins(schoolId, search, page, size));
    }

    @Operation(summary = "Get a single admin account by ID")
    @GetMapping("/admins/{id}")
    public ResponseEntity<AdminUserResponse> getAdmin(@PathVariable UUID id) {
        return ResponseEntity.ok(superAdminService.getAdminById(id));
    }

    @Operation(
            summary = "Create a new admin account for a school",
            description = "Can be used to add a second admin or replace an existing one."
    )
    @PostMapping("/admins")
    public ResponseEntity<AdminUserResponse> createAdmin(
            @Valid @RequestBody CreateAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(superAdminService.createAdmin(request));
    }

    @Operation(summary = "Update admin name / email / phone")
    @PutMapping("/admins/{id}")
    public ResponseEntity<AdminUserResponse> updateAdmin(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAdminRequest request
    ) {
        return ResponseEntity.ok(superAdminService.updateAdmin(id, request));
    }

    @Operation(summary = "Send password reset email to admin")
    @PostMapping("/admins/{id}/reset-password")
    public ResponseEntity<NormalApiResponse> resetAdminPassword(@PathVariable UUID id) {
        superAdminService.resetAdminPassword(id);
        return ResponseEntity.ok(NormalApiResponse.ok("Password reset email sent."));
    }

    @Operation(summary = "Resend welcome email with new temporary credentials")
    @PostMapping("/admins/{id}/resend-welcome")
    public ResponseEntity<NormalApiResponse> resendWelcome(@PathVariable UUID id) {
        superAdminService.resendWelcomeEmail(id);
        return ResponseEntity.ok(NormalApiResponse.ok("Welcome email resent."));
    }

    @Operation(summary = "Disable an admin account — prevents login")
    @PostMapping("/admins/{id}/disable")
    public ResponseEntity<NormalApiResponse> disableAdmin(@PathVariable UUID id) {
        superAdminService.disableAdmin(id);
        return ResponseEntity.ok(NormalApiResponse.ok("Admin account disabled."));
    }

    @Operation(summary = "Re-enable a previously disabled admin account")
    @PostMapping("/admins/{id}/enable")
    public ResponseEntity<NormalApiResponse> enableAdmin(@PathVariable UUID id) {
        superAdminService.enableAdmin(id);
        return ResponseEntity.ok(NormalApiResponse.ok("Admin account enabled."));
    }

    // ════════════════════════════════════════════════════════════
    //  ANALYTICS
    // ════════════════════════════════════════════════════════════

    @Operation(summary = "Platform-wide analytics overview")
    @GetMapping("/analytics/overview")
    public ResponseEntity<PlatformStatsResponse> analyticsOverview() {
        return ResponseEntity.ok(superAdminService.getAnalyticsOverview());
    }

    @Operation(summary = "Monthly revenue data (last 12 months by default)")
    @GetMapping("/analytics/revenue")
    public ResponseEntity<List<MonthlyRevenuePoint>> revenueData(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        if (from == null) from = LocalDate.now().minusMonths(11).withDayOfMonth(1);
        if (to   == null) to   = LocalDate.now();
        return ResponseEntity.ok(superAdminService.getRevenueData(from, to));
    }

    @Operation(summary = "School & student growth trend")
    @GetMapping("/analytics/growth")
    public ResponseEntity<List<GrowthDataPoint>> growthData() {
        return ResponseEntity.ok(superAdminService.getGrowthData());
    }

    // ════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════

    private User resolveUser(UserDetails userDetails) {
        //UUID id = UUID.fromString(userDetails.getUsername());
       // userRepository.findByEmailAndSchoolIsNull(userDetails.getUsername())
        return userRepository.findByEmailAndSchoolIsNull(userDetails.getUsername()).orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
