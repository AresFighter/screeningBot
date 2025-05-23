package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Основной класс Telegram-бота для медицинской диагностики.
 * Обрабатывает команды пользователя, управляет тестовыми сессиями и выдает результаты.
 */
public class MedicalDiagnosisBot extends TelegramLongPollingBot {
    // Логгер для записи событий
    private final Logger logger = LoggerFactory.getLogger(MedicalDiagnosisBot.class);

    // Учетные данные бота из переменных окружения
    private final String botToken;
    private final String botUsername;

    // Активные сессии пользователей (chatId -> сессия)
    private Map<String, DiagnosisSession> userSessions = new HashMap<>();

    // Доступные диагностические тесты
    private final List<DiagnosticTest> availableTests;

    /**
     * Конструктор бота. Инициализирует:
     * 1. Учетные данные из config.env
     * 2. Доступные тесты из JSON-конфига
     */
    public MedicalDiagnosisBot() {
        // Загрузка конфигурации из файла .env
        Dotenv dotenv = Dotenv.configure()
                .filename("config.env")
                .ignoreIfMissing()
                .load();

        this.botToken = dotenv.get("BOT_TOKEN");
        this.botUsername = dotenv.get("BOT_USERNAME");
        this.userSessions = new HashMap<>();

        // Проверка обязательных параметров
        if (botToken == null || botUsername == null) {
            logger.error("Не указаны BOT_TOKEN или BOT_USERNAME в config.env!");
            throw new RuntimeException("Не указаны BOT_TOKEN или BOT_USERNAME в config.env!");
        }

        try {
            // Загрузка тестов из JSON-файла
            this.availableTests = loadTests("/tests_config.json");
            logger.info("Бот успешно инициализирован, загружено тестов: {}", availableTests.size());
        } catch (IOException e) {
            logger.error("Ошибка загрузки тестов", e);
            throw new RuntimeException("Ошибка загрузки тестов", e);
        }
    }

    /**
     * Загружает тесты из JSON-файла.
     * Поддерживает два формата:
     * 1. Прямой массив тестов: [{...}, {...}]
     * 2. Объект с полем tests: {"tests": [...]}
     */
    private List<DiagnosticTest> loadTests(String configPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = getClass().getResourceAsStream(configPath)) {
            if (is == null) {
                throw new IOException("Файл конфигурации не найден: " + configPath);
            }

            // Чтение и анализ структуры JSON
            JsonNode rootNode = mapper.readTree(is);

            if (rootNode.isArray()) {
                // Формат: массив тестов
                return mapper.readValue(rootNode.toString(), new TypeReference<List<DiagnosticTest>>() {});
            } else if (rootNode.has("tests")) {
                // Формат: объект с полем tests
                return mapper.readValue(rootNode.get("tests").toString(), new TypeReference<List<DiagnosticTest>>() {});
            } else {
                throw new IOException("Неверный формат конфигурационного файла");
            }
        }
    }

    /**
     * Основной обработчик входящих сообщений.
     * Игнорирует сообщения без текста.
     */
    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        String chatId = update.getMessage().getChatId().toString();
        String messageText = update.getMessage().getText();

        try {
            SendMessage response = processMessage(chatId, messageText);
            execute(response);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при обработке сообщения: {}", messageText, e);
        }
    }

    /**
     * Маршрутизатор команд. Определяет тип сообщения и вызывает соответствующий обработчик.
     */
    private SendMessage processMessage(String chatId, String message) {
        logger.debug("Обработка сообщения от {}: {}", chatId, message);

        switch (message) {
            case "/start":
                return createMessage(chatId, "Добро пожаловать в медицинский диагностический бот!\n\n" +
                        "Используйте команды:\n" +
                        "/glasgow - Тест Глазго (оценка уровня сознания)\n" +
                        "/help - Показать справку\n" +
                        "/cancel - Отменить текущий тест");
            case "/help":
                return helpCommand(chatId);
            case "/glasgow":
                return startGlasgowTest(chatId);
            case "/cancel":
                return cancelSession(chatId);
            default:
                return handleUserResponse(chatId, message);
        }
    }

    /**
     * Отправляет пользователю справку по командам бота.
     */
    private SendMessage helpCommand(String chatId) {
        logger.debug("Запрос справки от {}", chatId);
        return createMessage(chatId,
                "Справка по боту:\n\n" +
                        "Этот бот позволяет пройти медицинские диагностические тесты.\n\n" +
                        "Доступные команды:\n" +
                        "/glasgow - Тест Глазго (оценка уровня сознания)\n" +
                        "/help - Показать эту справку\n" +
                        "/cancel - Отменить текущий тест\n\n" +
                        "Во время прохождения теста просто вводите номер выбранного ответа.");
    }

    /**
     * Начинает новый тест Глазго для пользователя.
     * Создает новую сессию и задает первый вопрос.
     */
    private SendMessage startGlasgowTest(String chatId) {
        logger.info("Начало теста Глазго для {}", chatId);
        try {
            // Поиск теста Глазго среди доступных
            DiagnosticTest glasgowTest = availableTests.stream()
                    .filter(t -> t.getTestName().contains("Глазго"))
                    .findFirst()
                    .orElse(null);

            if (glasgowTest == null) {
                logger.error("Тест Глазго не найден в конфигурации");
                return createMessage(chatId, "Тест временно недоступен");
            }

            // Создание и сохранение сессии
            DiagnosisSession session = new DiagnosisSession(glasgowTest);
            userSessions.put(chatId, session);
            logger.debug("Создана новая сессия для {}", chatId);

            return askNextQuestion(chatId, session);
        } catch (Exception e) {
            logger.error("Ошибка при старте теста Глазго", e);
            return createMessage(chatId, "Произошла ошибка при запуске теста");
        }
    }

    /**
     * Формирует сообщение со следующим вопросом теста.
     * Включает номер вопроса, текст вопроса и варианты ответов.
     */
    private SendMessage askNextQuestion(String chatId, DiagnosisSession session) {
        try {
            DiagnosticQuestion nextQuestion = session.getNextQuestion();
            if (nextQuestion == null) {
                logger.error("Нет вопросов в тесте для {}", chatId);
                return createMessage(chatId, "Произошла ошибка: нет вопросов в тесте");
            }

            // Формирование текста сообщения
            StringBuilder messageText = new StringBuilder();
            messageText.append("Вопрос ").append(session.getCurrentQuestionNumber())
                    .append(" из ").append(session.getTotalQuestions()).append(":\n")
                    .append(nextQuestion.getQuestionText()).append("\n\n");

            // Добавление вариантов ответов
            List<String> possibleAnswers = nextQuestion.getPossibleAnswers();
            for (int i = 0; i < possibleAnswers.size(); i++) {
                messageText.append(i + 1).append(". ").append(possibleAnswers.get(i)).append("\n");
            }

            logger.debug("Отправлен вопрос {} для {}", session.getCurrentQuestionNumber(), chatId);

            // Создание и настройка сообщения
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(messageText.toString());
            message.setReplyMarkup(new ReplyKeyboardRemove(true)); // Удаление клавиатуры

            return message;
        } catch (Exception e) {
            logger.error("Ошибка при получении вопроса для {}", chatId, e);
            return createMessage(chatId, "Произошла ошибка при получении вопроса");
        }
    }

    /**
     * Обрабатывает ответ пользователя на вопрос теста.
     * Записывает баллы, проверяет завершение теста и либо задает следующий вопрос,
     * либо выводит результат.
     */
    private SendMessage handleUserResponse(String chatId, String message) {
        try {
            // Получение текущей сессии
            DiagnosisSession session = userSessions.get(chatId);
            if (session == null) {
                logger.warn("Попытка ответа без активной сессии: {}", chatId);
                return createMessage(chatId,
                        "У вас нет активного теста. Начните тест с помощью команды /glasgow");
            }

            // Проверка текущего вопроса
            DiagnosticQuestion currentQuestion = session.getCurrentQuestion();
            if (currentQuestion == null) {
                logger.error("Текущий вопрос не найден для {}", chatId);
                return createMessage(chatId, "Ошибка: текущий вопрос не найден");
            }

            // Обработка номера ответа
            List<String> possibleAnswers = currentQuestion.getPossibleAnswers();
            int answerIndex = Integer.parseInt(message) - 1;

            if (answerIndex < 0 || answerIndex >= possibleAnswers.size()) {
                logger.warn("Некорректный ответ от {}: {}", chatId, message);
                return createMessage(chatId, "Пожалуйста, введите номер ответа из предложенных");
            }

            // Запись ответа
            String selectedAnswer = possibleAnswers.get(answerIndex);
            Integer answerValue = currentQuestion.getValueForAnswer(selectedAnswer);
            session.recordAnswer(currentQuestion.getParameterName(), answerValue);
            logger.debug("Записан ответ от {}: {} = {}", chatId, selectedAnswer, answerValue);

            // Проверка завершения теста
            if (session.isComplete()) {
                String diagnosis = session.getDiagnosisResult();
                userSessions.remove(chatId);
                logger.info("Тест завершен для {}, результат: {}", chatId, diagnosis);
                return createMessage(chatId,
                        "Диагностика завершена.\n\n" +
                                "Результат: " + diagnosis + "\n\n" +
                                "Для нового теста используйте команду /glasgow");
            } else {
                return askNextQuestion(chatId, session);
            }
        } catch (NumberFormatException e) {
            logger.warn("Некорректный формат ответа от {}: {}", chatId, message);
            return createMessage(chatId, "Пожалуйста, введите номер ответа (1, 2, 3 и т.д.)");
        } catch (Exception e) {
            logger.error("Ошибка обработки ответа от {}", chatId, e);
            return createMessage(chatId, "Произошла ошибка при обработке вашего ответа");
        }
    }

    /**
     * Отменяет текущую тестовую сессию пользователя.
     */
    private SendMessage cancelSession(String chatId) {
        try {
            if (userSessions.containsKey(chatId)) {
                userSessions.remove(chatId);
                logger.info("Сессия отменена для {}", chatId);
                return createMessage(chatId,
                        "Текущий тест отменен. Вы можете начать новый тест с помощью команды /glasgow");
            }
            logger.warn("Попытка отмены несуществующей сессии: {}", chatId);
            return createMessage(chatId, "Нет активного теста для отмены");
        } catch (Exception e) {
            logger.error("Ошибка при отмене сессии для {}", chatId, e);
            return createMessage(chatId, "Произошла ошибка при отмене теста");
        }
    }

    /**
     * Вспомогательный метод для создания текстового сообщения.
     */
    private SendMessage createMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        return message;
    }

    /**
     * Возвращает имя бота (из конфигурации).
     */
    @Override
    public String getBotUsername() {
        return botUsername;
    }

    /**
     * Возвращает токен бота (из конфигурации).
     */
    @Override
    public String getBotToken() {
        return botToken;
    }
}