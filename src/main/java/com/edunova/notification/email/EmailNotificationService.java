package com.edunova.notification.email;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // ── Welcome email for newly onboarded school admin ────────

    @Async
    public void sendAdminWelcomeEmail(
            String toEmail,
            String adminName,
            String schoolName,
            String setPasswordToken
    ) {
        String subject = "Welcome to EduNova — Your admin account is ready";
        String setPasswordUrl = frontendUrl + "/set-password?token=" + setPasswordToken;
        String body = buildWelcomeHtml(adminName, schoolName, toEmail, setPasswordUrl);
        sendHtml(toEmail, adminName, subject, body);
        log.info("Welcome email sent to {}", toEmail);
    }

    // ── Password reset ────────────────────────────────────────

    @Async
    public void sendPasswordResetEmail(String toEmail, String name, String resetToken) {
        String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;
        String subject  = "EduNova — Reset your password";
        String body     = buildPasswordResetHtml(name, resetUrl);
        sendHtml(toEmail, name, subject, body);
        log.info("Password reset email sent to {}", toEmail);
    }

    // ── Subscription expiry warning ───────────────────────────

    @Async
    public void sendSubscriptionExpiryWarning(
            String toEmail,
            String adminName,
            String schoolName,
            String expiryDate,
            int    daysLeft
    ) {
        String subject = "EduNova — Your subscription expires in " + daysLeft + " days";
        String body    = buildExpiryWarningHtml(adminName, schoolName, expiryDate, daysLeft);
        sendHtml(toEmail, adminName, subject, body);
        log.info("Expiry warning sent to {} for school {}", toEmail, schoolName);
    }

    // ── Private helpers ───────────────────────────────────────

    private void sendHtml(String toEmail, String toName, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send email to {}: {}", toEmail, ex.getMessage());
        }
    }

    private String buildWelcomeHtml(
            String adminName,
            String schoolName,
            String email,
            String setPasswordUrl
    ) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family:sans-serif;max-width:560px;margin:auto;padding:32px;color:#1e1e1e">
                  <div style="background:#4C1D95;padding:20px 28px;border-radius:10px 10px 0 0">
                    <h1 style="color:#fff;font-size:22px;margin:0">Welcome to EduNova 🎉</h1>
                  </div>
                  <div style="background:#faf9fe;border:1px solid #e8e4f5;padding:28px;border-radius:0 0 10px 10px">
                    <p>Hello <strong>%s</strong>,</p>
                    <p>Your admin account for <strong>%s</strong> has been created on EduNova.</p>
                
                     <p style="margin:10px 0"><strong>Login email:</strong> %s</p>
                   
                    <p>Please set your password before logging in. This link will expire in <strong>2 hour</strong></p> 
                    <a href="%s" style="display:inline-block;background:#4C1D95;color:#fff;padding:12px 24px;border-radius:8px;text-decoration:none;font-weight:600">
                      Set password →
                    </a>
                    <p style="color:#888;font-size:12px;margin-top:32px">
                      If you did not request this account, please contact support@edunova.app
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(adminName, schoolName, email, setPasswordUrl);
    }

    private String buildPasswordResetHtml(String name, String resetUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family:sans-serif;max-width:560px;margin:auto;padding:32px;color:#1e1e1e">
                  <div style="background:#4C1D95;padding:20px 28px;border-radius:10px 10px 0 0">
                    <h1 style="color:#fff;font-size:22px;margin:0">Reset your password</h1>
                  </div>
                  <div style="background:#faf9fe;border:1px solid #e8e4f5;padding:28px;border-radius:0 0 10px 10px">
                    <p>Hello <strong>%s</strong>,</p>
                    <p>We received a request to reset your EduNova password. Click below to choose a new password.</p>
                    <a href="%s" style="display:inline-block;background:#4C1D95;color:#fff;padding:12px 24px;border-radius:8px;text-decoration:none;font-weight:600">
                      Reset password →
                    </a>
                    <p>This link expires in <strong>2 hour</strong>.</p>
                    <p style="color:#888;font-size:12px;margin-top:28px">
                      If you didn't request a password reset, you can ignore this email.
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(name, resetUrl);
    }

    private String buildExpiryWarningHtml(
            String adminName,
            String schoolName,
            String expiryDate,
            int daysLeft
    ) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family:sans-serif;max-width:560px;margin:auto;padding:32px;color:#1e1e1e">
                  <div style="background:#B45309;padding:20px 28px;border-radius:10px 10px 0 0">
                    <h1 style="color:#fff;font-size:22px;margin:0">⚠ Subscription expiring soon</h1>
                  </div>
                  <div style="background:#fffbf0;border:1px solid #fde68a;padding:28px;border-radius:0 0 10px 10px">
                    <p>Hello <strong>%s</strong>,</p>
                    <p>The EduNova subscription for <strong>%s</strong> will expire on <strong>%s</strong>
                       (%d day%s remaining).</p>
                    <p>Please contact EduNova support to renew your subscription and ensure uninterrupted access.</p>
                    <a href="%s/admin/settings" style="display:inline-block;background:#B45309;color:#fff;padding:12px 24px;border-radius:8px;text-decoration:none;font-weight:600">
                      View subscription details →
                    </a>
                  </div>
                </body>
                </html>
                """.formatted(adminName, schoolName, expiryDate, daysLeft,
                daysLeft == 1 ? "" : "s", frontendUrl);
    }
}
