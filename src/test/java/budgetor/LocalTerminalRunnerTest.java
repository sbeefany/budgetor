package budgetor;

import budgetor.domain.TransactionType;
import budgetor.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test") // We need 'test' for H2 DB, but NOT 'local-cli' to prevent auto-running
@Transactional // Rolls back DB changes after each test automatically
class LocalTerminalRunnerTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private SummaryService summaryService;

    @Autowired
    private GoalService goalService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TipService tipService;

    @Autowired
    private TransactionParser transactionParser;

    @MockBean
    private ChatClient chatClient; // Mocking ONLY external LLM provider

    @Mock
    private ChatResponse chatResponse;

    @Mock
    private Generation generation;

    private LocalTerminalRunner runner;

    private final InputStream originalIn = System.in;
    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream outContent;

    @BeforeEach
    void setUp() {
        // Manually instantiate the runner to avoid blocking the Spring context startup
        runner = new LocalTerminalRunner(transactionService, summaryService, goalService,
                categoryService, tipService, transactionParser);

        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    private void provideInput(String data) {
        System.setIn(new ByteArrayInputStream(data.getBytes()));
    }

    @Test
    void run_shouldExitOnExitCommand() {
        provideInput("exit\n");

        runner.run();

        assertThat(outContent.toString()).contains("Exiting Local CLI.");
    }

    @Test
    void run_shouldShowHelpCommand() {
        provideInput("/help\nexit\n");

        runner.run();

        assertThat(outContent.toString()).contains("Available commands: /balance, /summary, /goals, /categories, /tips, /help");
    }

    @Test
    void run_shouldShowBalance() {
        // Given a transaction in the DB to have some balance
        transactionService.createTransaction(new BigDecimal("1500.00"), "Зарплата", "Аванс", TransactionType.INCOME);
        provideInput("/balance\nexit\n");

        runner.run();

        assertThat(outContent.toString()).contains("Balance: 1500.00");
    }

    @Test
    void run_shouldShowGoals() {
        // Given a goal in the DB
        goalService.createSavingsGoal(new BigDecimal("100000"), java.time.LocalDateTime.now().plusMonths(1), "iPhone");
        provideInput("/goals\nexit\n");

        runner.run();

        assertThat(outContent.toString()).contains("iPhone (SAVINGS): 0.00 / 100000 (0.00%)");
    }

    @Test
    void run_shouldShowCategories() {
        provideInput("/categories\nexit\n");

        runner.run();

        assertThat(outContent.toString()).contains("- Транспорт [EXPENSE]");
    }

    @Test
    void run_shouldShowTips() {
        // Given we mock the external AI behavior for tips
        when(chatClient.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(new AssistantMessage("Avoid spending too much on coffee."));

        provideInput("/tips\nexit\n");

        runner.run();

        assertThat(outContent.toString()).contains("Tip: Avoid spending too much on coffee.");
    }

    @Test
    void run_shouldParseTransaction() {
        // Given we mock the external AI parser to simulate returning a structured JSON
        String aiJsonOutput = "{\"amount\":350.00, \"categoryName\":\"Еда\", \"description\":\"Обед с коллегами\", \"type\":\"EXPENSE\"}";
        when(chatClient.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(new AssistantMessage(aiJsonOutput));

        provideInput("обед 350\nexit\n");

        runner.run();

        assertThat(outContent.toString()).contains("Saved EXPENSE: 350.00 in category 'Еда'");
        
        // Assert it was actually saved in DB
        var balance = transactionService.getCurrentBalance();
        assertThat(balance).isEqualTo(new BigDecimal("-350.00")); // Because we spent 350
    }
}
