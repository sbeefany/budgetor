package budgetor;

import budgetor.domain.Category;
import budgetor.domain.Transaction;
import budgetor.service.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

@Component
@Profile("local-cli") // Only active if the 'local-cli' profile is enabled, to avoid breaking Telegram mode
public class LocalTerminalRunner implements CommandLineRunner {

    private final TransactionService transactionService;
    private final SummaryService summaryService;
    private final GoalService goalService;
    private final CategoryService categoryService;
    private final TipService tipService;
    private final TransactionParser transactionParser;

    public LocalTerminalRunner(TransactionService transactionService,
                               SummaryService summaryService,
                               GoalService goalService,
                               CategoryService categoryService,
                               TipService tipService,
                               TransactionParser transactionParser) {
        this.transactionService = transactionService;
        this.summaryService = summaryService;
        this.goalService = goalService;
        this.categoryService = categoryService;
        this.tipService = tipService;
        this.transactionParser = transactionParser;
    }

    @Override
    public void run(String... args) {
        System.out.println("=================================================");
        System.out.println(" Budgetor Local CLI Started!");
        System.out.println(" Type 'exit' to quit, '/help' for commands.");
        System.out.println("=================================================");

        try (var scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\nYou: ");
                var input = scanner.nextLine();

                if ("exit".equalsIgnoreCase(input.trim())) {
                    System.out.println("Exiting Local CLI.");
                    break;
                }

                if (input.isBlank()) {
                    continue;
                }

                String command = input.trim().toLowerCase();
                try {
                    switch (command) {
                        case "/help":
                            System.out.println("Available commands: /balance, /summary, /goals, /categories, /tips, /help");
                            System.out.println("Or just type a standard transaction like 'обед 350'");
                            break;
                        case "/balance":
                            System.out.println("Balance: " + transactionService.getCurrentBalance());
                            break;
                        case "/summary":
                            LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
                            LocalDateTime now = LocalDateTime.now();
                            var summary = summaryService.getSummary(startOfMonth, now);
                            System.out.println("Summary (this month):");
                            System.out.println("  Total Income: " + summary.totalIncome());
                            System.out.println("  Total Expense: " + summary.totalExpenses());
                            summary.expensesByCategory().forEach(cs -> 
                                System.out.println("  - " + cs.categoryName() + ": " + cs.totalAmount())
                            );
                            break;
                        case "/goals":
                            var goals = goalService.getAllGoalProgresses();
                            if (goals.isEmpty()) {
                                System.out.println("No goals found.");
                            } else {
                                goals.forEach(g -> System.out.println(g.name() + " (" + g.type() + "): " + g.currentAmount() + " / " + g.targetAmount() + " (" + g.progressPercentage() + "%)"));
                            }
                            break;
                        case "/categories":
                            var categories = categoryService.getAllCategories();
                            categories.forEach(c -> System.out.println("- " + c.getName() + " [" + c.getType() + "]"));
                            break;
                        case "/tips":
                            System.out.println("Budgetor AI: ...thinking...");
                            System.out.println("Tip: " + tipService.generateTip());
                            break;
                        default:
                            System.out.println("Budgetor AI: ...parsing transaction...");
                            List<String> availableCategories = categoryService.getAllCategories().stream()
                                    .map(Category::getName)
                                    .collect(Collectors.toList());
                            var parsed = transactionParser.parse(input, availableCategories);
                            Transaction tx = transactionService.createTransaction(parsed.amount(), parsed.categoryName(), parsed.description(), parsed.type());
                            System.out.println("Saved " + tx.getType() + ": " + tx.getAmount() + " in category '" + tx.getCategory().getName() + "'");
                            break;
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
    }
}
