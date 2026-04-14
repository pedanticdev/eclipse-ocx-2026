package com.azul.eclipseocx2026.repository;

import com.azul.eclipseocx2026.model.DocumentChunk;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
public interface Chunks extends BasicRepository<DocumentChunk, Long> {

    @Find
    List<DocumentChunk> findBySource(String source);

    @Query("where source like :pattern order by id asc")
    List<DocumentChunk> findBySourceLike(@Param("pattern") String pattern);

    @Query("select count(this) where source = :source")
    long countBySource(@Param("source") String source);
}
