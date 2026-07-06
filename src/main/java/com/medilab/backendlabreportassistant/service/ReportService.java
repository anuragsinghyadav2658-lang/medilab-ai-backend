package com.medilab.backendlabreportassistant.service;

import com.medilab.backendlabreportassistant.entity.MedicalReport;
import com.medilab.backendlabreportassistant.repository.MedicalReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportService {

    @Autowired
    private MedicalReportRepository medicalReportRepository;

    @Autowired
    private AiService aiService;

    // React se aayi PDF/Image file receive karega, analyze karega aur TiDB me save karega
    public MedicalReport saveAndAnalyzeReport(MultipartFile file) {
        
        // 1. File ko AiService me bhej kar extract & analyze karana
        String aiResult = aiService.analyzePdfReport(file);
        
        // 2. Naya MedicalReport object banana aur details set karna
        MedicalReport report = new MedicalReport();
        report.setFileName(file.getOriginalFilename());
        report.setFileType(file.getContentType());
        report.setAiSummary(aiResult);
        report.setUploadDate(LocalDateTime.now()); // Explicitly date set kar di
        
        // 3. Database (TiDB/MySQL) me save karna
        return medicalReportRepository.save(report);
    }

    // Database se saari reports nikalne ke liye
    public List<MedicalReport> getAllReports() {
        return medicalReportRepository.findAll();
    }
}
