package budgetor.service;

import budgetor.domain.GoalType;
import budgetor.dto.GoalProgressDto;
import budgetor.service.dto.CategorySummaryDto;
import budgetor.service.dto.SummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TipServiceImplTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private SummaryService summaryService;

    @Mock
    private GoalService goalService;

    @Mock
    private TransactionService transactionService;

    private TipServiceImpl tipService;

    @BeforeEach
    void setUp() {
        Resource systemPrompt = new ByteArrayResource("Ты финансовый консультант.".getBytes(StandardCharsets.UTF_8));
        Resource userPrompt = new ByteArrayResource(
                "Текущий баланс: {currentBalance}\nДоходы за месяц: {totalIncome}\nРасходы за месяц: {totalExpenses}\nТраты по категориям:\n{categories}\nЦели:\n{goals}"
                        .getBytes(StandardCharsets.UTF_8));
        tipService = new TipServiceImpl(chatClient, summaryService, goalService, transactionService, systemPrompt,
                userPrompt);
    }

    @Test
    void generateTip_shouldFormatDataAndCallAiClient() throws Exception {
        // Given
        var currentBalance = new BigDecimal("45000.00");
        when(transactionService.getCurrentBalance()).thenReturn(currentBalance);

        var summary = new SummaryDto(
                new BigDecimal("50000.00"),
                new BigDecimal("5000.00"),
                List.of(new CategorySummaryDto("Еда", new BigDecimal("5000.00"))));
        when(summaryService.getSummary(any(), any())).thenReturn(summary);

        var goalProgress = new GoalProgressDto(
                UUID.randomUUID(),
                "Отпуск",
                GoalType.SAVINGS,
                new BigDecimal("100000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("10.00"));
        when(goalService.getAllGoalProgresses()).thenReturn(List.of(goalProgress));

        var generatedTip = "Тратьте меньше на еду!";
        when(chatClient.call(any(Prompt.class))).thenReturn(new org.springframework.ai.chat.ChatResponse(
                List.of(new org.springframework.ai.chat.Generation(generatedTip))));

        // When
        var result = tipService.generateTip();

        // Then
        assertThat(result).isEqualTo(generatedTip);
        verify(chatClient).call(any(Prompt.class));
    }

    @Test
    void generateTip_whenAiFails_shouldReturnFallbackTip() throws Exception {
        // Given
        when(transactionService.getCurrentBalance()).thenReturn(BigDecimal.ZERO);
        when(summaryService.getSummary(any(), any())).thenReturn(new SummaryDto(BigDecimal.ZERO, BigDecimal.ZERO, List.of()));
        when(goalService.getAllGoalProgresses()).thenReturn(List.of());
        when(chatClient.call(any(Prompt.class))).thenThrow(new RuntimeException("AI is down"));

        // When
        var result = tipService.generateTip();

        // Then
        assertThat(result).isEqualTo("Помните: регулярность важнее суммы! Откладывайте хотя бы 10% от любого дохода.");
    }
}
