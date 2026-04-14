package com.azul.eclipseocx2026.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(length = 200)
    private String source;

    @Column(length = 200)
    private String title;

    @Lob
    @Column(columnDefinition = "bytea")
    private byte[] embedding;

    protected DocumentChunk() {}

    public DocumentChunk(String content, String source, String title) {
        this.content = content;
        this.source = source;
        this.title = title;
    }

    public Long getId() { return id; }
    public String getContent() { return content; }
    public String getSource() { return source; }
    public String getTitle() { return title; }
    public byte[] getEmbedding() { return embedding; }

    public void setEmbedding(byte[] embedding) { this.embedding = embedding; }

    public float[] getEmbeddingVector() {
        return embedding != null ? EmbeddingConverter.toFloatArray(embedding) : null;
    }

    public void setEmbeddingVector(float[] vector) {
        this.embedding = EmbeddingConverter.toByteArray(vector);
    }
}
