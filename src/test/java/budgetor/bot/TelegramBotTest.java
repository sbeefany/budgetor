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
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.springframework.core.io.Resource;
import java.util.UUID;

import java.io.ByteArrayInputStream;
import org.jeasy.random.EasyRandom;
import java.math.BigDecimal;
import java.util.Collections;

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
        var update = createUpdateWithText(12345L, "/start");
        doReturn(null).when(telegramBot).execute(any(SendMessage.class));

        // When
        telegramBot.onUpdateReceived(update);

        // Then
        var captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot).execute(captor.capture());
        
        var sentMessage = captor.getValue();
        assertThat(sentMessage.getChatId()).isEqualTo("12345");
        assertThat(sentMessage.getText()).contains("Привет! Я Budgetor");
        
        assertThat(sentMessage.getReplyMarkup()).isInstanceOf(InlineKeyboardMarkup.class);
        var markup = (InlineKeyboardMarkup) sentMessage.getReplyMarkup();
        var keyboard = markup.getKeyboard();
        
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
        var update = createUpdateWithCallback(12345L, "menu_balance", 999);
        doReturn(null).when(telegramBot).execute(any(SendMessage.class));

        // When
        telegramBot.onUpdateReceived(update);

        // Then
        var captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot).execute(captor.capture());

        var sentMessage = captor.getValue();
        assertThat(sentMessage.getChatId()).isEqualTo("12345");
        assertThat(sentMessage.getText()).contains("баланс");
    }

    @Test
    void onUpdateReceived_withTransactionText_parsesAndSaves() throws TelegramApiException {
        // Given
        var update = createUpdateWithText(12345L, "обед 350");
        var category = mockCategory("Еда", TransactionType.EXPENSE);
        var tx = mockTransaction(new BigDecimal("350"), category, "обед", TransactionType.EXPENSE);

        // We can use random values or override specific ones if needed for verification
        var expectedDto = new ParsedTransactionDto(new BigDecimal("350"), "Еда", "обед", TransactionType.EXPENSE);

        when(categoryService.getAllCategories()).thenReturn(Collections.singletonList(category));
        when(transactionParser.parse(anyString(), anyList())).thenReturn(expectedDto);
        when(transactionService.createTransaction(any(), any(), any(), any())).thenReturn(tx);

        doReturn(null).when(telegramBot).execute(any(SendMessage.class));

        // When
        telegramBot.onUpdateReceived(update);

        // Then
        verify(transactionParser).parse(eq("обед 350"), anyList());
        verify(transactionService).createTransaction(eq(new BigDecimal("350")), eq("Еда"), eq("обед"), eq(TransactionType.EXPENSE));
        
        var captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("Записано");
        assertThat(captor.getValue().getText()).contains("350");
    }

    @Test
    void onUpdateReceived_withParsingError_sendsErrorMessage() throws TelegramApiException {
        // Given
        var update = createUpdateWithText(12345L, "invalid input");
        when(transactionParser.parse(anyString(), anyList())).thenThrow(new RuntimeException("AI error"));
        doReturn(null).when(telegramBot).execute(any(SendMessage.class));

        // When
        telegramBot.onUpdateReceived(update);

        // Then
        var captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("Извини, не удалось распознать трату");
    }

    @Test
    void onUpdateReceived_withPhoto_parsesAndSaves() throws TelegramApiException {
        // Given
        var update = createUpdateWithPhoto(12345L, "file-id");
        var category = mockCategory("Еда", TransactionType.EXPENSE);
        var tx = mockTransaction(new BigDecimal("100"), category, "чек", TransactionType.EXPENSE);

        var expectedDto = new ParsedTransactionDto(new BigDecimal("100"), "Еда", "чек", TransactionType.EXPENSE);

        var file = new File();
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
        
        var captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("Записано");
        assertThat(captor.getValue().getText()).contains("100");
    }

    @Test
    void onUpdateReceived_withTransaction_includesCancelButton() throws TelegramApiException {
        // Given
        var update = createUpdateWithText(12345L, "обед 350");
        var category = mockCategory("Еда", TransactionType.EXPENSE);
        var tx = mockTransaction(new BigDecimal("350"), category, "обед", TransactionType.EXPENSE);
        tx.setId(UUID.randomUUID());

        when(transactionParser.parse(anyString(), anyList())).thenReturn(new ParsedTransactionDto(tx.getAmount(), category.getName(), "обед", tx.getType()));
        when(transactionService.createTransaction(any(), any(), any(), any())).thenReturn(tx);

        doReturn(null).when(telegramBot).execute(any(SendMessage.class));

        // When
        telegramBot.onUpdateReceived(update);

        // Then
        var captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot).execute(captor.capture());
        var sentMessage = captor.getValue();

        assertThat(sentMessage.getReplyMarkup()).isInstanceOf(InlineKeyboardMarkup.class);
        var markup = (InlineKeyboardMarkup) sentMessage.getReplyMarkup();
        var lastRow = markup.getKeyboard().get(markup.getKeyboard().size() - 1);
        
        assertThat(lastRow).hasSize(1);
        assertThat(lastRow.get(0).getText()).contains("Отмена");
        assertThat(lastRow.get(0).getCallbackData()).isEqualTo("tx_cancel:" + tx.getId());
    }

    @Test
    void onUpdateReceived_withCancelCallback_deletesTransactionAndUpdatesMessage() throws TelegramApiException {
        // Given
        var txId = UUID.randomUUID();
        var update = createUpdateWithCallback(12345L, "tx_cancel:" + txId, 999);
        doReturn(null).when(telegramBot).execute(any(EditMessageText.class));

        // When
        telegramBot.onUpdateReceived(update);

        // Then
        verify(transactionService).deleteTransaction(txId);
        
        var captor = ArgumentCaptor.forClass(EditMessageText.class);
        verify(telegramBot).execute(captor.capture());
        var editedMessage = captor.getValue();
        
        assertThat(editedMessage.getChatId()).isEqualTo("12345");
        assertThat(editedMessage.getMessageId()).isEqualTo(999);
        assertThat(editedMessage.getText()).contains("Запись удалена");
        assertThat(editedMessage.getReplyMarkup()).isNull();
    }

    private Update createUpdateWithText(long chatId, String text) {
        var update = new Update();
        var message = new Message();
        update.setMessage(message);
        message.setText(text);
        var chat = new org.telegram.telegrambots.meta.api.objects.Chat();
        chat.setId(chatId);
        message.setChat(chat);
        return update;
    }

    private Update createUpdateWithPhoto(long chatId, String fileId) {
        var update = new Update();
        var message = new Message();
        update.setMessage(message);
        var photoSize = new PhotoSize();
        photoSize.setFileId(fileId);
        message.setPhoto(Collections.singletonList(photoSize));
        var chat = new org.telegram.telegrambots.meta.api.objects.Chat();
        chat.setId(chatId);
        message.setChat(chat);
        return update;
    }

    private Update createUpdateWithCallback(long chatId, String data, int messageId) {
        var update = new Update();
        var callbackQuery = new CallbackQuery();
        update.setCallbackQuery(callbackQuery);
        callbackQuery.setData(data);
        var message = new Message();
        message.setMessageId(messageId);
        var chat = new org.telegram.telegrambots.meta.api.objects.Chat();
        chat.setId(chatId);
        message.setChat(chat);
        callbackQuery.setMessage(message);
        return update;
    }

    private Category mockCategory(String name, TransactionType type) {
        var category = easyRandom.nextObject(Category.class);
        category.setName(name);
        category.setType(type);
        return category;
    }

    private Transaction mockTransaction(BigDecimal amount, Category category, String description, TransactionType type) {
        var tx = easyRandom.nextObject(Transaction.class);
        tx.setAmount(amount);
        tx.setCategory(category);
        tx.setType(type);
        tx.setDescription(description);
        return tx;
    }
}
