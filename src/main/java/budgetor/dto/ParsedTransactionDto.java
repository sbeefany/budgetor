package budgetor.dto;

import budgetor.domain.TransactionType;
import java.math.BigDecimal;

public record ParsedTransactionDto(
        BigDecimal amount,
        String categoryName,
        String description,
        TransactionType type
) {
}
