package com.final_year.v2.service;

import com.final_year.v2.constaint.Role;
import com.final_year.v2.model.MonthlyEarnings;
import com.final_year.v2.model.PayoutRequest;
import com.final_year.v2.model.User;
import com.final_year.v2.model.Video;
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
    
    private List<String> getAdminEmails() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.ADMIN)
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
        List<String> adminEmails = getAdminEmails();
        if (adminEmails.isEmpty()) return;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminEmails.toArray(new String[0]));
        message.setSubject("New Video Uploaded: " + video.getTitle());
        message.setText(String.format(
                "A new video has been uploaded by %s (%s).\n\n" +
                        "Title: %s\n" +
                        "Type: %s\n" +
                        "View in admin panel: http://localhost:3000/admin/videos\n\n" +
                        "Please review and approve/reject.",
                user.getUsername(), user.getEmail(), video.getTitle(), video.getType()
        ));
        mailSender.send(message);
    }

    public void sendVideoApprovedNotification(Video video, User user) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Your video has been approved!");
        message.setText(String.format(
                "Dear %s,\n\n" +
                        "Your video \"%s\" has been approved and is now live.\n" +
                        "Watch it here: http://localhost:3000/watch/%d\n\n" +
                        "Thank you for contributing!",
                user.getUsername(), video.getTitle(), video.getId()
        ));
        mailSender.send(message);
    }

    public void sendVideoRejectedNotification(Video video, User user, String reason) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Your video was not approved");
        message.setText(String.format(
                "Dear %s,\n\n" +
                        "Your video \"%s\" has been rejected.\n" +
                        "Reason: %s\n\n" +
                        "Please edit and resubmit if you wish.",
                user.getUsername(), video.getTitle(), reason
        ));
        mailSender.send(message);
    }

    // ---------------------- New monetization methods ----------------------

    /**
     * Send notification to all admins when a creator requests a payout.
     */
    public void sendPayoutRequestNotification(PayoutRequest payoutRequest, User creator) {
        List<String> adminEmails = getAdminEmails();
        if (adminEmails.isEmpty()) return;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminEmails.toArray(new String[0]));
        message.setSubject("💰 New Payout Request from " + creator.getUsername());
        message.setText(String.format(
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
        mailSender.send(message);
    }

    /**
     * Send monthly earnings report to a creator.
     */
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

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(creator.getEmail());
        message.setSubject("📊 Monthly Earnings Report - " + YearMonth.now().minusMonths(1).format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        message.setText(report.toString());
        mailSender.send(message);
    }

    /**
     * Send monthly platform revenue report to all admins.
     */
    public void sendMonthlyRevenueReport(YearMonth reportMonth, BigDecimal totalRevenue, BigDecimal platformFee, BigDecimal creatorPool, int activeSubscriptions, int activeCreators) {
        List<String> adminEmails = getAdminEmails();
        if (adminEmails.isEmpty()) return;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminEmails.toArray(new String[0]));
        message.setSubject("📈 Monthly Platform Revenue Report - " + reportMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        message.setText(String.format(
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
        mailSender.send(message);
    }
}