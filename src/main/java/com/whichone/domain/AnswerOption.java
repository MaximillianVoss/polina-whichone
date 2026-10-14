package com.whichone.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "answer_options")
public class AnswerOption {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
    @Column(nullable = false, length = 300)
    private String text;
    @Column(name = "option_position", nullable = false)
    private int position;
    @Column(nullable = false)
    private boolean correct;
    @Column(length = 24)
    private String outcomeCode;
    @Column(nullable = false)
    private int weight;

    protected AnswerOption() { }

    public AnswerOption(Question question, String text, int position, boolean correct, String outcomeCode, int weight) {
        this.question = question;
        this.text = text;
        this.position = position;
        this.correct = correct;
        this.outcomeCode = outcomeCode;
        this.weight = weight;
    }

    public Long getId() { return id; }
    public Question getQuestion() { return question; }
    public String getText() { return text; }
    public int getPosition() { return position; }
    public boolean isCorrect() { return correct; }
    public String getOutcomeCode() { return outcomeCode; }
    public int getWeight() { return weight; }
}
