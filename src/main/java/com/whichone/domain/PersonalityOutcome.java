package com.whichone.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "personality_outcomes")
public class PersonalityOutcome {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;
    @Column(nullable = false, length = 24)
    private String code;
    @Column(nullable = false, length = 120)
    private String label;
    @Column(nullable = false, length = 500)
    private String description;
    @Column(name = "outcome_position", nullable = false)
    private int position;

    protected PersonalityOutcome() { }

    public PersonalityOutcome(Quiz quiz, String code, String label, String description, int position) {
        this.quiz = quiz;
        this.code = code;
        this.label = label;
        this.description = description;
        this.position = position;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public int getPosition() { return position; }
}
