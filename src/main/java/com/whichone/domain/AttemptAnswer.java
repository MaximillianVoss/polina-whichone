package com.whichone.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "attempt_answers")
public class AttemptAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private AnswerOption option;

    protected AttemptAnswer() { }

    public AttemptAnswer(QuizAttempt attempt, AnswerOption option) {
        this.attempt = attempt;
        this.option = option;
    }

    public AnswerOption getOption() { return option; }
}
