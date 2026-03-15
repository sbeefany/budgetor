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
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import org.jeasy.random.EasyRandom;
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
    private EasyRandom easyRandom;

    @BeforeEach
    void setUp() {
        config = mock(BotConfig.class);
        transactionService = mock(TransactionService.class);
        categoryService = mock(CategoryService.class);
        transactionParser = mock(TransactionParser.class);
        
        easyRandom = new EasyRandom();
        telegramBot = spy(new TelegramBot(config, transactionService, categoryService, transactionParser));
    }

    @Test
    void onUpdateReceived_withStartCommand_sendsWelcomeMessage() throws TelegramApiException {
        // Given
        Update update = new Update();
        Message message = new Message();
        update.setMessage(message);
        message.setText("/start");
        org.telegram.telegrambots.meta.api.objects.Chat chat = new org.telegram.telegrambots.meta.api.objects.Chat();
        chat.setId(12345L);
        message.setChat(chat);

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
        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        Message message = new Message();
        
        update.setCallbackQuery(callbackQuery);
        callbackQuery.setData("menu_balance");
        callbackQuery.setMessage(message);
        org.telegram.telegrambots.meta.api.objects.Chat chat = new org.telegram.telegrambots.meta.api.objects.Chat();
        chat.setId(12345L);
        message.setChat(chat);

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
        Update update = new Update();
        Message message = new Message();
        update.setMessage(message);
        message.setText("обед 350");
        org.telegram.telegrambots.meta.api.objects.Chat chat = new org.telegram.telegrambots.meta.api.objects.Chat();
        chat.setId(12345L);
        message.setChat(chat);

        Category category = easyRandom.nextObject(Category.class);
        category.setName("Еда");
        category.setType(TransactionType.EXPENSE);
        
        Transaction tx = easyRandom.nextObject(Transaction.class);
        tx.setAmount(new BigDecimal("350"));
        tx.setCategory(category);
        tx.setType(TransactionType.EXPENSE);
        tx.setDescription("обед");

        // We can use random values or override specific ones if needed for verification
        ParsedTransactionDto expectedDto = new ParsedTransactionDto(new BigDecimal("350"), "Еда", "обед", TransactionType.EXPENSE);

        when(categoryService.getAllCategories()).thenReturn(Collections.singletonList(category));
        when(transactionParser.parse(anyString(), anyList())).thenReturn(expectedDto);
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
        Update update = new Update();
        Message message = new Message();
        update.setMessage(message);
        message.setText("invalid input");
        org.telegram.telegrambots.meta.api.objects.Chat chat = new org.telegram.telegrambots.meta.api.objects.Chat();
        chat.setId(12345L);
        message.setChat(chat);

        when(transactionParser.parse(anyString(), anyList())).thenThrow(new RuntimeException("AI error"));

        doReturn(null).when(telegramBot).execute(any(SendMessage.class));

        // When
        telegramBot.onUpdateReceived(update);

        // Then
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("Извини, не удалось распознать трату");
    }
    @Test
    void onUpdateReceived_withPhoto_parsesAndSaves() throws TelegramApiException {
        // Given
        Update update = new Update();
        Message message = new Message();
        PhotoSize photoSize = new PhotoSize();
        
        update.setMessage(message);
        message.setPhoto(Collections.singletonList(photoSize));
        photoSize.setFileId("file-id");
        org.telegram.telegrambots.meta.api.objects.Chat chat = new org.telegram.telegrambots.meta.api.objects.Chat();
        chat.setId(12345L);
        message.setChat(chat);

        Category category = easyRandom.nextObject(Category.class);
        category.setName("Еда");
        category.setType(TransactionType.EXPENSE);
        
        Transaction tx = easyRandom.nextObject(Transaction.class);
        tx.setAmount(new BigDecimal("100"));
        tx.setCategory(category);
        tx.setType(TransactionType.EXPENSE);
        tx.setDescription("чек");

        ParsedTransactionDto expectedDto = new ParsedTransactionDto(new BigDecimal("100"), "Еда", "чек", TransactionType.EXPENSE);

        File file = new File();
        file.setFilePath("path/to/file");
        
        doReturn(file).when(telegramBot).execute(any(GetFile.class));
        doReturn(new ByteArrayInputStream("test-image".getBytes())).when(telegramBot).downloadFileAsStream(any(File.class));

        when(categoryService.getAllCategories()).thenReturn(Collections.singletonList(category));
        when(transactionParser.parse(any(Resource.class), anyList())).thenReturn(expectedDto);
        when(transactionService.createTransaction(any(), any(), any(), any())).thenReturn(tx);

        doReturn(null).when(telegramBot).execute(any(SendMessage.class));

        // When
        telegramBot.onUpdateReceived(update);

        // Then
        verify(transactionParser).parse(any(Resource.class), anyList());
        verify(transactionService).createTransaction(eq(new BigDecimal("100")), eq("Еда"), eq("чек"), eq(TransactionType.EXPENSE));
        
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("Записано");
        assertThat(captor.getValue().getText()).contains("100");
    }
}
