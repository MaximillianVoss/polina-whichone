package com.whichone.service;

import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.List;

/** Pure calculations: no HTTP, no database, no client-supplied score. */
@Service
public class ScoringService {
    public record EducationalAnswer(boolean correct, int points) { }
    public record EducationalScore(int score, int maxScore) { }
    public record PersonalityVote(String outcomeCode, int weight) { }

    public EducationalScore educational(List<EducationalAnswer> answers) {
        int max = answers.stream().mapToInt(EducationalAnswer::points).sum();
        int score = answers.stream().filter(EducationalAnswer::correct).mapToInt(EducationalAnswer::points).sum();
        return new EducationalScore(score, max);
    }

    public String personality(List<String> orderedOutcomes, List<PersonalityVote> votes) {
        if (orderedOutcomes.isEmpty()) {
            throw new IllegalArgumentException("No personality outcomes");
        }
        var totals = new LinkedHashMap<String, Integer>();
        orderedOutcomes.forEach(code -> totals.put(code, 0));
        for (var vote : votes) {
            if (!totals.containsKey(vote.outcomeCode()) || vote.weight() < 0) {
                throw new IllegalArgumentException("Invalid personality vote");
            }
            totals.compute(vote.outcomeCode(), (code, total) -> total + vote.weight());
        }
        // Strict > preserves the first declared outcome when totals are equal.
        String winner = orderedOutcomes.get(0);
        for (String code : orderedOutcomes) {
            if (totals.get(code) > totals.get(winner)) {
                winner = code;
            }
        }
        return winner;
    }
}
