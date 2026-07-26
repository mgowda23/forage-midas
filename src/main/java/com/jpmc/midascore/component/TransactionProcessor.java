package com.jpmc.midascore.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;

@Component
public class TransactionProcessor {
    private static final Logger logger = LoggerFactory.getLogger(TransactionProcessor.class);
    private static final String INCENTIVE_URL = "http://localhost:8080/incentive";
    private final DatabaseConduit databaseConduit;
    private final RestTemplate restTemplate;

    public TransactionProcessor(DatabaseConduit databaseConduit) {
        this.databaseConduit = databaseConduit;
        this.restTemplate = new RestTemplate();
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

        Incentive incentive = restTemplate.postForObject(INCENTIVE_URL, transaction, Incentive.class);
        float incentiveAmount = incentive != null ? incentive.getAmount() : 0.0f;

        sender.setBalance(sender.getBalance() - transaction.getAmount());
        recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentiveAmount);

        databaseConduit.save(sender);
        databaseConduit.save(recipient);

        TransactionRecord transactionRecord = new TransactionRecord(sender, recipient, transaction.getAmount(), incentiveAmount);
        databaseConduit.save(transactionRecord);

        logger.info("Transaction processed successfully: {}", transactionRecord);
        
    }
}
