package com.medilab.backendlabreportassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.api.key}")
    private String apiKey;

    // 1. Multimodal File Analysis (PDF & Images) - UNCHANGED
    public String analyzePdfReport(MultipartFile file) {
        try {
            String contentType = file.getContentType();
            
            // Updated Prompt with Strict Extraction Rules
            String prompt = "Act as an expert doctor. Analyze this medical lab report and give a simple summary in 2-3 lines for a patient."
                          + "\n\nIMPORTANT: At the end of your analysis, you MUST strictly append the exact vital signs in this exact format: [HeartRate: value] [BP: value] [BloodSugar: value] [Temp: value]. If a value is missing, write N/A.";
            
            RestTemplate restTemplate = new RestTemplate();
            ObjectMapper objectMapper = new ObjectMapper();
            String url = apiUrl + "?key=" + apiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            List<Map<String, Object>> partsList = new ArrayList<>();
            Map<String, Object> textPart = new HashMap<>();

            // Agar file Image hai (PNG/JPEG)
            if (contentType != null && (contentType.equals("image/png") || contentType.equals("image/jpeg") || contentType.equals("image/jpg"))) {
                textPart.put("text", prompt);
                partsList.add(textPart);

                String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
                
                Map<String, Object> inlineData = new HashMap<>();
                inlineData.put("mime_type", contentType);
                inlineData.put("data", base64Image);

                Map<String, Object> imagePart = new HashMap<>();
                imagePart.put("inline_data", inlineData);
                partsList.add(imagePart);
            } 
            // Agar file PDF hai
            else if (contentType != null && contentType.equals("application/pdf")) {
                String extractedText = "";
                try (PDDocument document = Loader.loadPDF(file.getBytes())) {
                    PDFTextStripper pdfStripper = new PDFTextStripper();
                    extractedText = pdfStripper.getText(document);
                }

                if (extractedText == null || extractedText.trim().isEmpty()) {
                    return "Error: Could not extract text. The PDF might be empty or a scanned image.";
                }

                textPart.put("text", prompt + "\n\n[EXTRACTED_TEXT]\n" + extractedText);
                partsList.add(textPart);
            } 
            // Invalid file type
            else {
                return "Error: Unsupported file format. Please upload a PDF, PNG, or JPEG file.";
            }

            // Gemini API ka request payload build karna
            Map<String, Object> contentMap = new HashMap<>();
            contentMap.put("parts", partsList);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(contentMap));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            return rootNode.path("candidates")
                           .get(0)
                           .path("content")
                           .path("parts")
                           .get(0)
                           .path("text")
                           .asText();

        } catch (Exception e) {
            e.printStackTrace();
            return "AI Analysis failed. Error: " + e.getMessage();
        }
    }

    // 2. Context-Aware Chatbot Method
    public String chatWithMedicalAi(String userMessage, String context) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            ObjectMapper objectMapper = new ObjectMapper();
            String url = apiUrl + "?key=" + apiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String prompt;
            
            // Context check: Agar context hai toh usko prompt me daalo
            if (context != null && !context.trim().isEmpty()) {
                prompt = "You are an expert AI medical assistant named MediLab AI. Here is the user's latest medical report summary: " 
                         + context 
                         + "\n\nBased on this data, answer the user's health-related query in a helpful, concise, and easy-to-understand manner. Query: " 
                         + userMessage;
            } else {
                // Bina context wala normal behavior
                prompt = "You are an expert AI medical assistant named MediLab AI. Answer the user's health-related query in a helpful, concise, and easy-to-understand manner. Query: " 
                         + userMessage;
            }

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);

            Map<String, Object> partsMap = new HashMap<>();
            partsMap.put("parts", List.of(textPart));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(partsMap));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            return rootNode.path("candidates")
                           .get(0)
                           .path("content")
                           .path("parts")
                           .get(0)
                           .path("text")
                           .asText();

        } catch (Exception e) {
            e.printStackTrace();
            return "Sorry, I am facing some technical issues right now. Please try again later.";
        }
    }
}
