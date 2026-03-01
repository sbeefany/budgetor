package budgetor.service.dto;

import java.math.BigDecimal;
import java.util.List;

public record SummaryDto(
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        List<CategorySummaryDto> expensesByCategory) {
}
