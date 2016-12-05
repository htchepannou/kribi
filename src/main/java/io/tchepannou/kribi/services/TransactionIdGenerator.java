package io.tchepannou.kribi.services;

import java.util.UUID;

public class TransactionIdGenerator {
    private String id = UUID.randomUUID().toString();

    public String get(){
        return id;
    }
}
