package com.medilab.backendlabreportassistant.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clinic_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClinicDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clinicName;

    private String doctorName;

    private String clinicLogoUrl;

    private String contactNumber;

    private String address;
}
