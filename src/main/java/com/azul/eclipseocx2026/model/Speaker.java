package com.azul.eclipseocx2026.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "speakers")
public class Speaker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(length = 1000)
    private String bio;

    private String company;
    private String role;

    public Speaker() {
    }

    public Speaker(String name, String bio, String company, String role) {
        this.name = name;
        this.bio = bio;
        this.company = company;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getBio() { return bio; }
    public String getCompany() { return company; }
    public String getRole() { return role; }
}
