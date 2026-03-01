package budgetor.service;

import budgetor.domain.Goal;
import budgetor.dto.GoalProgressDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface GoalService {

    /**
     * Creates a savings goal.
     */
    Goal createSavingsGoal(BigDecimal targetAmount, LocalDateTime targetDate, String name);

    /**
     * Creates a budget goal (spending limit).
     */
    Goal createBudgetGoal(String categoryName, BigDecimal targetAmount, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Retrieves the progress of all goals.
     */
    List<GoalProgressDto> getAllGoalProgresses();
}
