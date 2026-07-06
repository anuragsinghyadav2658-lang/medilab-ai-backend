package com.medilab.backendlabreportassistant.controller;

import com.medilab.backendlabreportassistant.entity.MedicalReport;
import com.medilab.backendlabreportassistant.entity.User;
import com.medilab.backendlabreportassistant.repository.MedicalReportRepository;
import com.medilab.backendlabreportassistant.repository.UserRepository;
import com.medilab.backendlabreportassistant.service.AiService;
import com.medilab.backendlabreportassistant.service.EmailService;
import com.medilab.backendlabreportassistant.service.ReportService;
import com.medilab.backendlabreportassistant.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/reports")
// @CrossOrigin(origins = "*") // Class-level CORS enabled
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private AiService aiService;

    @Autowired
    private MedicalReportRepository medicalReportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtUtil jwtUtil;

    // 1. Report upload karne, analyze karne aur Critical Email Alerts trigger karne
    // ka endpoint
    @PostMapping("/upload")
    public ResponseEntity<?> uploadReport(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Report save aur AI analysis run karna
        MedicalReport savedReport = reportService.saveAndAnalyzeReport(file);

        if (savedReport == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process report.");
        }

        // Critical Email Alert Trigger Logic
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String email = jwtUtil.verifyTokenAndGetEmail(token);
                Optional<User> userOptional = userRepository.findByEmail(email);

                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    String aiSummary = savedReport.getAiSummary();

                    // 1. Check if User has enabled email alerts
                    if (user.isEmailAlertsEnabled() && aiSummary != null) {

                        // 2. Case-insensitive keyword checking (High, Critical, Abnormal)
                        String lowerSummary = aiSummary.toLowerCase();
                        if (lowerSummary.contains("high") || lowerSummary.contains("critical")
                                || lowerSummary.contains("abnormal")) {

                            // 3. Email trigger karna
                            emailService.sendCriticalAlert(user.getEmail(), user.getFullName(), aiSummary);
                        }
                    }
                }
            } catch (Exception e) {
                // System crash na ho isliye error console me print karke flow normal rakhenge
                System.err.println("Error processing auto email alert trigger: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(savedReport);
    }

    // 2. Database se saari reports laane ke liye
    @GetMapping("/all")
    public ResponseEntity<List<MedicalReport>> getAllReports() {
        List<MedicalReport> reports = reportService.getAllReports();
        return ResponseEntity.ok(reports);
    }

    // 3. AI Chatbot Endpoint (Context-Aware)
    @PostMapping("/chat")
    public ResponseEntity<String> chatWithAi(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");

        MedicalReport latestReport = medicalReportRepository.findAll()
                .stream()
                .max((r1, r2) -> r1.getUploadDate().compareTo(r2.getUploadDate()))
                .orElse(null);

        String context = "";
        if (latestReport != null && latestReport.getAiSummary() != null) {
            context = latestReport.getAiSummary();
        }

        String aiResponse = aiService.chatWithMedicalAi(userMessage, context);
        return ResponseEntity.ok(aiResponse);
    }

    // 4. Dashboard ke liye sabse latest report fetch karne ke liye
    @GetMapping("/latest")
    public ResponseEntity<MedicalReport> getLatestReport() {
        MedicalReport latestReport = medicalReportRepository.findAll()
                .stream()
                .max((r1, r2) -> r1.getUploadDate().compareTo(r2.getUploadDate()))
                .orElse(null);

        if (latestReport == null) {
            return ResponseEntity.ok().body(null);
        }
        return ResponseEntity.ok(latestReport);
    }
}
