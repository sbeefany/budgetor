package budgetor.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);
    private final BotConfig config;

    public TelegramBot(BotConfig config) {
        super(config.getToken());
        this.config = config;
    }

    @Override
    public String getBotUsername() {
        return config.getUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.equals("/start")) {
                sendWelcomeMessage(chatId);
            }
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
                
                Нажми на кнопку 'Меню' или начни вводить траты!
                """;
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(welcomeText);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending welcome message: {}", e.getMessage());
        }
    }
}
