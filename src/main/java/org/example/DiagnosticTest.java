package org.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class DiagnosticTest {
    private String testName;
    private List<DiagnosticQuestion> questions;
    private Map<String, String> diagnosisMap;

    @JsonCreator
    public DiagnosticTest(
            @JsonProperty("testName") String testName,
            @JsonProperty("questions") List<DiagnosticQuestion> questions,
            @JsonProperty("diagnosisRules") Map<String, String> diagnosisRules) {
        this.testName = testName;
        this.questions = questions != null ? questions : new ArrayList<>();
        this.diagnosisMap = diagnosisRules != null ? diagnosisRules : new HashMap<>();
    }

    public void addQuestion(DiagnosticQuestion question) {
        questions.add(question);
    }

    public void addDiagnosisRule(String scoreRange, String diagnosis) {
        diagnosisMap.put(scoreRange, diagnosis);
    }

    public List<DiagnosticQuestion> getQuestions() {
        return new ArrayList<>(questions);
    }

    public String getTestName() {
        return testName;
    }

    public String evaluateDiagnosis(int totalScore) {
        for (Map.Entry<String, String> entry : diagnosisMap.entrySet()) {
            if (isScoreInRange(totalScore, entry.getKey())) {
                return entry.getValue();
            }
        }
        return "Не удалось определить диагноз";
    }

    private boolean isScoreInRange(int score, String range) {
        if (range.contains("-")) {
            String[] parts = range.split("-");
            int min = Integer.parseInt(parts[0].trim());
            int max = Integer.parseInt(parts[1].trim());
            return score >= min && score <= max;
        } else if (range.startsWith("<=")) {
            int max = Integer.parseInt(range.substring(2).trim());
            return score <= max;
        } else if (range.startsWith(">=")) {
            int min = Integer.parseInt(range.substring(2).trim());
            return score >= min;
        }
        return false;
    }
}