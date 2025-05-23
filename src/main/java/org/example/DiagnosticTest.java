package org.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Класс, представляющий диагностический тест.
 * Содержит название теста, список вопросов и правила интерпретации результатов.
 */
public class DiagnosticTest {
    private String testName;

    // Список вопросов теста
    private List<DiagnosticQuestion> questions;

    private Map<String, String> diagnosisMap;

    /**
     * Конструктор для десериализации из JSON
     * @param testName название теста
     * @param questions список вопросов
     * @param diagnosisRules правила интерпретации результатов
     */
    @JsonCreator
    public DiagnosticTest(
            @JsonProperty("testName") String testName,
            @JsonProperty("questions") List<DiagnosticQuestion> questions,
            @JsonProperty("diagnosisRules") Map<String, String> diagnosisRules) {
        this.testName = testName;
        // Защита от null при инициализации коллекций
        this.questions = questions != null ? questions : new ArrayList<>();
        this.diagnosisMap = diagnosisRules != null ? diagnosisRules : new HashMap<>();
    }

    /**
     * Добавить вопрос в тест
     * @param question объект DiagnosticQuestion
     */
    public void addQuestion(DiagnosticQuestion question) {
        questions.add(question);
    }

    /**
     * Добавить правило интерпретации результатов
     * @param scoreRange диапазон баллов (например "0-5")
     * @param diagnosis текст диагноза/результата
     */
    public void addDiagnosisRule(String scoreRange, String diagnosis) {
        diagnosisMap.put(scoreRange, diagnosis);
    }

    /**
     * Получить копию списка вопросов
     * @return новый список вопросов
     */
    public List<DiagnosticQuestion> getQuestions() {
        return new ArrayList<>(questions); // Возвращаем копию для защиты от изменений
    }

    /**
     * Получить название теста
     * @return название теста
     */
    public String getTestName() {
        return testName;
    }

    /**
     * Оценить результат теста на основе суммы баллов
     * @param totalScore общая сумма баллов
     * @return текстовый результат диагностики
     */
    public String evaluateDiagnosis(int totalScore) {
        // Проверяем каждый диапазон в правилах
        for (Map.Entry<String, String> entry : diagnosisMap.entrySet()) {
            if (isScoreInRange(totalScore, entry.getKey())) {
                return entry.getValue();
            }
        }
        return "Не удалось определить диагноз"; // Значение по умолчанию
    }

    /**
     * Проверяет попадание балла в диапазон
     * @param score проверяемый балл
     * @param range строка диапазона (форматы: "X-Y", "<=X", ">=Y")
     * @return true если балл попадает в диапазон
     */
    private boolean isScoreInRange(int score, String range) {
        if (range.contains("-")) {
            // Обработка диапазона вида "X-Y"
            String[] parts = range.split("-");
            int min = Integer.parseInt(parts[0].trim());
            int max = Integer.parseInt(parts[1].trim());
            return score >= min && score <= max;
        } else if (range.startsWith("<=")) {
            // Обработка "<=X"
            int max = Integer.parseInt(range.substring(2).trim());
            return score <= max;
        } else if (range.startsWith(">=")) {
            // Обработка ">=Y"
            int min = Integer.parseInt(range.substring(2).trim());
            return score >= min;
        }
        return false;
    }
}