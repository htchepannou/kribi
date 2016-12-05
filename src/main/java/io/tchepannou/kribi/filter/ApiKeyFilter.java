package io.tchepannou.kribi.filter;

import io.tchepannou.kribi.model.Account;
import io.tchepannou.kribi.services.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ApiKeyFilter implements Filter {
    public static final String API_KEY_HEADER = "api_key";
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeyFilter.class);

    AccountRepository accountRepository;

    public ApiKeyFilter(AccountRepository accountRepository){
        this.accountRepository = accountRepository;
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(
            final ServletRequest request,
            final ServletResponse response,
            final FilterChain filterChain
    ) throws IOException, ServletException {
        doFilter((HttpServletRequest) request, (HttpServletResponse) response, filterChain);
    }

    private void doFilter(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws IOException, ServletException {
        final String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null) {
            LOGGER.error("No API key found in the request header");
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        final Account account = accountRepository.findAccount();
        if (!account.apiKeyMatches(apiKey)) {
            LOGGER.error("Invalid API key");
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        LOGGER.info("Authenticated");
        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}
