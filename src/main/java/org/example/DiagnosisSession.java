package org.example;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class DiagnosisSession {
    private DiagnosticTest currentTest;
    private Map<String, Integer> collectedAnswers;
    private int currentQuestionIndex;

    public DiagnosisSession(DiagnosticTest test) {
        this.currentTest = test;
        this.collectedAnswers = new HashMap<>();
        this.currentQuestionIndex = 0;
    }

    public DiagnosticQuestion getNextQuestion() {
        if (currentQuestionIndex >= currentTest.getQuestions().size()) {
            return null;
        }
        return currentTest.getQuestions().get(currentQuestionIndex++);
    }

    public void recordAnswer(String parameterName, int value) {
        collectedAnswers.put(parameterName, value);
    }

    public boolean isComplete() {
        return currentQuestionIndex >= currentTest.getQuestions().size();
    }

    public String getDiagnosisResult() {
        int totalScore = collectedAnswers.values().stream().mapToInt(Integer::intValue).sum();
        return currentTest.evaluateDiagnosis(totalScore);
    }

    public int getCurrentQuestionNumber() {
        return currentQuestionIndex;
    }

    public int getTotalQuestions() {
        return currentTest.getQuestions().size();
    }

    public DiagnosticQuestion getCurrentQuestion() {
        if (currentQuestionIndex == 0 || currentQuestionIndex > currentTest.getQuestions().size()) {
            return null;
        }
        return currentTest.getQuestions().get(currentQuestionIndex - 1);
    }

    public List<DiagnosticQuestion> getQuestions() {
        return currentTest.getQuestions();
    }
}
