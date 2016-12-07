package io.tchepannou.kribi.api;

import io.tchepannou.kribi.model.Account;
import io.tchepannou.kribi.model.KeyPair;
import io.tchepannou.kribi.services.AccountRepository;
import io.tchepannou.kribi.services.StorageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/v1/account")
@Api(value = "Account Manager API")
public class AccountController {

    @Autowired
    StorageService storageService;

    @Autowired
    AccountRepository accountRepository;

    @RequestMapping(value = "/generate_api_key", method = RequestMethod.GET)
    @ApiOperation(value = "Generate account's API key")
    public String generateApiKey() throws IOException {
        final Account account = accountRepository.findAccount();
        KeyPair keyPair = account.generateApiKeyPair();
        account.setApiKey(keyPair.getPrivateKey());
        accountRepository.update(account);

        return keyPair.getPublicKey();
    }
}
