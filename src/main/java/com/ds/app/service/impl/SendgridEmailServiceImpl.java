package com.ds.app.service.impl;

import com.ds.app.entity.Employee;
import com.ds.app.entity.Leave;
import com.ds.app.entity.RegularizationRequest;
import com.ds.app.entity.Timesheet;
import com.ds.app.enums.ApprovalStatus;
import com.ds.app.service.IEmailService;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class SendgridEmailServiceImpl implements IEmailService {

    private final SendGrid sendGrid;

    @Value("${app.sendgrid.from-email}")
    private String fromEmail;

    @Value("${app.sendgrid.from-name}")
    private String fromName;

    // SendGrid is constructed with just the API key — no SMTP config needed
    public SendgridEmailServiceImpl(@Value("${app.sendgrid.api-key}") String apiKey) {
        this.sendGrid = new SendGrid(apiKey);
    }

    // ── Core send method ──────────────────────────────────────────────────────

    @Override
    public void sendPlainText(String to, String subject, String body) {
        Email from    = new Email(fromEmail, fromName);
        Email toEmail = new Email(to);
        Content content = new Content("text/plain", body);
        Mail mail = new Mail(from, subject, toEmail, content);

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            // 202 Accepted = SendGrid queued it for delivery (normal success)
            // 4xx = our fault (bad API key, invalid email address, etc.)
            // 5xx = SendGrid's fault (their servers down)
            if (response.getStatusCode() == 202) {
                log.debug("Email accepted by SendGrid → to={} subject={}", to, subject);
            } else {
                log.error("SendGrid rejected email → status={} to={} subject={} body={}",
                        response.getStatusCode(), to, subject, response.getBody());
            }

        } catch (IOException e) {
            // Network error reaching SendGrid's API
            log.error("Failed to reach SendGrid API → to={} subject={} error={}",
                    to, subject, e.getMessage());
        }
    }

    // ── Leave notifications ───────────────────────────────────────────────────

    @Override
    @Async("emailTaskExecutor")
    public void notifyManagerForNewLeave(Employee employee, Leave leave) {
        Employee manager = employee.getManager();
        if (isBlank(manager)) return;

        String subject = "New Leave Request - " + fullName(employee);
        String body = "Hello " + manager.getFirstName() + ",\n\n"
                + fullName(employee) + " has applied for leave.\n\n"
                + "Type: "   + leave.getLeaveType()  + "\n"
                + "Dates: "  + leave.getStartDate()  + " to " + leave.getEndDate() + "\n"
                + "Days: "   + leave.getTotalDays()   + "\n"
                + "Reason: " + leave.getReasonForLeave() + "\n"
                + "Status: " + leave.getStatus()     + "\n\n"
                + "Please review it in the portal.";

        sendPlainText(manager.getEmail(), subject, body);
    }

    @Override
    @Async("emailTaskExecutor")
    public void notifyEmployeeForLeaveDecision(Employee employee, Leave leave) {
        if (isBlank(employee.getEmail())) return;

        String subject = "Leave Request " + leave.getStatus();
        String body = "Hello " + employee.getFirstName() + ",\n\n"
                + "Your leave request has been " + leave.getStatus() + ".\n\n"
                + "Type: "  + leave.getLeaveType() + "\n"
                + "Dates: " + leave.getStartDate() + " to " + leave.getEndDate() + "\n"
                + "Days: "  + leave.getTotalDays()  + "\n"
                + (leave.getStatus().name().equals("REJECTED")
                ? "Reason: " + nullSafe(leave.getRejectionReason()) + "\n"
                : "")
                + "\nRegards,\nHR Team";

        sendPlainText(employee.getEmail(), subject, body);
    }

    @Override
    @Async("emailTaskExecutor")
    public void notifyManagerForCancellationRequest(Employee employee, Leave leave) {
        Employee manager = employee.getManager();
        if (isBlank(manager)) return;

        String subject = "Leave Cancellation Request - " + fullName(employee);
        String body = "Hello " + manager.getFirstName() + ",\n\n"
                + fullName(employee) + " has requested cancellation of an approved leave.\n\n"
                + "Type: "           + leave.getLeaveType() + "\n"
                + "Dates: "          + leave.getStartDate() + " to " + leave.getEndDate() + "\n"
                + "Days: "           + leave.getTotalDays()  + "\n"
                + "Current Status: " + leave.getStatus()    + "\n\n"
                + "Please review the cancellation request in the portal.";

        sendPlainText(manager.getEmail(), subject, body);
    }

    @Override
    @Async("emailTaskExecutor")
    public void notifyEmployeeForCancellationDecision(Employee employee, Leave leave,
                                                      ApprovalStatus decision, String reason) {
        if (isBlank(employee.getEmail())) return;

        String subject = "Leave Cancellation Request " + leave.getStatus();
        String body = "Hello " + employee.getFirstName() + ",\n\n"
                + "Your leave cancellation request has been " + leave.getStatus() + ".\n\n"
                + "Type: "  + leave.getLeaveType() + "\n"
                + "Dates: " + leave.getStartDate() + " to " + leave.getEndDate() + "\n"
                + "Days: "  + leave.getTotalDays()  + "\n"
                + (decision == ApprovalStatus.REJECTED ? "Reason: " + nullSafe(reason) + "\n" : "")
                + "\nRegards,\nHR Team";

        sendPlainText(employee.getEmail(), subject, body);
    }

    // ── Regularization notifications ──────────────────────────────────────────

    @Override
    @Async("emailTaskExecutor")
    public void notifyManagerForNewRegularization(Employee employee,
                                                  RegularizationRequest req) {
        Employee manager = employee.getManager();
        if (isBlank(manager)) return;

        String subject = "New Regularization Request - " + fullName(employee);
        String body = "Hello " + manager.getFirstName() + ",\n\n"
                + fullName(employee) + " has applied for regularization.\n\n"
                + "Date: "      + req.getDate()          + "\n"
                + "Punch In: "  + req.getPunchInTime()    + "\n"
                + "Punch Out: " + req.getPunchOutTime()   + "\n"
                + "Reason: "    + req.getReason()         + "\n"
                + "Status: "    + req.getStatus()         + "\n\n"
                + "Please review it in the portal.";

        sendPlainText(manager.getEmail(), subject, body);
    }

    @Override
    @Async("emailTaskExecutor")
    public void notifyEmployeeForRegularizationDecision(Employee employee,
                                                        RegularizationRequest req) {
        if (isBlank(employee.getEmail())) return;

        String subject = "Regularization Request " + req.getStatus();
        String body = "Hello " + employee.getFirstName() + ",\n\n"
                + "Your regularization request has been " + req.getStatus() + ".\n\n"
                + "Date: "      + req.getDate()        + "\n"
                + "Punch In: "  + req.getPunchInTime()  + "\n"
                + "Punch Out: " + req.getPunchOutTime() + "\n"
                + "Reason: "    + req.getReason()       + "\n"
                + (req.getStatus().name().equals("REJECTED")
                ? "Reason for Rejection: " + nullSafe(req.getRejectionReason()) + "\n"
                : "")
                + "\nRegards,\nHR Team";

        sendPlainText(employee.getEmail(), subject, body);
    }

    // ── Timesheet notifications ───────────────────────────────────────────────

    @Override
    @Async("emailTaskExecutor")
    public void notifyManagerForTimesheetSubmission(Employee employee, Timesheet timesheet) {
        Employee manager = employee.getManager();
        if (isBlank(manager)) return;

        int mins = timesheet.getTotalMonthlyMinutes() != null ? timesheet.getTotalMonthlyMinutes() : 0;

        String subject = "Timesheet Submitted - " + fullName(employee);
        String body = "Hello " + manager.getFirstName() + ",\n\n"
                + fullName(employee) + " has submitted a timesheet.\n\n"
                + "Month/Year: "   + timesheet.getMonth() + "/" + timesheet.getYear() + "\n"
                + "Total Hours: "  + String.format("%.1f", mins / 60.0) + "\n"
                + "Status: "       + timesheet.getStatus() + "\n\n"
                + "Please review it in the portal.";

        sendPlainText(manager.getEmail(), subject, body);
    }

    @Override
    @Async("emailTaskExecutor")
    public void notifyEmployeeForTimesheetDecision(Employee employee, Timesheet timesheet) {
        if (isBlank(employee.getEmail())) return;

        int mins = timesheet.getTotalMonthlyMinutes() != null ? timesheet.getTotalMonthlyMinutes() : 0;

        String subject = "Timesheet " + timesheet.getStatus();
        String body = "Hello " + employee.getFirstName() + ",\n\n"
                + "Your timesheet has been " + timesheet.getStatus() + ".\n\n"
                + "Month/Year: "  + timesheet.getMonth() + "/" + timesheet.getYear() + "\n"
                + "Total Hours: " + String.format("%.1f", mins / 60.0) + "\n"
                + (timesheet.getStatus().name().equals("REJECTED")
                ? "Reason: " + nullSafe(timesheet.getRejectionReason()) + "\n"
                : "")
                + "\nRegards,\nHR Team";

        sendPlainText(employee.getEmail(), subject, body);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String fullName(Employee e) {
        return e.getFirstName() + " " + e.getLastName();
    }

    private boolean isBlank(String email) {
        return email == null || email.isBlank();
    }

    private boolean isBlank(Employee manager) {
        return manager == null || isBlank(manager.getEmail());
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}