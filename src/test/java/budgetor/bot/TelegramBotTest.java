package budgetor.bot;

import budgetor.domain.Category;
import budgetor.domain.Transaction;
import budgetor.domain.TransactionType;
import budgetor.dto.ParsedTransactionDto;
import budgetor.service.CategoryService;
import budgetor.service.TransactionParser;
import budgetor.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TelegramBotTest {

    private TelegramBot telegramBot;
    private BotConfig config;
    private TransactionService transactionService;
    private CategoryService categoryService;
    private TransactionParser transactionParser;

    @BeforeEach
    void setUp() {
        config = mock(BotConfig.class);
        transactionService = mock(TransactionService.class);
        categoryService = mock(CategoryService.class);
        transactionParser = mock(TransactionParser.class);
        
        when(config.getToken()).thenReturn("test-token");
        when(config.getUsername()).thenReturn("test-bot");
        
        telegramBot = spy(new TelegramBot(config, transactionService, categoryService, transactionParser));
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
        
        assertThat(sentMessage.getReplyMarkup()).isInstanceOf(InlineKeyboardMarkup.class);
        InlineKeyboardMarkup markup = (InlineKeyboardMarkup) sentMessage.getReplyMarkup();
        List<List<InlineKeyboardButton>> keyboard = markup.getKeyboard();
        
        assertThat(keyboard).hasSize(3);
        assertThat(keyboard.get(0)).extracting(InlineKeyboardButton::getCallbackData)
                .containsExactly("menu_balance", "menu_summary");
        assertThat(keyboard.get(1)).extracting(InlineKeyboardButton::getCallbackData)
                .containsExactly("menu_goals", "menu_categories");
        assertThat(keyboard.get(2)).extracting(InlineKeyboardButton::getCallbackData)
                .containsExactly("menu_tips");
    }

    @Test
    void onUpdateReceived_withCallbackQuery_sendsProperResponse() throws TelegramApiException {
        // Given
        Update update = mock(Update.class);
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);

        when(update.hasCallbackQuery()).thenReturn(true);
        when(update.getCallbackQuery()).thenReturn(callbackQuery);
        when(callbackQuery.getData()).thenReturn("menu_balance");
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(12345L);

        doReturn(null).when(telegramBot).execute(any(SendMessage.class));

        // When
        telegramBot.onUpdateReceived(update);

        // Then
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot).execute(captor.capture());

        SendMessage sentMessage = captor.getValue();
        assertThat(sentMessage.getChatId()).isEqualTo("12345");
        assertThat(sentMessage.getText()).contains("баланс");
    }

    @Test
    void onUpdateReceived_withTransactionText_parsesAndSaves() throws TelegramApiException {
        // Given
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("обед 350");
        when(message.getChatId()).thenReturn(12345L);

        Category category = new Category();
        category.setName("Еда");
        category.setType(TransactionType.EXPENSE);
        
        Transaction tx = new Transaction();
        tx.setAmount(new BigDecimal("350"));
        tx.setCategory(category);
        tx.setType(TransactionType.EXPENSE);
        tx.setDescription("обед");

        when(categoryService.getAllCategories()).thenReturn(Collections.singletonList(category));
        when(transactionParser.parse(anyString(), anyList())).thenReturn(new ParsedTransactionDto(new BigDecimal("350"), "Еда", "обед", TransactionType.EXPENSE));
        when(transactionService.createTransaction(any(), any(), any(), any())).thenReturn(tx);

        doReturn(null).when(telegramBot).execute(any(SendMessage.class));

        // When
        telegramBot.onUpdateReceived(update);

        // Then
        verify(transactionParser).parse(eq("обед 350"), anyList());
        verify(transactionService).createTransaction(eq(new BigDecimal("350")), eq("Еда"), eq("обед"), eq(TransactionType.EXPENSE));
        
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("Записано");
        assertThat(captor.getValue().getText()).contains("350");
    }

    @Test
    void onUpdateReceived_withParsingError_sendsErrorMessage() throws TelegramApiException {
        // Given
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("invalid input");
        when(message.getChatId()).thenReturn(12345L);

        when(transactionParser.parse(anyString(), anyList())).thenThrow(new RuntimeException("AI error"));

        doReturn(null).when(telegramBot).execute(any(SendMessage.class));

        // When
        telegramBot.onUpdateReceived(update);

        // Then
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("Извини, не удалось распознать трату");
    }
}
