//package com.ds.app.service.impl;
//
//import com.ds.app.entity.Employee;
//import com.ds.app.entity.Leave;
//import com.ds.app.entity.RegularizationRequest;
//import com.ds.app.entity.Timesheet;
//import com.ds.app.enums.ApprovalStatus;
//import com.ds.app.service.IEmailService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class EmailServiceImpl implements IEmailService {
//
//    private final JavaMailSender mailSender;
//
//    @Value("${app.mail.from}")
//    private String from;
//
//    @Override
//    public void sendPlainText(String to, String subject, String body) {
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//
//        }
//        SimpleMailMessage msg = new SimpleMailMessage();
//        msg.setFrom(from);
//        msg.setTo(to);
//        msg.setSubject(subject);
//        msg.setText(body);
//        mailSender.send(msg);
//    }
//
//    // Leave notifications
//
//    @Override
//    @Async("emailTaskExecutor")
//    public void notifyManagerForNewLeave(Employee employee, Leave leave) {
//        Employee manager = employee.getManager();
//        if (manager == null || manager.getEmail() == null || manager.getEmail().isBlank()) return;
//
//        String subject = "New Leave Request - " + employee.getFirstName() + " " + employee.getLastName();
//        String body = "Hello " + manager.getFirstName() + ",\n\n"
//                + employee.getFirstName() + " " + employee.getLastName() + " has applied for leave.\n\n"
//                + "Type: " + leave.getLeaveType() + "\n"
//                + "Dates: " + leave.getStartDate() + " to " + leave.getEndDate() + "\n"
//                + "Days: " + leave.getTotalDays() + "\n"
//                + "Reason: " + leave.getReasonForLeave() + "\n"
//                + "Status: " + leave.getStatus() + "\n\n"
//                + "Please review it in the portal.";
//        sendPlainText(manager.getEmail(), subject, body);
//    }
//
//    @Override
//    @Async("emailTaskExecutor")
//    public void notifyEmployeeForLeaveDecision(Employee employee, Leave leave) {
//        if (employee.getEmail() == null || employee.getEmail().isBlank()) return;
//
//        String subject = "Leave Request " + leave.getStatus();
//        String body = "Hello " + employee.getFirstName() + ",\n\n"
//                + "Your leave request has been " + leave.getStatus() + ".\n\n"
//                + "Type: " + leave.getLeaveType() + "\n"
//                + "Dates: " + leave.getStartDate() + " to " + leave.getEndDate() + "\n"
//                + "Days: " + leave.getTotalDays() + "\n"
//                + (leave.getStatus().name().equals("REJECTED")
//                ? "Reason: " + (leave.getRejectionReason() == null ? "" : leave.getRejectionReason()) + "\n"
//                : "")
//                + "\nRegards,\nHR Team";
//        sendPlainText(employee.getEmail(), subject, body);
//    }
//
//    @Override
//    @Async("emailTaskExecutor")
//    public void notifyManagerForCancellationRequest(Employee employee, Leave leave) {
//        Employee manager = employee.getManager();
//        if (manager == null || manager.getEmail() == null || manager.getEmail().isBlank()) return;
//
//        String subject = "Leave Cancellation Request - " + employee.getFirstName() + " " + employee.getLastName();
//        String body = "Hello " + manager.getFirstName() + ",\n\n"
//                + employee.getFirstName() + " " + employee.getLastName()
//                + " has requested cancellation of an approved leave.\n\n"
//                + "Type: " + leave.getLeaveType() + "\n"
//                + "Dates: " + leave.getStartDate() + " to " + leave.getEndDate() + "\n"
//                + "Days: " + leave.getTotalDays() + "\n"
//                + "Current Status: " + leave.getStatus() + "\n\n"
//                + "Please review the cancellation request in the portal.";
//        sendPlainText(manager.getEmail(), subject, body);
//    }
//
//    @Override
//    @Async("emailTaskExecutor")
//    public void notifyEmployeeForCancellationDecision(Employee employee, Leave leave, ApprovalStatus decision, String reason) {
//        if (employee.getEmail() == null || employee.getEmail().isBlank()) return;
//
//        String subject = "Leave Cancellation Request " + leave.getStatus();
//        String body = "Hello " + employee.getFirstName() + ",\n\n"
//                + "Your leave cancellation request has been " + leave.getStatus() + ".\n\n"
//                + "Type: " + leave.getLeaveType() + "\n"
//                + "Dates: " + leave.getStartDate() + " to " + leave.getEndDate() + "\n"
//                + "Days: " + leave.getTotalDays() + "\n"
//                + (decision == ApprovalStatus.REJECTED ? "Reason: " + (reason == null ? "" : reason) + "\n" : "")
//                + "\nRegards,\nHR Team";
//        sendPlainText(employee.getEmail(), subject, body);
//    }
//
//    // Regularization notifications
//
//    @Override
//    @Async("emailTaskExecutor")
//    public void notifyManagerForNewRegularization(Employee employee, RegularizationRequest regularizationRequest) {
//        Employee manager = employee.getManager();
//        if (manager == null || manager.getEmail() == null || manager.getEmail().isBlank()) return;
//
//        String subject = "New Regularization Request - " + employee.getFirstName() + " " + employee.getLastName();
//        String body = "Hello " + manager.getFirstName() + ",\n\n"
//                + employee.getFirstName() + " " + employee.getLastName() + " has applied for regularization.\n\n"
//                + "Date: " + regularizationRequest.getDate() + "\n"
//                + "Punch In: " + regularizationRequest.getPunchInTime() + "\n"
//                + "Punch Out: " + regularizationRequest.getPunchOutTime() + "\n"
//                + "Reason: " + regularizationRequest.getReason() + "\n"
//                + "Status: " + regularizationRequest.getStatus() + "\n\n"
//                + "Please review it in the portal.";
//        sendPlainText(manager.getEmail(), subject, body);
//    }
//
//    @Override
//    @Async("emailTaskExecutor")
//    public void notifyEmployeeForRegularizationDecision(Employee employee, RegularizationRequest regularizationRequest) {
//        if (employee.getEmail() == null || employee.getEmail().isBlank()) return;
//
//        String subject = "Regularization Request " + regularizationRequest.getStatus();
//        String body = "Hello " + employee.getFirstName() + ",\n\n"
//                + "Your regularization request has been " + regularizationRequest.getStatus() + ".\n\n"
//                + "Date: " + regularizationRequest.getDate() + "\n"
//                + "Punch In: " + regularizationRequest.getPunchInTime() + "\n"
//                + "Punch Out: " + regularizationRequest.getPunchOutTime() + "\n"
//                + "Reason: " + regularizationRequest.getReason() + "\n"
//                + (regularizationRequest.getStatus().name().equals("REJECTED")
//                ? "Reason for Rejection: "
//                + (regularizationRequest.getRejectionReason() == null ? "" : regularizationRequest.getRejectionReason()) + "\n"
//                : "")
//                + "\nRegards,\nHR Team";
//        sendPlainText(employee.getEmail(), subject, body);
//    }
//
//    // Timesheet notifications
//
//    @Override
//    @Async("emailTaskExecutor")
//    public void notifyManagerForTimesheetSubmission(Employee employee, Timesheet timesheet) {
//        Employee manager = employee.getManager();
//        if (manager == null || manager.getEmail() == null || manager.getEmail().isBlank()) return;
//
//        int mins = timesheet.getTotalMonthlyMinutes() != null ? timesheet.getTotalMonthlyMinutes() : 0;
//
//        String subject = "Timesheet Submitted - " + employee.getFirstName() + " " + employee.getLastName();
//        String body = "Hello " + manager.getFirstName() + ",\n\n"
//                + employee.getFirstName() + " " + employee.getLastName() + " has submitted a timesheet.\n\n"
//                + "Month/Year: " + timesheet.getMonth() + "/" + timesheet.getYear() + "\n"
//                + "Total Hours: " + String.format("%.1f", mins / 60.0) + "\n"
//                + "Status: " + timesheet.getStatus() + "\n\n"
//                + "Please review it in the portal.";
//        sendPlainText(manager.getEmail(), subject, body);
//    }
//
//    @Override
//    @Async("emailTaskExecutor")
//    public void notifyEmployeeForTimesheetDecision(Employee employee, Timesheet timesheet) {
//        if (employee.getEmail() == null || employee.getEmail().isBlank()) return;
//
//        int mins = timesheet.getTotalMonthlyMinutes() != null ? timesheet.getTotalMonthlyMinutes() : 0;
//
//        String subject = "Timesheet " + timesheet.getStatus();
//        String body = "Hello " + employee.getFirstName() + ",\n\n"
//                + "Your timesheet has been " + timesheet.getStatus() + ".\n\n"
//                + "Month/Year: " + timesheet.getMonth() + "/" + timesheet.getYear() + "\n"
//                + "Total Hours: " + String.format("%.1f", mins / 60.0) + "\n"
//                + (timesheet.getStatus().name().equals("REJECTED")
//                ? "Reason: " + (timesheet.getRejectionReason() == null ? "" : timesheet.getRejectionReason()) + "\n"
//                : "")
//                + "\nRegards,\nHR Team";
//        sendPlainText(employee.getEmail(), subject, body);
//    }
//}