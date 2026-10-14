package com.whichone.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
public class Quiz {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;
    @Column(nullable = false, length = 120)
    private String title;
    @Column(nullable = false, length = 1000)
    private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private QuizType type;
    @Column(nullable = false)
    private boolean published;
    @Column(nullable = false)
    private Instant createdAt;
    private Instant publishedAt;
    @Version
    private long version;
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<Question> questions = new ArrayList<>();
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<PersonalityOutcome> outcomes = new ArrayList<>();

    protected Quiz() { }

    public Quiz(AppUser author) {
        this.author = author;
        this.createdAt = Instant.now();
    }

    public void define(String title, String description, QuizType type) {
        this.title = title;
        this.description = description;
        this.type = type;
    }

    public void clearDraftContents() {
        questions.clear();
        outcomes.clear();
    }

    public void addQuestion(Question question) { questions.add(question); }
    public void addOutcome(PersonalityOutcome outcome) { outcomes.add(outcome); }

    public void publish() {
        if (!published) {
            published = true;
            publishedAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public AppUser getAuthor() { return author; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public QuizType getType() { return type; }
    public boolean isPublished() { return published; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public List<Question> getQuestions() { return questions; }
    public List<PersonalityOutcome> getOutcomes() { return outcomes; }
}
