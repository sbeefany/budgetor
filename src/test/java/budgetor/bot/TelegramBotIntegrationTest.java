package budgetor.bot;

import budgetor.domain.Transaction;
import budgetor.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TelegramBotIntegrationTest {

    @Autowired
    private TelegramBot telegramBot;

    @Autowired
    private TransactionRepository transactionRepository;

    @MockBean
    private ChatClient chatClient; // Mock external LLM

    @Test
    void integration_sendTextMessage_parsesAndSavesToDb() throws Exception {
        // Given
        String rawInput = "ужин 800";
        String aiJsonOutput = "{\"amount\":800.00, \"categoryName\":\"Еда\", \"description\":\"ужин\", \"type\":\"EXPENSE\"}";
        
        // Mock AI response
        ChatResponse chatResponse = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        when(chatClient.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(new AssistantMessage(aiJsonOutput));

        // Mock Telegram Message
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn(rawInput);
        when(message.getChatId()).thenReturn(999L);

        // Spy on bot to capture sent messages without actually calling Telegram API
        TelegramBot botSpy = spy(telegramBot);
        doReturn(null).when(botSpy).execute(any(SendMessage.class));

        // When
        botSpy.onUpdateReceived(update);

        // Then
        // 1. Verify DB contains the transaction
        List<Transaction> transactions = transactionRepository.findAll();
        assertThat(transactions).hasSize(1);
        Transaction savedTx = transactions.get(0);
        assertThat(savedTx.getAmount()).isEqualByComparingTo("800");
        assertThat(savedTx.getCategory().getName()).isEqualTo("Еда");

        // 2. Verify bot "sent" confirmation
        verify(botSpy).execute(argThat((SendMessage msg) -> 
                msg.getText().contains("Записано") && 
                msg.getText().contains("800")
        ));
    }
}
