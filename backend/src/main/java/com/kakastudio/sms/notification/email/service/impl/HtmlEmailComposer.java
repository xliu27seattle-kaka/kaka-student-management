package com.kakastudio.sms.notification.email.service.impl;

import com.kakastudio.sms.notification.email.dto.EmailMessage;
import com.kakastudio.sms.notification.email.service.EmailComposer;
import com.kakastudio.sms.user.domain.User;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Plain HTML emails. Everything that comes from a user (names, addresses) is escaped before it is
 * put into the markup.
 */
@Component
public class HtmlEmailComposer implements EmailComposer {

  @Override
  public EmailMessage registrationReceived(User user, String studentNo) {
    return message(
        user,
        "We received your registration",
        paragraph("Hello " + esc(user.getFirstName()) + ",")
            + paragraph(
                "Thank you for registering. Your student number is <strong>"
                    + esc(studentNo)
                    + "</strong>.")
            + paragraph(
                "An administrator will review your registration. You will get another email as soon"
                    + " as a decision is made; until then you cannot sign in."));
  }

  @Override
  public EmailMessage accountApproved(User user) {
    return message(
        user,
        "Your account is approved",
        paragraph("Hello " + esc(user.getFirstName()) + ",")
            + paragraph(
                "Your registration has been approved. You can now sign in with your email address"
                    + " and the password you chose."));
  }

  @Override
  public EmailMessage accountRejected(User user) {
    return message(
        user,
        "Your registration was not approved",
        paragraph("Hello " + esc(user.getFirstName()) + ",")
            + paragraph(
                "We are sorry: your registration was not approved. If you think this is a mistake,"
                    + " you can register again with the same email address."));
  }

  @Override
  public EmailMessage accountCreated(User user, String temporaryPassword) {
    return message(
        user,
        "Your Kaka Student Management account",
        paragraph("Hello " + esc(user.getFirstName()) + ",")
            + paragraph("An account has been created for you.")
            + paragraph(
                "Email: <strong>"
                    + esc(user.getEmail())
                    + "</strong><br>Temporary password: <strong>"
                    + esc(temporaryPassword)
                    + "</strong>")
            + paragraph("Please sign in and change this password from your profile."));
  }

  @Override
  public EmailMessage graduationApproved(User user, String certificateNo) {
    return message(
        user,
        "Congratulations on your graduation",
        paragraph("Hello " + esc(user.getFirstName()) + ",")
            + paragraph(
                "Your graduation has been approved. Your certificate number is <strong>"
                    + esc(certificateNo)
                    + "</strong>.")
            + paragraph(
                "You can print your certificate and your official transcript from the Graduation"
                    + " page after signing in."));
  }

  @Override
  public EmailMessage graduationRejected(User user) {
    return message(
        user,
        "Your graduation application",
        paragraph("Hello " + esc(user.getFirstName()) + ",")
            + paragraph(
                "Your graduation application was not approved this time. Please contact the"
                    + " registrar's office; you can apply again from the Graduation page."));
  }

  @Override
  public EmailMessage sectionAssigned(
      User professor, String section, String semester, String schedule) {
    return message(
        professor,
        "You teach " + section + " in " + semester,
        paragraph("Hello " + esc(professor.getFirstName()) + ",")
            + paragraph(
                "You have been assigned <strong>"
                    + esc(section)
                    + "</strong> in "
                    + esc(semester)
                    + ": "
                    + esc(schedule)
                    + ".")
            + paragraph("The class list appears on your Teaching page once students enroll."));
  }

  @Override
  public EmailMessage notification(User recipient, String subject, String body, String senderName) {
    String text = esc(body).replace("\r\n", "\n").replace("\n", "<br>");
    return message(
        recipient,
        subject,
        paragraph("Hello " + esc(recipient.getFirstName()) + ",")
            + paragraph(text)
            + paragraph("<span style=\"color:#66766d\">From " + esc(senderName) + "</span>"));
  }

  private static EmailMessage message(User user, String subject, String bodyHtml) {
    return new EmailMessage(user.getEmail(), subject, layout(subject, bodyHtml));
  }

  /** The one frame every email shares: heading, body, "Kaka Studio" footer. */
  private static String layout(String heading, String bodyHtml) {
    return "<div style=\"font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:0 auto;"
        + "padding:24px;color:#14201a\">"
        + "<p style=\"margin:0 0 4px;font-size:12px;letter-spacing:.08em;text-transform:uppercase;"
        + "color:#2F6B4F\">Kaka Student Management</p>"
        + "<h1 style=\"margin:0 0 16px;font-size:22px\">"
        + esc(heading)
        + "</h1>"
        + bodyHtml
        + "<hr style=\"border:0;border-top:1px solid #dfe3dd;margin:24px 0 12px\">"
        + "<p style=\"margin:0;font-size:12px;color:#66766d\">Kaka Studio</p>"
        + "</div>";
  }

  private static String paragraph(String html) {
    return "<p style=\"margin:0 0 12px;font-size:15px;line-height:1.5\">" + html + "</p>";
  }

  private static String esc(String value) {
    return HtmlUtils.htmlEscape(value == null ? "" : value);
  }
}
