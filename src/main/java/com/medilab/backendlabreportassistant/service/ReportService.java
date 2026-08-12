package com.medilab.backendlabreportassistant.service;

import com.medilab.backendlabreportassistant.entity.MedicalReport;
import com.medilab.backendlabreportassistant.repository.MedicalReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.medilab.backendlabreportassistant.entity.Patient;
import com.medilab.backendlabreportassistant.repository.PatientRepository;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private MedicalReportRepository medicalReportRepository;

    @Autowired
    private AiService aiService;

    public MedicalReport saveAndAnalyzeReport(MultipartFile file, Long patientId) {
        String aiResult = aiService.analyzePdfReport(file);

        MedicalReport report = new MedicalReport();
        report.setFileName(file.getOriginalFilename());
        report.setFileType(file.getContentType());
        report.setAiSummary(aiResult);
        report.setUploadDate(LocalDateTime.now());

        // Patient ko ID se nikal kar map karna
        if (patientId != null) {
            Patient patient = patientRepository.findById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + patientId));
            report.setPatient(patient);
        }

        return medicalReportRepository.save(report);
    }

    // Database se saari reports nikalne ke liye
    public List<MedicalReport> getAllReports() {
        return medicalReportRepository.findAll();
    }
}
