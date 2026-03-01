package budgetor.dto;

import budgetor.domain.TransactionType;

public record CategoryDto(String name, TransactionType type) {
}
