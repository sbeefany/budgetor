package budgetor.service;

import budgetor.domain.Balance;
import budgetor.domain.Transaction;
import budgetor.domain.TransactionType;
import budgetor.repository.BalanceRepository;
import budgetor.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final BalanceRepository balanceRepository;
    private final CategoryService categoryService;

    public TransactionServiceImpl(
            TransactionRepository transactionRepository,
            BalanceRepository balanceRepository,
            CategoryService categoryService) {
        this.transactionRepository = transactionRepository;
        this.balanceRepository = balanceRepository;
        this.categoryService = categoryService;
    }

    @Override
    @Transactional
    public Transaction createTransaction(BigDecimal amount, String categoryName, String description,
            TransactionType type) {
        var category = categoryService.getOrCreateCategory(categoryName, type);
        var transaction = new Transaction(amount, category, description, LocalDateTime.now(), type);

        transactionRepository.save(transaction);
        updateBalance(amount, type, true);

        return transaction;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCurrentBalance() {
        return balanceRepository.findById(1L)
                .map(Balance::getAmount)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public Transaction setInitialBalance(BigDecimal targetBalance) {
        var currentBalance = getCurrentBalance();
        var difference = targetBalance.subtract(currentBalance);

        if (difference.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        var type = difference.compareTo(BigDecimal.ZERO) > 0 ? TransactionType.INCOME : TransactionType.EXPENSE;
        var amount = difference.abs();

        var category = categoryService.getOrCreateCategory("Корректировка", type);
        var transaction = new Transaction(amount, category, "Ручная корректировка баланса", LocalDateTime.now(), type);

        transactionRepository.save(transaction);

        var balanceOptional = balanceRepository.findById(1L);
        Balance balance;
        if (balanceOptional.isPresent()) {
            balance = balanceOptional.get();
            balance.setAmount(targetBalance);
        } else {
            balance = new Balance(targetBalance);
        }
        balanceRepository.save(balance);

        return transaction;
    }

    @Override
    @Transactional
    public void deleteTransaction(UUID transactionId) {
        transactionRepository.findById(transactionId).ifPresent(transaction -> {
            updateBalance(transaction.getAmount(), transaction.getType(), false);
            transactionRepository.delete(transaction);
        });
    }

    private void updateBalance(BigDecimal amount, TransactionType type, boolean isAdded) {
        var balance = balanceRepository.findById(1L).orElse(new Balance(BigDecimal.ZERO));

        var currentAmount = balance.getAmount();

        // If adding a new transaction
        if (isAdded) {
            if (type == TransactionType.INCOME) {
                balance.setAmount(currentAmount.add(amount));
            } else {
                balance.setAmount(currentAmount.subtract(amount));
            }
            // If deleting a transaction, reverse the logic
        } else {
            if (type == TransactionType.INCOME) {
                balance.setAmount(currentAmount.subtract(amount));
            } else {
                balance.setAmount(currentAmount.add(amount));
            }
        }

        balanceRepository.save(balance);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalSpentCategory(budgetor.domain.Category category, java.time.LocalDateTime start,
            java.time.LocalDateTime end, TransactionType type) {
        return transactionRepository.findByCategoryAndTransactionDateBetweenAndType(category, start, end, type)
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
