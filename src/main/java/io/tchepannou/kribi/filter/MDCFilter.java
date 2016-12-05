package io.tchepannou.kribi.filter;

import io.tchepannou.kribi.services.TransactionIdGenerator;
import org.slf4j.MDC;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class MDCFilter implements Filter{
    final TransactionIdGenerator transactionIdGenerator;

    public MDCFilter(final TransactionIdGenerator transactionIdGenerator) {
        this.transactionIdGenerator = transactionIdGenerator;
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain)
            throws IOException, ServletException {
        MDC.put("TransactionId", transactionIdGenerator.get());
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {

    }
}
