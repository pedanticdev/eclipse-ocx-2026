package com.azul.eclipseocx2026.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "conference_talks")
public class ConferenceTalk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 2000)
    private String abstractText;

    private String speakerName;

    private String speakerCompany;

    @Column(length = 1000)
    private String speakerBio;

    private String track;

    private String timeSlot;

    protected ConferenceTalk() {}

    public ConferenceTalk(String title, String abstractText, String speakerName,
                          String speakerCompany, String speakerBio, String track, String timeSlot) {
        this.title = title;
        this.abstractText = abstractText;
        this.speakerName = speakerName;
        this.speakerCompany = speakerCompany;
        this.speakerBio = speakerBio;
        this.track = track;
        this.timeSlot = timeSlot;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getAbstractText() { return abstractText; }
    public String getSpeakerName() { return speakerName; }
    public String getSpeakerCompany() { return speakerCompany; }
    public String getSpeakerBio() { return speakerBio; }
    public String getTrack() { return track; }
    public String getTimeSlot() { return timeSlot; }
}
