package io.tchepannou.kribi.services;

import io.tchepannou.kribi.model.Account;

import java.io.IOException;

public interface AccountRepository {
    Account findAccount();

    void update(Account account) throws IOException;
}
