package vn.com.anhemsoftware.license_app.service;

public interface EmailService {
    void sendEmail(String to, String subject, String htmlContent);
}
