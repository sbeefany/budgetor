package budgetor.service;

import budgetor.domain.Category;
import budgetor.domain.TransactionType;
import budgetor.dto.CategoryDto;
import budgetor.repository.CategoryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;
    private final Resource categoriesResource;

    public CategoryServiceImpl(
            CategoryRepository categoryRepository,
            ObjectMapper objectMapper,
            @Value("classpath:categories.json") Resource categoriesResource) {
        this.categoryRepository = categoryRepository;
        this.objectMapper = objectMapper;
        this.categoriesResource = categoriesResource;
    }

    @Override
    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initializeDefaults() {
        try (var inputStream = categoriesResource.getInputStream()) {
            var defaultCategories = objectMapper.readValue(inputStream, new TypeReference<List<CategoryDto>>() {
            });
            for (var dto : defaultCategories) {
                categoryRepository.findByNameIgnoreCase(dto.name()).ifPresentOrElse(
                        existing -> log.debug("Category '{}' already exists, skipping.", dto.name()),
                        () -> {
                            var newCategory = new Category(dto.name(), dto.type(), true);
                            categoryRepository.save(newCategory);
                            log.info("Initialized default category: {}", dto.name());
                        });
            }
        } catch (IOException e) {
            log.error("Failed to load default categories from categories.json", e);
        }
    }

    @Override
    @Transactional
    public Category getOrCreateCategory(String name, TransactionType type) {
        return categoryRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    var category = new Category(name, type, false);
                    log.info("Creating new user-defined category: {}", name);
                    return categoryRepository.save(category);
                });
    }
}
