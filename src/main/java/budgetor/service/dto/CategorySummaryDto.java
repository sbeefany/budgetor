package budgetor.service.dto;

import java.math.BigDecimal;

public record CategorySummaryDto(String categoryName, BigDecimal totalAmount) {
}
