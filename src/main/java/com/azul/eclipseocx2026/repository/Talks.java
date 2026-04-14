package com.azul.eclipseocx2026.repository;

import com.azul.eclipseocx2026.model.ConferenceTalk;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
public interface Talks extends BasicRepository<ConferenceTalk, Long> {

    @Find
    List<ConferenceTalk> findByTrack(String track);

    @Query("where title like :pattern or abstractText like :pattern")
    List<ConferenceTalk> search(@Param("pattern") String pattern);
}
