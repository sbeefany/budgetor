package budgetor.bot;

import budgetor.service.CategoryService;
import budgetor.service.TransactionParser;
import budgetor.service.TransactionService;
import budgetor.service.SummaryService;
import budgetor.service.GoalService;
import budgetor.service.TipService;
import budgetor.service.dto.SummaryDto;
import budgetor.service.dto.CategorySummaryDto;
import budgetor.dto.GoalProgressDto;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.springframework.core.io.ByteArrayResource;

import java.io.InputStream;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);
    private final BotConfig config;
    private final TransactionService transactionService;
    private final CategoryService categoryService;
    private final TransactionParser transactionParser;
    private final SummaryService summaryService;
    private final GoalService goalService;
    private final TipService tipService;

    public TelegramBot(BotConfig config,
                       TransactionService transactionService,
                       CategoryService categoryService,
                       TransactionParser transactionParser,
                       SummaryService summaryService,
                       GoalService goalService,
                       TipService tipService) {
        super(config.getToken());
        this.config = config;
        this.transactionService = transactionService;
        this.categoryService = categoryService;
        this.transactionParser = transactionParser;
        this.summaryService = summaryService;
        this.goalService = goalService;
        this.tipService = tipService;
    }

    @Override
    public String getBotUsername() {
        return config.getUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            var message = update.getMessage();
            if (message.hasText()) {
                handleTextMessage(message);
            } else if (message.hasPhoto()) {
                handlePhotoMessage(message);
            }
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        }
    }

    private void handleTextMessage(Message message) {
        var messageText = message.getText();
        var chatId = message.getChatId();

        if (messageText.startsWith("/")) {
            if (messageText.equals("/start")) {
                sendWelcomeMessage(chatId);
            }
            // Handle other commands if necessary
            return;
        }

        // AI Parsing Flow
        try {
            logger.info("Processing free-text transaction: '{}'", messageText);
            
            var availableCategories = categoryService.getAllCategories().stream()
                    .map(budgetor.domain.Category::getName)
                    .toList();
            
            var parsed = transactionParser.parse(messageText, availableCategories);
            var tx = transactionService.createTransaction(
                    parsed.amount(),
                    parsed.categoryName(),
                    parsed.description(),
                    parsed.type()
            );
            
            var response = """
                    ✅ Записано:
                    📝 Тип: %s
                    💰 Сумма: %.2f ₽
                    📂 Категория: %s
                    📄 Описание: %s
                    """.formatted(
                            tx.getType() == budgetor.domain.TransactionType.EXPENSE ? "Расход" : "Доход",
                            tx.getAmount(),
                            tx.getCategory().getName(),
                            tx.getDescription()
                    );
            
            sendConfirmationWithCancel(chatId, tx, response);
            
        } catch (Exception e) {
            logger.error("Error processing transaction: {}", e.getMessage());
            sendSimpleMessage(chatId, "❌ Извини, не удалось распознать трату. Попробуй написать понятнее, например: 'обед 350'.");
        }
    }

    private void handlePhotoMessage(Message message) {
        var chatId = message.getChatId();
        var photos = message.getPhoto();
        
        if (photos == null || photos.isEmpty()) {
            return;
        }

        // The last photo in the list is the largest one
        var largestPhoto = photos.get(photos.size() - 1);
        var fileId = largestPhoto.getFileId();

        try {
            logger.info("Processing photo transaction: fileId={}", fileId);
            
            // 1. Get File Info
            var getFile = new GetFile();
            getFile.setFileId(fileId);
            var file = execute(getFile);

            // 2. Download File
            try (InputStream is = downloadFileAsStream(file)) {
                var bytes = is.readAllBytes();
                var resource = new ByteArrayResource(bytes);

                // 3. AI Parsing flow
                var availableCategories = categoryService.getAllCategories().stream()
                        .map(budgetor.domain.Category::getName)
                        .toList();

                var parsed = transactionParser.parse(resource, availableCategories);
                
                var tx = transactionService.createTransaction(
                        parsed.amount(),
                        parsed.categoryName(),
                        parsed.description(),
                        parsed.type()
                );

                var response = """
                        ✅ Записано:
                        📝 Тип: %s
                        💰 Сумма: %.2f ₽
                        📂 Категория: %s
                        📄 Описание: %s
                        """.formatted(
                        tx.getType() == budgetor.domain.TransactionType.EXPENSE ? "Расход" : "Доход",
                        tx.getAmount(),
                        tx.getCategory().getName(),
                        tx.getDescription()
                );

                sendConfirmationWithCancel(chatId, tx, response);
            }
        } catch (Exception e) {
            logger.error("Error processing photo transaction: {}", e.getMessage(), e);
            sendSimpleMessage(chatId, "❌ Извини, не удалось распознать трату на фото. Попробуй сделать фото четче или отправь текст.");
        }
    }

    private void sendSimpleMessage(long chatId, String text) {
        var message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message: {}", e.getMessage());
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        var callbackData = callbackQuery.getData();
        var chatId = callbackQuery.getMessage().getChatId();
        String responseText;

        switch (callbackData) {
            case "menu_balance" -> responseText = "💰 Твой текущий баланс: %.2f ₽".formatted(transactionService.getCurrentBalance());
            case "menu_summary" -> responseText = formatSummary(summaryService.getSummary(
                    java.time.LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0),
                    java.time.LocalDateTime.now()));
            case "menu_goals" -> responseText = formatGoals(goalService.getAllGoalProgresses());
            case "menu_categories" -> responseText = formatCategories(categoryService.getAllCategories());
            case "menu_tips" -> responseText = tipService.generateTip();
            default -> {
                if (callbackData.startsWith("tx_cancel:")) {
                    handleTransactionCancel(callbackQuery);
                    return;
                }
                responseText = "Извини, я пока не знаю эту команду.";
            }
        }

        var message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(responseText);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending callback response: {}", e.getMessage());
        }
    }

    private void sendWelcomeMessage(long chatId) {
        String welcomeText = """
                👋 Привет! Я Budgetor — твой личный финансовый помощник.
                
                Я помогу тебе следить за расходами и доходами прямо здесь, в Telegram.
                
                Что я умею:
                ✅ Записывать расходы и доходы (просто напиши, например: 'обед 350')
                📸 Распознавать чеки по фото
                📊 Показывать сводку трат
                🎯 Следить за финансовыми целями
                💡 Давать советы по экономии
                
                Выбери действие ниже или просто начни вводить траты!
                """;
        var message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(welcomeText);

        // Create Inline Keyboard
        var markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        var row1 = new ArrayList<InlineKeyboardButton>();
        row1.add(createButton("💰 Баланс", "menu_balance"));
        row1.add(createButton("📊 Сводка", "menu_summary"));

        var row2 = new ArrayList<InlineKeyboardButton>();
        row2.add(createButton("🎯 Цели", "menu_goals"));
        row2.add(createButton("📋 Категории", "menu_categories"));

        var row3 = new ArrayList<InlineKeyboardButton>();
        row3.add(createButton("💡 Совет", "menu_tips"));

        rowsInline.add(row1);
        rowsInline.add(row2);
        rowsInline.add(row3);

        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending welcome message: {}", e.getMessage());
        }
    }

    private InlineKeyboardButton createButton(String text, String callbackData) {
        var button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

    private void sendConfirmationWithCancel(long chatId, budgetor.domain.Transaction tx, String text) {
        var message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        var markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        var row = new ArrayList<InlineKeyboardButton>();
        row.add(createButton("❌ Отмена", "tx_cancel:" + tx.getId()));
        rowsInline.add(row);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending confirmation message: {}", e.getMessage());
        }
    }

    private void handleTransactionCancel(CallbackQuery callbackQuery) {
        var callbackData = callbackQuery.getData();
        var txId = UUID.fromString(callbackData.split(":")[1]);
        var chatId = callbackQuery.getMessage().getChatId();
        var messageId = callbackQuery.getMessage().getMessageId();

        try {
            transactionService.deleteTransaction(txId);
            logger.info("Transaction cancelled and deleted: {}", txId);

            var editMessage = new EditMessageText();
            editMessage.setChatId(String.valueOf(chatId));
            editMessage.setMessageId(messageId);
            editMessage.setText("🗑️ Запись удалена");
            editMessage.setReplyMarkup(null);

            execute(editMessage);
        } catch (Exception e) {
            logger.error("Error cancelling transaction: {}", e.getMessage());
        }
    }

    private String formatSummary(SummaryDto summary) {
        var sb = new StringBuilder("📊 Сводка за этот месяц:\n\n");
        sb.append("Доходы: %.2f ₽\n".formatted(summary.totalIncome()));
        sb.append("Расходы: %.2f ₽\n\n".formatted(summary.totalExpenses()));
        
        if (!summary.expensesByCategory().isEmpty()) {
            sb.append("По категориям:\n");
            for (CategorySummaryDto cat : summary.expensesByCategory()) {
                sb.append("• %s: %.2f ₽\n".formatted(cat.categoryName(), cat.totalAmount()));
            }
        }
        return sb.toString();
    }

    private String formatGoals(List<GoalProgressDto> goals) {
        if (goals.isEmpty()) {
            return "🎯 Твои финансовые цели: пока не заданы.";
        }
        var sb = new StringBuilder("🎯 Твои финансовые цели:\n\n");
        for (GoalProgressDto goal : goals) {
            sb.append("• %s: %.2f / %.2f (%.2f%%)\n".formatted(
                    goal.name(), 
                    goal.currentAmount(), 
                    goal.targetAmount(), 
                    goal.progressPercentage()));
        }
        return sb.toString();
    }

    private String formatCategories(List<budgetor.domain.Category> categories) {
        if (categories.isEmpty()) {
            return "📋 Список категорий пуст.";
        }
        var sb = new StringBuilder("📋 Доступные категории:\n\n");
        for (budgetor.domain.Category cat : categories) {
            sb.append("• %s (%s)\n".formatted(
                    cat.getName(), 
                    cat.getType() == budgetor.domain.TransactionType.EXPENSE ? "Расход" : "Доход"));
        }
        return sb.toString();
    }
}
