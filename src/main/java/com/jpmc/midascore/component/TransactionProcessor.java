package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TransactionProcessor {
    private static final Logger logger = LoggerFactory.getLogger(TransactionProcessor.class);
    private final DatabaseConduit databaseConduit;

    public TransactionProcessor(DatabaseConduit databaseConduit) {
        this.databaseConduit = databaseConduit;
    }

    public void process(Transaction transaction) {
        UserRecord sender = databaseConduit.getUserById(transaction.getSenderId());
        UserRecord recipient = databaseConduit.getUserById(transaction.getRecipientId());

        if (sender == null || recipient == null) {
            logger.warn("Transaction discarded: Sender or recipient not found. Transaction: {}", transaction);
            return;
        }

        if (sender.getBalance() < transaction.getAmount()) {
            logger.warn("Transaction discarded: Insufficient balance. Sender: {}, Transaction: {}", sender, transaction);
            return;
        }

        // Process transaction
        sender.setBalance(sender.getBalance() - transaction.getAmount());
        recipient.setBalance(recipient.getBalance() + transaction.getAmount());

        databaseConduit.save(sender);
        databaseConduit.save(recipient);

        TransactionRecord transactionRecord = new TransactionRecord(sender, recipient, transaction.getAmount());
        databaseConduit.save(transactionRecord);

        logger.info("Transaction processed successfully: {}", transactionRecord);
    }
}
