package com.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient_proxies")
public class PatientProxy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient; // Main patient whose record is being accessed

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proxy_user_id", nullable = false)
    private User proxyUser; // Family member or caregiver granting access

    @Column(nullable = false)
    private String relationship; // e.g. Parent, Child, Spouse, Legal Guardian, Caregiver

    @Column(nullable = false)
    private String accessLevel; // FULL_ACCESS, APPOINTMENTS_ONLY, READ_ONLY

    @Column(nullable = false)
    private String status; // PENDING, ACTIVE, REVOKED

    private LocalDateTime createdAt;

    public PatientProxy() {}

    public PatientProxy(User patient, User proxyUser, String relationship, String accessLevel) {
        this.patient = patient;
        this.proxyUser = proxyUser;
        this.relationship = relationship;
        this.accessLevel = accessLevel;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }

    public User getProxyUser() { return proxyUser; }
    public void setProxyUser(User proxyUser) { this.proxyUser = proxyUser; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    public String getAccessLevel() { return accessLevel; }
    public void setAccessLevel(String accessLevel) { this.accessLevel = accessLevel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
