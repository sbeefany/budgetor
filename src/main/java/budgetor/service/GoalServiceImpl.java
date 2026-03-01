package budgetor.service;

import budgetor.domain.Category;
import budgetor.domain.Goal;
import budgetor.domain.GoalType;
import budgetor.domain.TransactionType;
import budgetor.dto.GoalProgressDto;
import budgetor.repository.GoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class GoalServiceImpl implements GoalService {

    private final GoalRepository goalRepository;
    private final TransactionService transactionService;
    private final CategoryService categoryService;

    public GoalServiceImpl(GoalRepository goalRepository, TransactionService transactionService,
            CategoryService categoryService) {
        this.goalRepository = goalRepository;
        this.transactionService = transactionService;
        this.categoryService = categoryService;
    }

    @Override
    @Transactional
    public Goal createSavingsGoal(BigDecimal targetAmount, LocalDateTime targetDate, String name) {
        var goal = new Goal(GoalType.SAVINGS, null, targetAmount, LocalDateTime.now(), targetDate, name);
        return goalRepository.save(goal);
    }

    @Override
    @Transactional
    public Goal createBudgetGoal(String categoryName, BigDecimal targetAmount, LocalDateTime startDate,
            LocalDateTime endDate) {
        var category = categoryService.getCategoryByName(categoryName)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryName));

        var name = "Бюджет: " + category.getName();
        var goal = new Goal(GoalType.BUDGET, category, targetAmount, startDate, endDate, name);

        return goalRepository.save(goal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalProgressDto> getAllGoalProgresses() {
        return goalRepository.findAll().stream()
                .map(this::computeProgress)
                .toList();
    }

    private GoalProgressDto computeProgress(Goal goal) {
        BigDecimal currentAmount;

        if (goal.getTargetType() == GoalType.SAVINGS) {
            currentAmount = transactionService.getCurrentBalance();
        } else {
            // BUDGET goal focuses on specific category expenses
            currentAmount = transactionService.calculateTotalSpentCategory(
                    goal.getCategory(),
                    goal.getStartDate(),
                    goal.getEndDate(),
                    TransactionType.EXPENSE);
        }

        BigDecimal progressPercentage;
        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) == 0) {
            progressPercentage = BigDecimal.ZERO;
        } else {
            progressPercentage = currentAmount.divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new GoalProgressDto(
                goal.getId(),
                goal.getName(),
                goal.getTargetType(),
                goal.getTargetAmount(),
                currentAmount,
                progressPercentage);
    }
}
