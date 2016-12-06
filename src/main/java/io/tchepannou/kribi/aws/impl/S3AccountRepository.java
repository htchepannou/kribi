package io.tchepannou.kribi.aws.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.tchepannou.kribi.model.Account;
import io.tchepannou.kribi.services.AccountRepository;
import io.tchepannou.kribi.services.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class S3AccountRepository implements AccountRepository {
    public static final String ACCOUNT_FILE = "settings/account.json";
    public static final String PRIVATE_KEY_FILE = "settings/key.pem";

    @Autowired
    Jackson2ObjectMapperBuilder objectMapperBuilder;

    @Autowired
    private StorageService storageService;

    private Account account;

    @PostConstruct
    public void init() throws IOException {
        account = loadAccount();
        account.getKeyPair().setName(account.getName());
        account.getKeyPair().setPrivateKey(loadPrivateKey());
    }

    @Override
    public Account findAccount() {
        return account;
    }

    @Override
    public void update(final Account account) throws IOException {
        final String json = objectMapperBuilder.build().writeValueAsString(account);
        storageService.put(ACCOUNT_FILE, new ByteArrayInputStream(json.getBytes()));
        this.account = account;
    }

    //-- Private
    private Account loadAccount() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        storageService.get(ACCOUNT_FILE, out);

        final ObjectMapper mapper = objectMapperBuilder.build();
        return mapper.readValue(out.toByteArray(), Account.class);
    }

    private String loadPrivateKey() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        storageService.get(PRIVATE_KEY_FILE, out);

        return new String(out.toByteArray());
    }
}
