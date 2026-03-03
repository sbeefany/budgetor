package budgetor.service;

import java.util.Optional;

import budgetor.domain.Category;
import budgetor.domain.TransactionType;

public interface CategoryService {

    void initializeDefaults();

    Category getOrCreateCategory(String name, TransactionType type);

    Optional<Category> getCategoryByName(String name);
}
