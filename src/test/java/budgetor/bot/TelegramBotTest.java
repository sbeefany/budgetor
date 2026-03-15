package budgetor.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TelegramBotTest {

    private TelegramBot telegramBot;
    private BotConfig config;

    @BeforeEach
    void setUp() {
        config = mock(BotConfig.class);
        when(config.getToken()).thenReturn("test-token");
        when(config.getUsername()).thenReturn("test-bot");
        telegramBot = spy(new TelegramBot(config));
    }

    @Test
    void onUpdateReceived_withStartCommand_sendsWelcomeMessage() throws TelegramApiException {
        // Given
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/start");
        when(message.getChatId()).thenReturn(12345L);

        doReturn(null).when(telegramBot).execute(any(SendMessage.class));

        // When
        telegramBot.onUpdateReceived(update);

        // Then
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot).execute(captor.capture());
        
        SendMessage sentMessage = captor.getValue();
        assertThat(sentMessage.getChatId()).isEqualTo("12345");
        assertThat(sentMessage.getText()).contains("Привет! Я Budgetor");
        assertThat(sentMessage.getText()).contains("Распознавать чеки по фото");
    }

    @Test
    void onUpdateReceived_withUnknownCommand_doesNothing() throws TelegramApiException {
        // Given
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/unknown");

        // When
        telegramBot.onUpdateReceived(update);

        // Then
        verify(telegramBot, never()).execute(any(SendMessage.class));
    }
}
