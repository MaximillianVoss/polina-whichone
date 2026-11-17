package com.whichone.service;

import com.whichone.domain.QuizType;
import com.whichone.dto.ApiDtos.*;
import java.util.List;

final class DemoQuizzes {
    private DemoQuizzes() { }

    static List<QuizInput> definitions() {
        var educational = new QuizInput("Основы Java", "Два вопроса. Максимум — 5 баллов.",
                QuizType.EDUCATIONAL, List.of(
                knowledge("Чему равно 2 + 2?", 3, "4", "5"),
                knowledge("Какое слово объявляет класс?", 2, "class", "table")), List.of());
        var personality = new QuizInput("Как ты учишься?",
                "Учебный пример с двумя исходами. При равенстве побеждает первый объявленный исход.",
                QuizType.PERSONALITY, List.of(
                preference("С чего ты начинаешь новую тему?", "Составляю план", "Пробую пример"),
                preference("Как тебе удобнее закреплять знания?", "По расписанию", "Через эксперименты")),
                List.of(new OutcomeInput("PLANNER", "Планировщик", "Тебе нравится сначала составить план."),
                        new OutcomeInput("EXPLORER", "Исследователь", "Тебе нравится исследовать через практику.")));
        return List.of(educational, personality);
    }

    private static QuestionInput knowledge(String text, int points, String correct, String incorrect) {
        return new QuestionInput(text, points, List.of(
                new OptionInput(correct, true, null, 0), new OptionInput(incorrect, false, null, 0)));
    }

    private static QuestionInput preference(String text, String plan, String explore) {
        return new QuestionInput(text, 0, List.of(
                new OptionInput(plan, false, "PLANNER", 2), new OptionInput(explore, false, "EXPLORER", 2)));
    }
}
