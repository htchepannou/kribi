package io.tchepannou.kribi.filter;

import io.tchepannou.kribi.model.Account;
import io.tchepannou.kribi.services.AccountRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApiKeyFilterTest {
    @Mock
    AccountRepository accountRepository;

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @Mock
    FilterChain chain;

    @Mock
    Account account;

    @InjectMocks
    ApiKeyFilter filter;

    @Before
    public void setUp (){
        when(accountRepository.findAccount()).thenReturn(account);
    }

    @Test
    public void shouldReturnForbiddenIfNoApiKeyProvided() throws Exception {
        // Given
        when(request.getHeader(ApiKeyFilter.API_KEY_HEADER)).thenReturn(null);

        // When
        filter.doFilter(request, response, chain);

        // Then
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    public void shouldReturnForbiddenIfApiKeyMismatch() throws Exception {
        // Given
        when(request.getHeader(ApiKeyFilter.API_KEY_HEADER)).thenReturn("123");
        when(account.apiKeyMatches("123")).thenReturn(false);

        // When
        filter.doFilter(request, response, chain);

        // Then
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    public void shouldProcessRequestIfApiKeyMatch() throws Exception {
        // Given
        when(request.getHeader(ApiKeyFilter.API_KEY_HEADER)).thenReturn("123");
        when(account.apiKeyMatches("123")).thenReturn(true);

        // When
        filter.doFilter(request, response, chain);

        // Then
        verify(chain).doFilter(request, response);
    }
}
