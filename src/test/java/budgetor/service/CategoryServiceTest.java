package budgetor.service;

import budgetor.domain.Category;
import budgetor.domain.TransactionType;
import budgetor.dto.CategoryDto;
import budgetor.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Resource categoriesResource;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(categoryRepository, objectMapper, categoriesResource);
    }

    @Test
    void initializeDefaults_shouldLoadCategoriesFromJsonAndSaveTodb() throws IOException {
        // Given
        var json = "[{\"name\":\"Food\",\"type\":\"EXPENSE\"}]";
        var is = new ByteArrayInputStream(json.getBytes());
        when(categoriesResource.getInputStream()).thenReturn(is);

        var dto = new CategoryDto("Food", TransactionType.EXPENSE);
        when(objectMapper.readValue(any(java.io.InputStream.class),
                org.mockito.ArgumentMatchers.<TypeReference<java.util.List<CategoryDto>>>any()))
                .thenReturn(java.util.List.of(dto));

        when(categoryRepository.findByNameIgnoreCase("Food")).thenReturn(Optional.empty());

        // When
        categoryService.initializeDefaults();

        // Then
        verify(categoryRepository, times(1)).save(argThat(cat -> cat.getName().equals("Food")
                && cat.getType() == budgetor.domain.TransactionType.EXPENSE && cat.isDefault()));
    }

    @Test
    void getOrCreateCategory_shouldReturnExistingCategoryIgnoringCase() {
        // Given
        var existingCategory = new Category("Transport", budgetor.domain.TransactionType.EXPENSE, true);
        when(categoryRepository.findByNameIgnoreCase("transport")).thenReturn(Optional.of(existingCategory));

        // When
        var result = categoryService.getOrCreateCategory("transport", budgetor.domain.TransactionType.EXPENSE);

        // Then
        assertThat(result).isSameAs(existingCategory);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void getOrCreateCategory_shouldCreateAndSaveNewCategoryWhenNotFound() {
        // Given
        when(categoryRepository.findByNameIgnoreCase("Coffee")).thenReturn(Optional.empty());
        var savedCategory = new Category("Coffee", budgetor.domain.TransactionType.EXPENSE, false);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        // When
        var result = categoryService.getOrCreateCategory("Coffee", budgetor.domain.TransactionType.EXPENSE);

        // Then
        assertThat(result).isSameAs(savedCategory);
        verify(categoryRepository, times(1)).save(argThat(cat ->
                cat.getName().equals("Coffee") && cat.getType() == TransactionType.EXPENSE && !cat.isDefault()
        ));
    }

    @Test
    void getAllCategories_shouldReturnAllCategories() {
        // Given
        var cat1 = new Category("Food", TransactionType.EXPENSE, true);
        var cat2 = new Category("Salary", TransactionType.INCOME, true);
        when(categoryRepository.findAll()).thenReturn(java.util.List.of(cat1, cat2));

        // When
        var result = categoryService.getAllCategories();

        // Then
        assertThat(result).hasSize(2).containsExactlyInAnyOrder(cat1, cat2);
    }
}
