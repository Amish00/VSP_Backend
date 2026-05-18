package com.final_year.v2.service;

import com.final_year.v2.constaint.Role;
import com.final_year.v2.model.*;
import com.final_year.v2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;   // <-- new

    private List<User> getAdminUsers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.ADMIN)
                .collect(Collectors.toList());
    }

    private List<String> getAdminEmails() {
        return getAdminUsers().stream()
                .map(User::getEmail)
                .collect(Collectors.toList());
    }

    public void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Password Reset OTP");
        message.setText("Your OTP for password reset is: " + otp + "\nIt will expire in 5 minutes.");
        mailSender.send(message);
    }

    public void sendAdminNewVideoNotification(Video video, User user) {
        List<User> admins = getAdminUsers();
        if (admins.isEmpty()) return;

        // Email
        SimpleMailMessage emailMsg = new SimpleMailMessage();
        emailMsg.setTo(getAdminEmails().toArray(new String[0]));
        emailMsg.setSubject("New Video Uploaded: " + video.getTitle());
        emailMsg.setText(String.format(
                "A new video has been uploaded by %s (%s).\n\n" +
                        "Title: %s\n" +
                        "Type: %s\n" +
                        "View in admin panel: http://localhost:3000/admin/videos\n\n" +
                        "Please review and approve/reject.",
                user.getUsername(), user.getEmail(), video.getTitle(), video.getType()
        ));
        mailSender.send(emailMsg);

        // In-app notifications for each admin
        for (User admin : admins) {
            notificationService.createNotification(
                    admin,
                    "New Video Pending Approval",
                    String.format("%s uploaded \"%s\" - needs review.", user.getUsername(), video.getTitle()),
                    "NEW_VIDEO_UPLOAD",
                    video.getId().toString()
            );
        }
    }

    public void sendVideoApprovedNotification(Video video, User user) {
        // Email
        SimpleMailMessage emailMsg = new SimpleMailMessage();
        emailMsg.setTo(user.getEmail());
        emailMsg.setSubject("Your video has been approved!");
        emailMsg.setText(String.format(
                "Dear %s,\n\n" +
                        "Your video \"%s\" has been approved and is now live.\n" +
                        "Watch it here: http://localhost:3000/watch/%d\n\n" +
                        "Thank you for contributing!",
                user.getUsername(), video.getTitle(), video.getId()
        ));
        mailSender.send(emailMsg);

        // In-app notification for the creator
        notificationService.createNotification(
                user,
                "Video Approved",
                String.format("Your video \"%s\" has been approved and is now live.", video.getTitle()),
                "VIDEO_APPROVED",
                video.getId().toString()
        );
    }

    public void sendVideoRejectedNotification(Video video, User user, String reason) {
        // Email
        SimpleMailMessage emailMsg = new SimpleMailMessage();
        emailMsg.setTo(user.getEmail());
        emailMsg.setSubject("Your video was not approved");
        emailMsg.setText(String.format(
                "Dear %s,\n\n" +
                        "Your video \"%s\" has been rejected.\n" +
                        "Reason: %s\n\n" +
                        "Please edit and resubmit if you wish.",
                user.getUsername(), video.getTitle(), reason
        ));
        mailSender.send(emailMsg);

        // In-app notification for the creator
        notificationService.createNotification(
                user,
                "Video Rejected",
                String.format("Your video \"%s\" was rejected. Reason: %s", video.getTitle(), reason),
                "VIDEO_REJECTED",
                video.getId().toString()
        );
    }

    public void sendPayoutRequestNotification(PayoutRequest payoutRequest, User creator) {
        List<User> admins = getAdminUsers();
        if (admins.isEmpty()) return;

        // Email
        SimpleMailMessage emailMsg = new SimpleMailMessage();
        emailMsg.setTo(getAdminEmails().toArray(new String[0]));
        emailMsg.setSubject("New Payout Request from " + creator.getUsername());
        emailMsg.setText(String.format(
                "Hello Admin,\n\n" +
                        "A new payout request has been submitted.\n\n" +
                        "Creator: %s (%s)\n" +
                        "Amount: $%.2f\n" +
                        "Method: %s\n" +
                        "Account: %s\n" +
                        "Requested at: %s\n\n" +
                        "Please log in to the admin panel to process or reject this request.\n" +
                        "http://localhost:3000/admin/revenue",
                creator.getUsername(), creator.getEmail(),
                payoutRequest.getAmount(),
                payoutRequest.getWithdrawalMethod(),
                payoutRequest.getAccountDetails(),
                payoutRequest.getRequestedAt()
        ));
        mailSender.send(emailMsg);

        // In-app notifications for each admin
        for (User admin : admins) {
            notificationService.createNotification(
                    admin,
                    "New Payout Request",
                    String.format("%s requested $%.2f (%s).", creator.getUsername(), payoutRequest.getAmount(), payoutRequest.getWithdrawalMethod()),
                    "PAYOUT_REQUEST",
                    payoutRequest.getId().toString()
            );
        }
    }

    public void sendMonthlyEarningsReport(User creator, List<MonthlyEarnings> earningsList, BigDecimal totalEarned, BigDecimal pendingBalance) {
        StringBuilder report = new StringBuilder();
        report.append(String.format("Dear %s,\n\n", creator.getUsername()));
        report.append("Here is your earnings report for the past months (most recent first):\n\n");

        for (MonthlyEarnings e : earningsList) {
            report.append(String.format("- %s: $%.2f\n", e.getMonthYear(), e.getEarningsAmount()));
        }

        report.append(String.format("\nTotal earned to date: $%.2f\n", totalEarned));
        report.append(String.format("Pending balance (available for withdrawal): $%.2f\n\n", pendingBalance));
        report.append("You can request a payout from your Earnings page.\n");
        report.append("http://localhost:3000/creator/earnings\n\n");
        report.append("Thank you for creating with us!");

        SimpleMailMessage emailMsg = new SimpleMailMessage();
        emailMsg.setTo(creator.getEmail());
        emailMsg.setSubject("Monthly Earnings Report - " + YearMonth.now().minusMonths(1).format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        emailMsg.setText(report.toString());
        mailSender.send(emailMsg);

        // In-app notification for the creator (short version)
        String month = YearMonth.now().minusMonths(1).format(DateTimeFormatter.ofPattern("MMMM yyyy"));
        notificationService.createNotification(
                creator,
                "Monthly Earnings Report",
                String.format("You earned $%.2f in %s. Pending balance: $%.2f", totalEarned, month, pendingBalance),
                "MONTHLY_EARNINGS",
                null
        );
    }

    public void sendMonthlyRevenueReport(YearMonth reportMonth, BigDecimal totalRevenue, BigDecimal platformFee, BigDecimal creatorPool, int activeSubscriptions, int activeCreators) {
        List<User> admins = getAdminUsers();
        if (admins.isEmpty()) return;

        // Email
        SimpleMailMessage emailMsg = new SimpleMailMessage();
        emailMsg.setTo(getAdminEmails().toArray(new String[0]));
        emailMsg.setSubject("Monthly Platform Revenue Report - " + reportMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        emailMsg.setText(String.format(
                "Hello Admin,\n\n" +
                        "Here is the revenue summary for %s:\n\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "Total Subscription Revenue: $%.2f\n" +
                        "Platform Fee (30%%):        $%.2f\n" +
                        "Creator Earnings Pool:      $%.2f\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "Active Subscriptions:       %d\n" +
                        "Active Creators:            %d\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                        "Detailed earnings breakdown is available in the admin revenue panel.\n" +
                        "http://localhost:3000/admin/revenue\n\n" +
                        "Keep growing the platform!",
                reportMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                totalRevenue, platformFee, creatorPool,
                activeSubscriptions, activeCreators
        ));
        mailSender.send(emailMsg);

        // In-app notifications for each admin
        for (User admin : admins) {
            notificationService.createNotification(
                    admin,
                    "Monthly Revenue Report",
                    String.format("%s: Total revenue $%.2f | Platform fee $%.2f",
                            reportMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            totalRevenue, platformFee),
                    "MONTHLY_REVENUE_REPORT",
                    null
            );
        }
    }
}