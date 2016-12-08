package io.tchepannou.kribi.api;

import io.tchepannou.kribi.model.Account;
import io.tchepannou.kribi.model.KeyPair;
import io.tchepannou.kribi.services.AccountRepository;
import io.tchepannou.kribi.services.StorageService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AccountControllerTest {
    @Mock
    StorageService storageService;

    @Mock
    AccountRepository accountRepository;

    @InjectMocks
    AccountController controller;

    @Mock
    Account account;


    @Test
    public void testGenerateApiKey() throws Exception {
        // Given
        final String key = "--key--";
        final KeyPair keyPair = new KeyPair("publish", key);
        when(account.generateApiKeyPair()).thenReturn(keyPair);

        when(accountRepository.findAccount()).thenReturn(account);

        // When
        final String result = controller.generateApiKey();

        // Then
        assertThat(result).isEqualTo(keyPair.getPublicKey());

        verify(account).setApiKey(keyPair.getPrivateKey());
        verify(accountRepository).update(account);
    }
}
