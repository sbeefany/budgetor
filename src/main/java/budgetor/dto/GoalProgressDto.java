package budgetor.dto;

import budgetor.domain.GoalType;

import java.math.BigDecimal;
import java.util.UUID;

public record GoalProgressDto(
        UUID goalId,
        String name,
        GoalType type,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        BigDecimal progressPercentage) {
}
