package com.whichone.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_questions")
public class Question {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;
    @Column(nullable = false, length = 1000)
    private String text;
    @Column(name = "question_position", nullable = false)
    private int position;
    @Column(nullable = false)
    private int points;
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<AnswerOption> options = new ArrayList<>();

    protected Question() { }

    public Question(Quiz quiz, String text, int position, int points) {
        this.quiz = quiz;
        this.text = text;
        this.position = position;
        this.points = points;
    }

    public void addOption(AnswerOption option) { options.add(option); }
    public Long getId() { return id; }
    public Quiz getQuiz() { return quiz; }
    public String getText() { return text; }
    public int getPosition() { return position; }
    public int getPoints() { return points; }
    public List<AnswerOption> getOptions() { return options; }
}
