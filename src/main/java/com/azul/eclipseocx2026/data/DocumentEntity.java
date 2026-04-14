package com.azul.eclipseocx2026.data;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String source;

    @Lob
    private String content;

    private LocalDateTime createdAt;

    protected DocumentEntity() {}

    public DocumentEntity(String title, String source, String content) {
        this.title = title;
        this.source = source;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getSource() { return source; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
