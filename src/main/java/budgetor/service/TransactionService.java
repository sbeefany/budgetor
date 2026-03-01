package budgetor.service;

import budgetor.domain.Transaction;
import budgetor.domain.TransactionType;

import java.math.BigDecimal;
import java.util.UUID;

public interface TransactionService {

    /**
     * Creates a new transaction (income or expense) and updates the balance.
     */
    Transaction createTransaction(BigDecimal amount, String categoryName, String description, TransactionType type);

    /**
     * Retrieves the current balance from the user_balance table.
     */
    BigDecimal getCurrentBalance();

    /**
     * Sets or adjusts the initial balance to a target value by creating a
     * corrective transaction.
     */
    Transaction setInitialBalance(BigDecimal targetBalance);

    /**
     * Deletes a transaction by its ID and reverses its effect on the balance.
     */
    void deleteTransaction(UUID transactionId);
}
