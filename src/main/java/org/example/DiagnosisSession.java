package org.example;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Класс для управления сессией прохождения диагностического теста.
 * Хранит состояние тестирования для конкретного пользователя.
 */
public class DiagnosisSession {
    // Текущий тест, который проходит пользователь
    private DiagnosticTest currentTest;

    // Коллекция для хранения ответов пользователя: ключ/значение - название параметра/баллы
    private Map<String, Integer> collectedAnswers;

    // Индекс текущего вопроса
    private int currentQuestionIndex;

    /**
     * Конструктор сессии
     * @param test - диагностический тест для прохождения
     */
    public DiagnosisSession(DiagnosticTest test) {
        this.currentTest = test;
        this.collectedAnswers = new HashMap<>();
        this.currentQuestionIndex = 0;
    }

    /**
     * Получить следующий вопрос теста
     * @return следующий вопрос или null, если тест завершен
     */
    public DiagnosticQuestion getNextQuestion() {
        if (currentQuestionIndex >= currentTest.getQuestions().size()) {
            return null;
        }
        return currentTest.getQuestions().get(currentQuestionIndex++);
    }

    /**
     * Записать ответ пользователя
     * @param parameterName - название параметра/характеристики
     * @param value - балл за ответ
     */
    public void recordAnswer(String parameterName, int value) {
        collectedAnswers.put(parameterName, value);
    }

    /**
     * Проверить, завершен ли тест
     * @return true если все вопросы пройдены
     */
    public boolean isComplete() {
        return currentQuestionIndex >= currentTest.getQuestions().size();
    }

    /**
     * Получить результат диагностики на основе ответов
     * @return строку с диагнозом/результатом
     */
    public String getDiagnosisResult() {
        int totalScore = collectedAnswers.values().stream().mapToInt(Integer::intValue).sum();
        return currentTest.evaluateDiagnosis(totalScore);
    }

    /**
     * Получить номер текущего вопроса (начиная с 1)
     * @return текущий номер вопроса
     */
    public int getCurrentQuestionNumber() {
        return currentQuestionIndex;
    }

    /**
     * Получить общее количество вопросов в тесте
     * @return общее число вопросов
     */
    public int getTotalQuestions() {
        return currentTest.getQuestions().size();
    }

    /**
     * Получить текущий вопрос (последний заданный)
     * @return текущий вопрос или null если вопросы не начаты/завершены
     */
    public DiagnosticQuestion getCurrentQuestion() {
        if (currentQuestionIndex == 0 || currentQuestionIndex > currentTest.getQuestions().size()) {
            return null;
        }
        return currentTest.getQuestions().get(currentQuestionIndex - 1);
    }

    /**
     * Получить все вопросы теста
     * @return список всех вопросов
     */
    public List<DiagnosticQuestion> getQuestions() {
        return currentTest.getQuestions();
    }
}