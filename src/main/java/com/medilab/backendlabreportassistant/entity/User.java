package com.medilab.backendlabreportassistant.entity;

import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name")
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    private String password;

    @Column(columnDefinition = "boolean default false")
    private boolean emailAlertsEnabled = false;

    @Column(name = "phone")
    private String phone;

    // NAYA CODE: Social login track karne ke liye
    @Column(name = "auth_provider", columnDefinition = "varchar(50) default 'manual'")
    private String authProvider = "manual";

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }  
}
