package org.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Класс, представляющий вопрос диагностического теста.
 * Содержит текст вопроса, параметр для оценки и варианты ответов с баллами.
 */
public class DiagnosticQuestion {
    // Текст вопроса, который увидит пользователь
    private String questionText;

    // Название параметра, который оценивается этим вопросом
    private String parameterName;

    // Варианты ответов и соответствующие им баллы:
    // Ключ - текст ответа, Значение - балл за этот ответ
    private Map<String, Integer> answerValues;

    /**
     * Конструктор для десериализации из JSON
     * @param questionText текст вопроса
     * @param parameterName название оцениваемого параметра
     * @param answers варианты ответов с баллами
     */
    @JsonCreator
    public DiagnosticQuestion(
            @JsonProperty("questionText") String questionText,
            @JsonProperty("parameterName") String parameterName,
            @JsonProperty("answers") Map<String, Integer> answers) {
        this.questionText = questionText;
        this.parameterName = parameterName;
        // Создаем копию Map для защиты от внешних изменений
        this.answerValues = answers != null ? new HashMap<>(answers) : new HashMap<>();
    }

    /**
     * @return текст вопроса
     */
    public String getQuestionText() {
        return questionText;
    }

    /**
     * @return название оцениваемого параметра
     */
    public String getParameterName() {
        return parameterName;
    }

    /**
     * Возвращает копию Map с вариантами ответов и баллами
     * @return неизменяемую копию answerValues
     */
    public Map<String, Integer> getAnswerValues() {
        return new HashMap<>(answerValues);
    }

    /**
     * Возвращает список возможных вариантов ответа
     * @return список текстов ответов
     */
    public List<String> getPossibleAnswers() {
        return new ArrayList<>(answerValues.keySet());
    }

    /**
     * Получить балл за конкретный вариант ответа
     * @param answer текст ответа
     * @return балл за ответ или null если ответ не найден
     */
    public Integer getValueForAnswer(String answer) {
        return answerValues.get(answer);
    }
}