package com.whichone.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;
    @Column(nullable = false)
    private Instant submittedAt;
    private Integer score;
    private Integer maxScore;
    @Column(length = 24)
    private String outcomeCode;
    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<AttemptAnswer> answers = new ArrayList<>();

    protected QuizAttempt() { }

    public QuizAttempt(AppUser user, Quiz quiz, Integer score, Integer maxScore, String outcomeCode) {
        this.user = user;
        this.quiz = quiz;
        this.score = score;
        this.maxScore = maxScore;
        this.outcomeCode = outcomeCode;
        this.submittedAt = Instant.now();
    }

    public void addAnswer(AnswerOption option) { answers.add(new AttemptAnswer(this, option)); }
    public Long getId() { return id; }
    public Quiz getQuiz() { return quiz; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Integer getScore() { return score; }
    public Integer getMaxScore() { return maxScore; }
    public String getOutcomeCode() { return outcomeCode; }
    public List<AttemptAnswer> getAnswers() { return answers; }
}
