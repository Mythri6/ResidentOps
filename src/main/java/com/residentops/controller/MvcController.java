package com.residentops.controller;

import com.residentops.model.*;
import com.residentops.model.enums.*;
import com.residentops.repository.MaintenanceRequestRepository;
import com.residentops.service.*;
import com.residentops.singleton.AuditLogger;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Spring MVC Controller — all Thymeleaf page routes.
 * M = Model (JPA entities via services)
 * V = View  (Thymeleaf templates in /templates/)
 * C = Controller (this class)
 */
@Controller
public class MvcController {

    private final UserService             userService;
    private final MaintenanceService      maintenanceService;
    private final ComplaintService        complaintService;
    private final VotingService           votingService;
    private final ExpenseService          expenseService;
    private final NoticeService           noticeService;
    private final AuditLogger             auditLogger;
    private final VendorConcernService    vendorConcernService;
    private final MaintenanceRequestRepository requestRepo;

    // Upload directory for proof docs
    private static final String UPLOAD_DIR = "uploads/";

    public MvcController(UserService userService,
                         MaintenanceService maintenanceService,
                         ComplaintService complaintService,
                         VotingService votingService,
                         ExpenseService expenseService,
                         NoticeService noticeService,
                         AuditLogger auditLogger,
                         VendorConcernService vendorConcernService,
                         MaintenanceRequestRepository requestRepo) {
        this.userService          = userService;
        this.maintenanceService   = maintenanceService;
        this.complaintService     = complaintService;
        this.votingService        = votingService;
        this.expenseService       = expenseService;
        this.noticeService        = noticeService;
        this.auditLogger          = auditLogger;
        this.vendorConcernService = vendorConcernService;
        this.requestRepo          = requestRepo;
        // Ensure upload directory exists
        new File(UPLOAD_DIR).mkdirs();
    }

    // ═══════════════════════════════════════════════════════════
    //  AUTH
    // ═══════════════════════════════════════════════════════════

    @GetMapping({"/", "/login"})
    public String loginPage(HttpSession session, Model model) {
        if (session.getAttribute("user") != null) return "redirect:/dashboard";
        // Get admin email for forgot-password message
        List<User> admins = userService.getByRole(Role.COMMITTEE_ADMIN);
        String adminEmail = admins.isEmpty() ? "admin@residentops.com" : admins.get(0).getEmail();
        model.addAttribute("adminEmail", adminEmail);
        return "login";
    }

    @PostMapping("/login")
    public String loginSubmit(@RequestParam String email,
                              @RequestParam String password,
                              HttpSession session, Model model) {
        Optional<User> user = userService.login(email, password);
        if (user.isPresent()) {
            session.setAttribute("user", user.get());
            // If force change, redirect to change-password immediately
            if (user.get().isForcePasswordChange()) {
                session.setAttribute("forceChange", true);
                return "redirect:/profile/change-password";
            }
            return "redirect:/dashboard";
        }
        List<User> admins = userService.getByRole(Role.COMMITTEE_ADMIN);
        String adminEmail = admins.isEmpty() ? "admin@residentops.com" : admins.get(0).getEmail();
        model.addAttribute("error", "Invalid email or password.");
        model.addAttribute("adminEmail", adminEmail);
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // ═══════════════════════════════════════════════════════════
    //  DASHBOARD (unchanged)
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";

        var allRequests = maintenanceService.getAllRequests();
        var notices     = noticeService.getAll();
        var polls       = votingService.getAll();

        model.addAttribute("user",           user);
        model.addAttribute("openRequests",   allRequests.stream()
                .filter(r -> !List.of(RequestStatus.CLOSED, RequestStatus.REJECTED).contains(r.getStatus())).count());
        model.addAttribute("escalatedCount", allRequests.stream()
                .filter(r -> r.getStatus() == RequestStatus.ESCALATED).count());
        model.addAttribute("openPolls",      polls.stream()
                .filter(p -> p.getStatus() == PollStatus.OPEN).count());
        model.addAttribute("noticeCount",    notices.size());
        model.addAttribute("recentRequests", allRequests.stream().limit(5).collect(Collectors.toList()));
        model.addAttribute("latestNotices",  notices.stream().limit(3).collect(Collectors.toList()));
        model.addAttribute("emergencies",    noticeService.getEmergencies());
        return "dashboard";
    }

    // ═══════════════════════════════════════════════════════════
    //  MAINTENANCE REQUESTS (unchanged + vendor leave check)
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/requests")
    public String requestsPage(HttpSession session, Model model) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        List<?> requests;
        if      (user.getRole() == Role.RESIDENT) requests = maintenanceService.getRequestsByResident(user.getId());
        else if (user.getRole() == Role.VENDOR)   requests = maintenanceService.getRequestsByVendor(user.getId());
        else                                       requests = maintenanceService.getAllRequests();
        // Build leave-status map for vendors (admin view)
        Map<Long, VendorConcern> vendorLeaveMap = new HashMap<>();
        List<User> vendors = userService.getByRole(Role.VENDOR);
        for (User v : vendors) {
            VendorConcern lv = vendorConcernService.getActiveLeave(v.getId());
            if (lv != null) vendorLeaveMap.put(v.getId(), lv);
        }
        model.addAttribute("user",           user);
        model.addAttribute("requests",       requests);
        model.addAttribute("vendors",        vendors);
        model.addAttribute("vendorLeaveMap", vendorLeaveMap);
        return "requests";
    }

    @PostMapping("/requests/raise")
    public String raiseRequest(@RequestParam String title, @RequestParam String description,
                               @RequestParam String category, @RequestParam String priority,
                               HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        try {
            maintenanceService.raiseRequest(user.getId(), title, description,
                    RequestCategory.valueOf(category), Priority.valueOf(priority));
            ra.addFlashAttribute("success", "Request submitted successfully!");
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/requests";
    }

    @PostMapping("/requests/{id}/assign")
    public String assignVendor(@PathVariable Long id, @RequestParam Long vendorId,
                               HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        // Check vendor leave before assigning
        if (vendorConcernService.isVendorOnLeave(vendorId)) {
            VendorConcern leave = vendorConcernService.getActiveLeave(vendorId);
            String until = leave != null && leave.getLeaveTo() != null ? leave.getLeaveTo().toString() : "unknown date";
            ra.addFlashAttribute("error", "Cannot assign: this vendor is on approved leave until " + until + ".");
            return "redirect:/requests";
        }
        try {
            maintenanceService.assignVendor(id, vendorId, user.getId());
            ra.addFlashAttribute("success", "Vendor assigned.");
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/requests";
    }

    @PostMapping("/requests/{id}/start")
    public String startWork(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        maintenanceService.updateStatus(id, RequestStatus.IN_PROGRESS, user.getId(), null);
        ra.addFlashAttribute("success", "Work started.");
        return "redirect:/requests";
    }

    @PostMapping("/requests/{id}/resolve")
    public String resolveRequest(@PathVariable Long id,
                                 @RequestParam(required = false) String workSummary,
                                 HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        maintenanceService.updateStatus(id, RequestStatus.RESOLVED, user.getId(), workSummary);
        ra.addFlashAttribute("success", "Marked as resolved.");
        return "redirect:/requests";
    }

    @PostMapping("/requests/{id}/close")
    public String closeRequest(@PathVariable Long id,
                               @RequestParam(defaultValue = "0") double cost,
                               HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        maintenanceService.closeRequest(id, user.getId(), "Verified and closed by admin", cost);
        ra.addFlashAttribute("success", "Request closed.");
        return "redirect:/requests";
    }

    // ═══════════════════════════════════════════════════════════
    //  COMPLAINTS (unchanged)
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/complaints")
    public String complaintsPage(HttpSession session, Model model) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        var complaints = (user.getRole() == Role.RESIDENT)
                ? complaintService.getByResident(user.getId())
                : complaintService.getAll();
        model.addAttribute("user", user);
        model.addAttribute("complaints", complaints);
        return "complaints";
    }

    @PostMapping("/complaints/file")
    public String fileComplaint(@RequestParam String title, @RequestParam String description,
                                @RequestParam String type,
                                HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        try {
            complaintService.fileComplaint(user.getId(), title, description,
                    ComplaintType.valueOf(type), null);
            ra.addFlashAttribute("success", "Complaint filed successfully.");
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/complaints";
    }

    @PostMapping("/complaints/{id}/update")
    public String updateComplaint(@PathVariable Long id, @RequestParam String status,
                                  HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        complaintService.updateStatus(id, ComplaintStatus.valueOf(status), user.getId(), "Reviewed");
        ra.addFlashAttribute("success", "Complaint updated.");
        return "redirect:/complaints";
    }

    // ═══════════════════════════════════════════════════════════
    //  POLLS (unchanged)
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/polls")
    public String pollsPage(HttpSession session, Model model) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        var polls = votingService.getAll();
        Map<Long, Map<String, Long>> pollResults = new HashMap<>();
        for (Poll p : polls) {
            try { pollResults.put(p.getId(), votingService.getPollResults(p.getId())); }
            catch (Exception ignored) {}
        }
        model.addAttribute("user", user);
        model.addAttribute("polls", polls);
        model.addAttribute("pollResults", pollResults);
        return "polls";
    }

    @PostMapping("/polls/create")
    public String createPoll(@RequestParam String question, @RequestParam String description,
                             @RequestParam int quorum, @RequestParam String deadline,
                             HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        try {
            votingService.createPoll(user.getId(), question, description, quorum, LocalDateTime.parse(deadline));
            ra.addFlashAttribute("success", "Poll created.");
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/polls";
    }

    @PostMapping("/polls/{id}/vote")
    public String castVote(@PathVariable Long id, @RequestParam String choice,
                           HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        try {
            votingService.castVote(id, user.getId(), VoteChoice.valueOf(choice));
            ra.addFlashAttribute("success", "Vote recorded.");
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/polls";
    }

    @PostMapping("/polls/{id}/close")
    public String closePoll(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        try {
            Poll p = votingService.closePoll(id, user.getId());
            ra.addFlashAttribute("success", "Poll closed. " + p.getResult());
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/polls";
    }

    // ═══════════════════════════════════════════════════════════
    //  EXPENSES (unchanged)
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/expenses")
    public String expensesPage(HttpSession session, Model model) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user",          user);
        model.addAttribute("expenses",      expenseService.getAll());
        model.addAttribute("totalExpenses", expenseService.getTotalExpenses());
        return "expenses";
    }

    @PostMapping("/expenses/add")
    public String addExpense(@RequestParam String title, @RequestParam double amount,
                             @RequestParam String category, @RequestParam String expenseDate,
                             @RequestParam(required = false) String notes,
                             HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        try {
            expenseService.addExpense(user.getId(), title, amount,
                    ExpenseCategory.valueOf(category), null, null, notes, LocalDate.parse(expenseDate));
            ra.addFlashAttribute("success", "Expense recorded.");
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/expenses";
    }

    // ═══════════════════════════════════════════════════════════
    //  NOTICES (unchanged)
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/notices")
    public String noticesPage(HttpSession session, Model model) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user",    user);
        model.addAttribute("notices", noticeService.getAll());
        return "notices";
    }

    @PostMapping("/notices/post")
    public String postNotice(@RequestParam String title, @RequestParam String body,
                             @RequestParam String type,
                             @RequestParam(required = false) String emergency,
                             HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        try {
            boolean isEmergency = "on".equals(emergency) || "true".equals(emergency);
            noticeService.postNotice(user.getId(), title, body, NoticeType.valueOf(type), isEmergency);
            ra.addFlashAttribute("success", "Notice posted.");
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/notices";
    }

    // ═══════════════════════════════════════════════════════════
    //  AUDIT (unchanged)
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/audit")
    public String auditPage(HttpSession session, Model model) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        model.addAttribute("logs", auditLogger.getAll());
        return "audit";
    }

    // ═══════════════════════════════════════════════════════════
    //  NEW: PROFILE (Resident + Vendor)
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/profile")
    public String profilePage(HttpSession session, Model model) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        if (user.getRole() == Role.RESIDENT) {
            model.addAttribute("members",       userService.getMembersForResident(user.getId()));
            model.addAttribute("memberCount",   userService.getMemberCount(user.getId()));
            model.addAttribute("myUpdateReqs",  userService.getAllFlatUpdateRequests().stream()
                    .filter(r -> r.getResident().getId().equals(user.getId()))
                    .collect(Collectors.toList()));
        }
        // pending reset request for current user
        boolean hasPendingReset = !userService.getPendingResetRequests().stream()
                .filter(r -> r.getUser().getId().equals(user.getId()))
                .collect(Collectors.toList()).isEmpty();
        model.addAttribute("hasPendingReset", hasPendingReset);
        return "profile";
    }

    @PostMapping("/profile/update-phone")
    public String updatePhone(@RequestParam String newPhone,
                              HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        userService.updatePhone(user.getId(), newPhone);
        // Refresh session user
        userService.getById(user.getId()).ifPresent(u -> session.setAttribute("user", u));
        ra.addFlashAttribute("success", "Phone number updated.");
        return "redirect:/profile";
    }

    @PostMapping("/profile/request-reset")
    public String requestPasswordReset(HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        userService.requestPasswordReset(user.getId());
        ra.addFlashAttribute("success", "Password reset request sent to admin. You will be notified once processed.");
        return "redirect:/profile/change-password";
    }

    @GetMapping("/profile/change-password")
    public String changePasswordPage(HttpSession session, Model model) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        model.addAttribute("forceChange", session.getAttribute("forceChange") != null);
        return "change-password";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 HttpSession session, RedirectAttributes ra, Model model) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        try {
            userService.changePassword(user.getId(), currentPassword, newPassword);
            // Refresh session
            userService.getById(user.getId()).ifPresent(u -> session.setAttribute("user", u));
            session.removeAttribute("forceChange");
            ra.addFlashAttribute("success", "Password changed successfully!");
            return "redirect:/dashboard";
        } catch (RuntimeException e) {
            model.addAttribute("user", user);
            model.addAttribute("forceChange", session.getAttribute("forceChange") != null);
            if ("WRONG_CURRENT_PASSWORD".equals(e.getMessage())) {
                model.addAttribute("error", "Current password is incorrect.");
                model.addAttribute("showForgot", true);
            } else {
                model.addAttribute("error", e.getMessage());
            }
            return "change-password";
        }
    }

    // ── Resident: Add flat member (submit update request) ─────
    @PostMapping("/profile/add-member")
    public String addFlatMember(@RequestParam String memberName,
                                @RequestParam(required = false) String memberPhone,
                                @RequestParam(required = false) MultipartFile proof,
                                HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null) return "redirect:/login";
        try {
            String filename = "no-proof";
            if (proof != null && !proof.isEmpty()) {
                filename = user.getId() + "_" + System.currentTimeMillis() + "_" + proof.getOriginalFilename();
                Path path = Paths.get(UPLOAD_DIR + filename);
                Files.write(path, proof.getBytes());
            }
            userService.submitFlatUpdateRequest(user.getId(), memberName, memberPhone, filename);
            ra.addFlashAttribute("success", "Request submitted. Admin will verify and update.");
        } catch (IOException e) {
            ra.addFlashAttribute("error", "File upload failed: " + e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    // ═══════════════════════════════════════════════════════════
    //  NEW: ADMIN PEOPLE MANAGEMENT PAGE
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/admin/people")
    public String adminPeoplePage(HttpSession session, Model model,
                                   @RequestParam(defaultValue = "RESIDENT") String viewRole) {
        User user = requireLogin(session);
        if (user == null || user.getRole() != Role.COMMITTEE_ADMIN) return "redirect:/dashboard";

        Role role = Role.valueOf(viewRole);
        List<User> people = userService.getByRole(role);

        // For residents, also load their flat members
        Map<Long, List<FlatMember>> membersMap = new HashMap<>();
        if (role == Role.RESIDENT) {
            for (User r : people) {
                membersMap.put(r.getId(), userService.getMembersForResident(r.getId()));
            }
        }

        model.addAttribute("user",           user);
        model.addAttribute("people",         people);
        model.addAttribute("viewRole",       viewRole);
        model.addAttribute("membersMap",     membersMap);
        model.addAttribute("flatUpdateReqs", userService.getPendingFlatUpdateRequests());
        model.addAttribute("passwordReqs",   userService.getPendingResetRequests());
        model.addAttribute("concerns",       vendorConcernService.getPending());
        return "admin-people";
    }

    @PostMapping("/admin/people/add-resident")
    public String adminAddResident(@RequestParam String name,
                                   @RequestParam String blockNo,
                                   @RequestParam String floorNo,
                                   @RequestParam String flatNo,
                                   @RequestParam(required = false) String phoneNo,
                                   @RequestParam(defaultValue = "OWNER") String ownerType,
                                   @RequestParam(defaultValue = "RO") String apartmentPrefix,
                                   HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null || user.getRole() != Role.COMMITTEE_ADMIN) return "redirect:/dashboard";
        try {
            User created = userService.registerResident(name, blockNo, floorNo, flatNo,
                    phoneNo, ownerType, apartmentPrefix);
            ra.addFlashAttribute("success",
                    "Resident registered! Email: " + created.getEmail() + " | Password: RO@123");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/people?viewRole=RESIDENT";
    }

    @PostMapping("/admin/people/add-vendor")
    public String adminAddVendor(@RequestParam String name,
                                 @RequestParam(required = false) String phoneNo,
                                 @RequestParam String serviceType,
                                 HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null || user.getRole() != Role.COMMITTEE_ADMIN) return "redirect:/dashboard";
        try {
            User created = userService.registerVendor(name, phoneNo, serviceType);
            ra.addFlashAttribute("success",
                    "Vendor registered! Email: " + created.getEmail() + " | Password: RO@123");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/people?viewRole=VENDOR";
    }

    // ── Admin: Approve flat update ─────────────────────────────
    @PostMapping("/admin/flat-update/{id}/approve")
    public String approveFlatUpdate(@PathVariable Long id,
                                    HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null || user.getRole() != Role.COMMITTEE_ADMIN) return "redirect:/dashboard";
        userService.approveFlatUpdate(id, user.getId());
        ra.addFlashAttribute("success", "Flat member addition approved and updated.");
        return "redirect:/admin/people?viewRole=RESIDENT";
    }

    @PostMapping("/admin/flat-update/{id}/reject")
    public String rejectFlatUpdate(@PathVariable Long id,
                                   @RequestParam(defaultValue = "Documents insufficient") String remarks,
                                   HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null || user.getRole() != Role.COMMITTEE_ADMIN) return "redirect:/dashboard";
        userService.rejectFlatUpdate(id, user.getId(), remarks);
        ra.addFlashAttribute("success", "Flat update request rejected.");
        return "redirect:/admin/people?viewRole=RESIDENT";
    }

    // ── Admin: Process password reset ──────────────────────────
    @PostMapping("/admin/password-reset/{id}/approve")
    public String adminApprovePasswordReset(@PathVariable Long id,
                                             HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null || user.getRole() != Role.COMMITTEE_ADMIN) return "redirect:/dashboard";
        userService.adminResetPassword(id, user.getId());
        ra.addFlashAttribute("success", "Password reset to RO@123. Notification sent to user.");
        return "redirect:/admin/people";
    }

    // ═══════════════════════════════════════════════════════════
    //  NEW: VENDOR CONCERNS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/vendor/concerns")
    public String vendorConcernsPage(HttpSession session, Model model) {
        User user = requireLogin(session);
        if (user == null || user.getRole() != Role.VENDOR) return "redirect:/dashboard";
        // Only requests assigned to this vendor
        List<MaintenanceRequest> myRequests = maintenanceService.getRequestsByVendor(user.getId());
        model.addAttribute("user",       user);
        model.addAttribute("concerns",   vendorConcernService.getByVendor(user.getId()));
        model.addAttribute("myRequests", myRequests);
        return "vendor-concerns";
    }

    @PostMapping("/vendor/concerns/raise")
    public String raiseConcern(@RequestParam String concernType,
                               @RequestParam(required = false) Long relatedRequestId,
                               @RequestParam(required = false) String description,
                               @RequestParam(required = false) String leaveFrom,
                               @RequestParam(required = false) String leaveTo,
                               @RequestParam(required = false) String leaveReason,
                               HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null || user.getRole() != Role.VENDOR) return "redirect:/dashboard";
        try {
            LocalDate from = (leaveFrom != null && !leaveFrom.isBlank()) ? LocalDate.parse(leaveFrom) : null;
            LocalDate to   = (leaveTo   != null && !leaveTo.isBlank())   ? LocalDate.parse(leaveTo)   : null;
            vendorConcernService.raiseConcern(user.getId(), concernType,
                    relatedRequestId, description, from, to, leaveReason);
            ra.addFlashAttribute("success", "Concern raised. Admin will review shortly.");
        } catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/vendor/concerns";
    }

    // ── Admin: Process vendor concerns ────────────────────────
    @GetMapping("/admin/concerns")
    public String adminConcernsPage(HttpSession session, Model model) {
        User user = requireLogin(session);
        if (user == null || user.getRole() != Role.COMMITTEE_ADMIN) return "redirect:/dashboard";
        model.addAttribute("user",     user);
        model.addAttribute("concerns", vendorConcernService.getAll());
        return "admin-concerns";
    }

    @PostMapping("/admin/concerns/{id}/approve")
    public String adminApproveConcern(@PathVariable Long id,
                                      @RequestParam(defaultValue = "Approved") String message,
                                      HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null || user.getRole() != Role.COMMITTEE_ADMIN) return "redirect:/dashboard";
        vendorConcernService.approveConcern(id, user.getId(), message);
        ra.addFlashAttribute("success", "Concern approved.");
        return "redirect:/admin/concerns";
    }

    @PostMapping("/admin/concerns/{id}/reject")
    public String adminRejectConcern(@PathVariable Long id,
                                     @RequestParam(defaultValue = "Not approved") String message,
                                     HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null || user.getRole() != Role.COMMITTEE_ADMIN) return "redirect:/dashboard";
        vendorConcernService.rejectConcern(id, user.getId(), message);
        ra.addFlashAttribute("success", "Concern rejected.");
        return "redirect:/admin/concerns";
    }

    @PostMapping("/admin/concerns/{id}/drop")
    public String adminDropConcern(@PathVariable Long id,
                                   @RequestParam(defaultValue = "Dropped") String message,
                                   HttpSession session, RedirectAttributes ra) {
        User user = requireLogin(session);
        if (user == null || user.getRole() != Role.COMMITTEE_ADMIN) return "redirect:/dashboard";
        vendorConcernService.dropConcern(id, user.getId(), message);
        ra.addFlashAttribute("success", "Concern dropped.");
        return "redirect:/admin/concerns";
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPER
    // ═══════════════════════════════════════════════════════════
    private User requireLogin(HttpSession session) {
        return (User) session.getAttribute("user");
    }
}
