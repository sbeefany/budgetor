package budgetor.service;

import budgetor.dto.GoalProgressDto;
import org.springframework.ai.chat.ChatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TipServiceImpl implements TipService {

    private static final Logger log = LoggerFactory.getLogger(TipServiceImpl.class);

    private final ChatClient chatClient;
    private final SummaryService summaryService;
    private final GoalService goalService;
    private final TransactionService transactionService;
    private final Resource systemPromptResource;
    private final Resource userPromptResource;

    public TipServiceImpl(ChatClient chatClient,
            SummaryService summaryService,
            GoalService goalService,
            TransactionService transactionService,
            @Value("classpath:/prompts/tip-system-prompt.st") Resource systemPromptResource,
            @Value("classpath:/prompts/tip-user-prompt.st") Resource userPromptResource) {
        this.chatClient = chatClient;
        this.summaryService = summaryService;
        this.goalService = goalService;
        this.transactionService = transactionService;
        this.systemPromptResource = systemPromptResource;
        this.userPromptResource = userPromptResource;
    }

    @Override
    public String generateTip() {
        try {
            var currentBalance = transactionService.getCurrentBalance();

            var now = LocalDateTime.now();
            var startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0);
            var endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59);

            var summary = summaryService.getSummary(startOfMonth, endOfMonth);
            var goals = goalService.getAllGoalProgresses();

            String promptTemplateText = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
            var systemPrompt = new SystemMessage(promptTemplateText);

            var userPromptTemplate = new PromptTemplate(userPromptResource);
            var userMessage = userPromptTemplate.createMessage(Map.of(
                    "currentBalance", currentBalance,
                    "totalIncome", summary.totalIncome(),
                    "totalExpenses", summary.totalExpenses(),
                    "categories", formatCategories(summary.expensesByCategory()),
                    "goals", formatGoals(goals)));

            var prompt = new Prompt(List.of(systemPrompt, userMessage));

            var response = chatClient.call(prompt);
            return response.getResult().getOutput().getContent();
        } catch (Exception e) {
            log.error("Failed to generate financial tip using Spring AI", e);
            return "Помните: регулярность важнее суммы! Откладывайте хотя бы 10% от любого дохода.";
        }
    }

    private String formatCategories(List<budgetor.service.dto.CategorySummaryDto> categories) {
        if (categories == null || categories.isEmpty())
            return "Нет данных";
        return categories.stream()
                .map(cat -> "- " + cat.categoryName() + ": " + cat.totalAmount())
                .collect(Collectors.joining("\n"));
    }

    private String formatGoals(List<GoalProgressDto> goals) {
        if (goals == null || goals.isEmpty())
            return "Нет данных";
        return goals.stream()
                .map(goal -> "- " + goal.name() + ": Накоплено " + goal.currentAmount() + " из " + goal.targetAmount()
                        + " (" + goal.progressPercentage() + "%)")
                .collect(Collectors.joining("\n"));
    }
}
