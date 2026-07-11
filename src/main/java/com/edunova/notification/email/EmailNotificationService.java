package com.edunova.notification.email;


import com.edunova.module.student.dto.StudentDto;
import com.edunova.module.superadmin.model.UserSchoolDTO;
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


    @Async
    public void sendStudentWelcomeEmail(
            String toEmail,
            UserSchoolDTO loggedInUserDetails,
            StudentDto.Response student
    ) {
        String schoolName= loggedInUserDetails.getSchoolName();
        String subject = "Welcome to "+schoolName+  "— Your admission is completed";
        String userName= loggedInUserDetails.getFirstName()+loggedInUserDetails.getLastName();
        String body = buildStudentWelcomeHtml(userName,schoolName, student);
        sendHtml(toEmail, userName, subject, body);
        log.info("Student Onboarding welcome email sent to {}", toEmail);
    }

    private String buildStudentWelcomeHtml(
            String adminName,
            String schoolName,
            StudentDto.Response student
    ) {
        String html =  """
                <!DOCTYPE html>
                       <html lang="en" xmlns="http://www.w3.org/1999/xhtml" xmlns:v="urn:schemas-microsoft-com:vml" xmlns:o="urn:schemas-microsoft-com:office:office">
                       <head>
                       <meta charset="UTF-8">
                       <meta name="viewport" content="width=device-width, initial-scale=1.0">
                       <meta http-equiv="X-UA-Compatible" content="IE=edge">
                       <title>Student Onboarding</title>
                       <!--[if mso]>
                       <noscript>
                       <xml>
                       <o:OfficeDocumentSettings>
                       <o:PixelsPerInch>96</o:PixelsPerInch>
                       </o:OfficeDocumentSettings>
                       </xml>
                       </noscript>
                       <![endif]-->
                       </head>
                       <body style="margin:0; padding:0; background-color:#eef1f5; font-family:Arial, Helvetica, sans-serif;">
                
                       <!-- Preheader (hidden preview text) -->
                       <div style="display:none; max-height:0; overflow:hidden; mso-hide:all;">
                         Welcome to school, {{fullName}}! Your admission is confirmed. Admission No: {{admissionNo}}
                       </div>
                
                       <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#eef1f5; padding:24px 0;">
                         <tr>
                           <td align="center">
                
                             <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:600px; max-width:600px; background-color:#ffffff; border-radius:8px; overflow:hidden; box-shadow:0 2px 6px rgba(0,0,0,0.08);">
                
                               <!-- Header -->
                               <tr>
                                 <td style="background-color:#1a4d8f; padding:28px 32px; text-align:center;">
                                   <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                                     <tr>
                                       <td align="center">
                                         <!-- Replace with your school logo URL -->
                                         <!--<img src="https://via.placeholder.com/150x50/1a4d8f/ffffff?text=SCHOOL+LOGO" width="150" height="50" alt="School Logo" style="display:block; border:0; margin-bottom:12px;"> -->
                                         <p style="margin:0; color:#ffffff; font-size:20px; font-weight:bold; font-family:Arial, Helvetica, sans-serif;">
                                           Student Onboarding Confirmation
                                         </p>
                                       </td>
                                     </tr>
                                   </table>
                                 </td>
                               </tr>
                
                               <!-- Welcome message -->
                               <tr>
                                 <td style="padding:32px 32px 8px 32px;">
                                   <p style="margin:0 0 16px 0; font-size:16px; line-height:24px; color:#333333; font-family:Arial, Helvetica, sans-serif;">
                                     Dear Parent/Guardian,
                                   </p>
                                   <p style="margin:0 0 16px 0; font-size:15px; line-height:24px; color:#555555; font-family:Arial, Helvetica, sans-serif;">
                                     We are delighted to welcome <strong>{{fullName}}</strong> to <strong>{{schoolName}}</strong>! This email confirms
                                     the successful onboarding and admission for the academic year <strong>{{currentEnrollment.academicYearLabel}}</strong>.
                                     Below are the enrollment and personal details on record.
                                   </p>
                                 </td>
                               </tr>
                
                               <!-- Student Details Card -->
                               <tr>
                                 <td style="padding:8px 32px 8px 32px;">
                                   <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f7f9fc; border:1px solid #e3e8f0; border-radius:6px;">
                                     <tr>
                                       <td style="padding:18px 20px 6px 20px;">
                                         <p style="margin:0 0 10px 0; font-size:14px; font-weight:bold; color:#1a4d8f; text-transform:uppercase; letter-spacing:0.5px; font-family:Arial, Helvetica, sans-serif;">
                                           Student Information
                                         </p>
                                       </td>
                                     </tr>
                                     <tr>
                                       <td style="padding:0 20px 18px 20px;">
                                         <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="font-family:Arial, Helvetica, sans-serif; font-size:14px; color:#333333;">
                                           <tr>
                                             <td width="45%" style="padding:6px 0; color:#777777;">Full Name</td>
                                             <td width="55%" style="padding:6px 0; font-weight:bold;">{{fullName}}</td>
                                           </tr>
                                           <tr>
                                             <td style="padding:6px 0; color:#777777; border-top:1px solid #e3e8f0;">Admission No.</td>
                                             <td style="padding:6px 0; font-weight:bold; border-top:1px solid #e3e8f0;">{{admissionNo}}</td>
                                           </tr>
                                           <tr>
                                             <td style="padding:6px 0; color:#777777; border-top:1px solid #e3e8f0;">Date of Birth</td>
                                             <td style="padding:6px 0; font-weight:bold; border-top:1px solid #e3e8f0;">{{dateOfBirth}}</td>
                                           </tr>
                                           <tr>
                                             <td style="padding:6px 0; color:#777777; border-top:1px solid #e3e8f0;">Gender</td>
                                             <td style="padding:6px 0; font-weight:bold; border-top:1px solid #e3e8f0;">{{gender}}</td>
                                           </tr>
                                           <tr>
                                             <td style="padding:6px 0; color:#777777; border-top:1px solid #e3e8f0;">Blood Group</td>
                                             <td style="padding:6px 0; font-weight:bold; border-top:1px solid #e3e8f0;">{{bloodGroup}}</td>
                                           </tr>
                                           <tr>
                                             <td style="padding:6px 0; color:#777777; border-top:1px solid #e3e8f0; vertical-align:top;">Address</td>
                                             <td style="padding:6px 0; font-weight:bold; border-top:1px solid #e3e8f0;">{{address}}</td>
                                           </tr>
                                         </table>
                                       </td>
                                     </tr>
                                   </table>
                                 </td>
                               </tr>
                
                               <!-- Enrollment Details Card -->
                               <tr>
                                 <td style="padding:16px 32px 8px 32px;">
                                   <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f7f9fc; border:1px solid #e3e8f0; border-radius:6px;">
                                     <tr>
                                       <td style="padding:18px 20px 6px 20px;">
                                         <p style="margin:0 0 10px 0; font-size:14px; font-weight:bold; color:#1a4d8f; text-transform:uppercase; letter-spacing:0.5px; font-family:Arial, Helvetica, sans-serif;">
                                           Current Enrollment
                                         </p>
                                       </td>
                                     </tr>
                                     <tr>
                                       <td style="padding:0 20px 18px 20px;">
                                         <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="font-family:Arial, Helvetica, sans-serif; font-size:14px; color:#333333;">
                                           <tr>
                                             <td width="45%" style="padding:6px 0; color:#777777;">Academic Year</td>
                                             <td width="55%" style="padding:6px 0; font-weight:bold;">{{currentEnrollment.academicYearLabel}}</td>
                                           </tr>
                                           <tr>
                                             <td style="padding:6px 0; color:#777777; border-top:1px solid #e3e8f0;">Grade</td>
                                             <td style="padding:6px 0; font-weight:bold; border-top:1px solid #e3e8f0;">{{currentEnrollment.gradeName}}</td>
                                           </tr>
                                           <tr>
                                             <td style="padding:6px 0; color:#777777; border-top:1px solid #e3e8f0;">Section</td>
                                             <td style="padding:6px 0; font-weight:bold; border-top:1px solid #e3e8f0;">{{currentEnrollment.sectionName}}</td>
                                           </tr>
                                           <tr>
                                             <td style="padding:6px 0; color:#777777; border-top:1px solid #e3e8f0;">Roll Number</td>
                                             <td style="padding:6px 0; font-weight:bold; border-top:1px solid #e3e8f0;">{{currentEnrollment.rollNumber}}</td>
                                           </tr>
                                         </table>
                                       </td>
                                     </tr>
                                   </table>
                                 </td>
                               </tr>
                
                               <!-- CTA button -->
                               <tr>
                                 <td style="padding:28px 32px 8px 32px;" align="center">
                                   <table role="presentation" cellpadding="0" cellspacing="0" border="0">
                                     <tr>
                                       <td align="center" style="border-radius:5px; background-color:#1a4d8f;">
                                         <a href="{{portalLoginUrl}}" target="_blank" style="display:inline-block; padding:12px 28px; font-size:15px; font-weight:bold; color:#ffffff; text-decoration:none; font-family:Arial, Helvetica, sans-serif; border-radius:5px;">
                                           Access Student Portal
                                         </a>
                                       </td>
                                     </tr>
                                   </table>
                                 </td>
                               </tr>
                
                               <!-- Note -->
                               <tr>
                                 <td style="padding:20px 32px 32px 32px;">
                                   <p style="margin:0; font-size:13px; line-height:20px; color:#888888; font-family:Arial, Helvetica, sans-serif;">
                                     If any of the details above are incorrect, please contact the school administration office within
                                     7 days of receiving this email. We look forward to a wonderful academic year ahead.
                                   </p>
                                 </td>
                               </tr>
                
                               <!-- Footer -->
                               <tr>
                                 <td style="background-color:#f0f2f5; padding:20px 32px; text-align:center; border-top:1px solid #e3e8f0;">
                                   <p style="margin:0 0 4px 0; font-size:12px; color:#999999; font-family:Arial, Helvetica, sans-serif;">
                                     {{schoolName}} &bull; {{schoolAddress}}
                                   </p>
                                   <p style="margin:0; font-size:12px; color:#999999; font-family:Arial, Helvetica, sans-serif;">
                                     This is an automated email, please do not reply directly to this message.
                                   </p>
                                 </td>
                               </tr>
                
                             </table>
                
                           </td>
                         </tr>
                       </table>
                
                       </body>
                       </html>
                """;

        html = html.replace("{{fullName}}", student.getFullName())
                .replace("{{admissionNo}}", student.getAdmissionNo())
                .replace("{{dateOfBirth}}", student.getDateOfBirth().toString())
                .replace("{{gender}}", student.getGender())
                .replace("{{bloodGroup}}", student.getBloodGroup())
                .replace("{{address}}", student.getAddress())
                .replace("{{currentEnrollment.academicYearLabel}}", student.getCurrentEnrollment().getAcademicYearLabel())
                .replace("{{currentEnrollment.gradeName}}", student.getCurrentEnrollment().getGradeName())
                .replace("{{currentEnrollment.sectionName}}", student.getCurrentEnrollment().getSectionName())
                .replace("{{currentEnrollment.rollNumber}}", student.getCurrentEnrollment().getRollNumber())
                .replace("{{schoolName}}", schoolName);

        return html;
    }
}
