package budgetor.bot;

import budgetor.domain.Transaction;
import budgetor.service.TransactionService;
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
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import budgetor.domain.TransactionType;
import budgetor.domain.Category;
import budgetor.repository.CategoryRepository;
import budgetor.repository.GoalRepository;
import budgetor.domain.Goal;
import budgetor.domain.GoalType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private GoalRepository goalRepository;
    
    @Autowired
    private TransactionService transactionService;

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

    @Test
    void integration_menuBalance_returnsActualBalance() throws Exception {
        // Given: Set initial balance
        TelegramBot botSpy = spy(telegramBot);
        doReturn(null).when(botSpy).execute(any(SendMessage.class));
        
        // Actually, just save some transactions via service
        transactionService.createTransaction(new BigDecimal("100.00"), "Еда", "Обед", TransactionType.EXPENSE);
        transactionService.createTransaction(new BigDecimal("1000.00"), "Зарплата", "ЗП", TransactionType.INCOME);
        
        // When: Ask for balance
        Update update = createCallbackUpdate("menu_balance", 12345L);
        botSpy.onUpdateReceived(update);
        
        // Then: balance = 1000 - 100 = 900
        verify(botSpy).execute(argThat((SendMessage msg) -> 
                msg.getText().contains("баланс:") && msg.getText().contains("900")
        ));
    }

    @Test
    void integration_menuSummary_returnsFormattedSummary() throws Exception {
        // Given
        transactionService.createTransaction(new BigDecimal("250.00"), "Еда", "Ужин", TransactionType.EXPENSE);
        
        // When
        Update update = createCallbackUpdate("menu_summary", 12345L);
        TelegramBot botSpy = spy(telegramBot);
        doReturn(null).when(botSpy).execute(any(SendMessage.class));
        
        botSpy.onUpdateReceived(update);
        
        // Then
        verify(botSpy).execute(argThat((SendMessage msg) -> 
                msg.getText().contains("Сводка за этот месяц") &&
                msg.getText().contains("Еда:") &&
                msg.getText().contains("250")
        ));
    }

    @Test
    void integration_menuGoals_returnsGoalProgress() throws Exception {
        // Given
        Goal goal = new Goal(GoalType.SAVINGS, null, new BigDecimal("10000.00"), 
                LocalDateTime.now(), LocalDateTime.now().plusMonths(1), "Коплю на тату");
        goalRepository.save(goal);
        
        // When
        Update update = createCallbackUpdate("menu_goals", 12345L);
        TelegramBot botSpy = spy(telegramBot);
        doReturn(null).when(botSpy).execute(any(SendMessage.class));
        
        botSpy.onUpdateReceived(update);
        
        // Then
        verify(botSpy).execute(argThat((SendMessage msg) -> 
                msg.getText().contains("Коплю на тату") &&
                msg.getText().contains("10000")
        ));
    }

    @Test
    void integration_menuCategories_listsAllCategories() throws Exception {
        // Given
        categoryRepository.save(new Category("Спорт", TransactionType.EXPENSE, true));
        
        // When
        Update update = createCallbackUpdate("menu_categories", 12345L);
        TelegramBot botSpy = spy(telegramBot);
        doReturn(null).when(botSpy).execute(any(SendMessage.class));
        
        botSpy.onUpdateReceived(update);
        
        // Then
        verify(botSpy).execute(argThat((SendMessage msg) -> 
                msg.getText().contains("Спорт")
        ));
    }

    @Test
    void integration_menuTips_callsAiAndReturnsTip() throws Exception {
        // Given
        String tipText = "Совет: кушай дома!";
        ChatResponse chatResponse = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        when(chatClient.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(new AssistantMessage(tipText));
        
        // When
        Update update = createCallbackUpdate("menu_tips", 12345L);
        TelegramBot botSpy = spy(telegramBot);
        doReturn(null).when(botSpy).execute(any(SendMessage.class));
        
        botSpy.onUpdateReceived(update);
        
        // Then
        verify(botSpy).execute(argThat((SendMessage msg) -> 
                msg.getText().equals(tipText)
        ));
    }

    private Update createCallbackUpdate(String data, long chatId) {
        Update update = mock(Update.class);
        CallbackQuery query = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        when(update.hasCallbackQuery()).thenReturn(true);
        when(update.getCallbackQuery()).thenReturn(query);
        when(query.getData()).thenReturn(data);
        when(query.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(chatId);
        return update;
    }
}
