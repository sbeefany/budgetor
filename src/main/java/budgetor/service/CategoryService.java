package budgetor.service;

import budgetor.domain.Category;
import budgetor.domain.TransactionType;

public interface CategoryService {

    void initializeDefaults();

    Category getOrCreateCategory(String name, TransactionType type);

    java.util.Optional<Category> getCategoryByName(String name);
}
