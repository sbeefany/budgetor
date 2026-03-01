package budgetor.service;

import budgetor.domain.Balance;
import budgetor.domain.Category;
import budgetor.domain.Transaction;
import budgetor.domain.TransactionType;
import budgetor.repository.BalanceRepository;
import budgetor.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private CategoryService categoryService;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    @Captor
    private ArgumentCaptor<Balance> balanceCaptor;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl(transactionRepository, balanceRepository, categoryService);
    }

    @Test
    void shouldCreateExpenseTransactionAndDecreaseBalance() {
        // Given
        var amount = new BigDecimal("150.00");
        var categoryName = "Еда";
        var description = "Обед";
        var type = TransactionType.EXPENSE;

        var category = new Category(categoryName, type, true);
        var initialBalance = new Balance(new BigDecimal("1000.00"));

        when(categoryService.getOrCreateCategory(categoryName, type)).thenReturn(category);
        when(balanceRepository.findById(1L)).thenReturn(Optional.of(initialBalance));

        // When
        transactionService.createTransaction(amount, categoryName, description, type);

        // Then
        verify(transactionRepository).save(transactionCaptor.capture());
        verify(balanceRepository).save(balanceCaptor.capture());

        var savedTransaction = transactionCaptor.getValue();
        assertThat(savedTransaction.getAmount()).isEqualTo(amount);
        assertThat(savedTransaction.getDescription()).isEqualTo(description);
        assertThat(savedTransaction.getCategory()).isEqualTo(category);
        assertThat(savedTransaction.getType()).isEqualTo(type);
        assertThat(savedTransaction.getTransactionDate()).isNotNull();

        var savedBalance = balanceCaptor.getValue();
        assertThat(savedBalance.getAmount()).isEqualTo(new BigDecimal("850.00"));
    }

    @Test
    void shouldCreateIncomeTransactionAndIncreaseBalance() {
        // Given
        var amount = new BigDecimal("50000.00");
        var categoryName = "Зарплата";
        var description = "Аванс";
        var type = TransactionType.INCOME;

        var category = new Category(categoryName, type, true);
        var initialBalance = new Balance(new BigDecimal("1000.00"));

        when(categoryService.getOrCreateCategory(categoryName, type)).thenReturn(category);
        when(balanceRepository.findById(1L)).thenReturn(Optional.of(initialBalance));

        // When
        transactionService.createTransaction(amount, categoryName, description, type);

        // Then
        verify(transactionRepository).save(transactionCaptor.capture());
        verify(balanceRepository).save(balanceCaptor.capture());

        var savedTransaction = transactionCaptor.getValue();
        assertThat(savedTransaction.getAmount()).isEqualTo(amount);
        assertThat(savedTransaction.getType()).isEqualTo(type);

        var savedBalance = balanceCaptor.getValue();
        assertThat(savedBalance.getAmount()).isEqualTo(new BigDecimal("51000.00"));
    }

    @Test
    void shouldReturnCurrentBalance() {
        // Given
        var expectedAmount = new BigDecimal("1234.56");
        var balance = new Balance(expectedAmount);
        when(balanceRepository.findById(1L)).thenReturn(Optional.of(balance));

        // When
        var currentBalance = transactionService.getCurrentBalance();

        // Then
        assertThat(currentBalance).isEqualTo(expectedAmount);
    }

    @Test
    void shouldReturnZeroIfBalanceNotFound() {
        // Given
        when(balanceRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        var currentBalance = transactionService.getCurrentBalance();

        // Then
        assertThat(currentBalance).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void shouldSetInitialBalanceWhenPositiveDifference() {
        // Given
        var currentBalance = new Balance(new BigDecimal("1000.00"));
        var targetBalance = new BigDecimal("1500.00");

        when(balanceRepository.findById(1L)).thenReturn(Optional.of(currentBalance));
        var correctionCategory = new Category("Корректировка", TransactionType.INCOME, true);
        when(categoryService.getOrCreateCategory("Корректировка", TransactionType.INCOME))
                .thenReturn(correctionCategory);

        // When
        transactionService.setInitialBalance(targetBalance);

        // Then
        verify(transactionRepository).save(transactionCaptor.capture());
        verify(balanceRepository).save(balanceCaptor.capture());

        var savedTransaction = transactionCaptor.getValue();
        assertThat(savedTransaction.getAmount()).isEqualTo(new BigDecimal("500.00"));
        assertThat(savedTransaction.getType()).isEqualTo(TransactionType.INCOME);

        var savedBalance = balanceCaptor.getValue();
        assertThat(savedBalance.getAmount()).isEqualTo(targetBalance);
    }

    @Test
    void shouldSetInitialBalanceWhenNegativeDifference() {
        // Given
        var currentBalance = new Balance(new BigDecimal("1000.00"));
        var targetBalance = new BigDecimal("800.00");

        when(balanceRepository.findById(1L)).thenReturn(Optional.of(currentBalance));
        var correctionCategory = new Category("Корректировка", TransactionType.EXPENSE, true);
        when(categoryService.getOrCreateCategory("Корректировка", TransactionType.EXPENSE))
                .thenReturn(correctionCategory);

        // When
        transactionService.setInitialBalance(targetBalance);

        // Then
        verify(transactionRepository).save(transactionCaptor.capture());
        verify(balanceRepository).save(balanceCaptor.capture());

        var savedTransaction = transactionCaptor.getValue();
        assertThat(savedTransaction.getAmount()).isEqualTo(new BigDecimal("200.00"));
        assertThat(savedTransaction.getType()).isEqualTo(TransactionType.EXPENSE);

        var savedBalance = balanceCaptor.getValue();
        assertThat(savedBalance.getAmount()).isEqualTo(targetBalance);
    }

    @Test
    void shouldNotCreateTransactionWhenInitialBalanceMatchesCurrent() {
        // Given
        var currentBalance = new Balance(new BigDecimal("1000.00"));
        var targetBalance = new BigDecimal("1000.00");

        when(balanceRepository.findById(1L)).thenReturn(Optional.of(currentBalance));

        // When
        var result = transactionService.setInitialBalance(targetBalance);

        // Then
        verify(transactionRepository, never()).save(any());
        assertThat(result).isNull();
    }

    @Test
    void shouldDeleteExpenseTransactionAndRestoreBalance() {
        // Given
        var id = UUID.randomUUID();
        var category = new Category("Еда", TransactionType.EXPENSE, true);
        var transaction = new Transaction(new BigDecimal("100.00"), category, "Обед", LocalDateTime.now(),
                TransactionType.EXPENSE);
        var currentBalance = new Balance(new BigDecimal("900.00"));

        when(transactionRepository.findById(id)).thenReturn(Optional.of(transaction));
        when(balanceRepository.findById(1L)).thenReturn(Optional.of(currentBalance));

        // When
        transactionService.deleteTransaction(id);

        // Then
        verify(transactionRepository).delete(transaction);
        verify(balanceRepository).save(balanceCaptor.capture());

        var savedBalance = balanceCaptor.getValue();
        assertThat(savedBalance.getAmount()).isEqualTo(new BigDecimal("1000.00")); // Expense removed, balance goes up
    }

    @Test
    void shouldDeleteIncomeTransactionAndReduceBalance() {
        // Given
        var id = UUID.randomUUID();
        var category = new Category("Зарплата", TransactionType.INCOME, true);
        var transaction = new Transaction(new BigDecimal("5000.00"), category, "Аванс", LocalDateTime.now(),
                TransactionType.INCOME);
        var currentBalance = new Balance(new BigDecimal("5500.00"));

        when(transactionRepository.findById(id)).thenReturn(Optional.of(transaction));
        when(balanceRepository.findById(1L)).thenReturn(Optional.of(currentBalance));

        // When
        transactionService.deleteTransaction(id);

        // Then
        verify(transactionRepository).delete(transaction);
        verify(balanceRepository).save(balanceCaptor.capture());

        var savedBalance = balanceCaptor.getValue();
        assertThat(savedBalance.getAmount()).isEqualTo(new BigDecimal("500.00")); // Income removed, balance goes down
    }
}
