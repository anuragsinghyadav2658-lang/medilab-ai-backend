package com.medilab.backendlabreportassistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    private String fileType;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime uploadDate;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String aiAnalysisResult;
}
