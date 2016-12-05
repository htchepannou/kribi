package io.tchepannou.kribi.filter;

import io.tchepannou.kribi.services.TransactionIdGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MDCFilterTest {
    @Mock
    TransactionIdGenerator transactionIdGenerator;

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @Mock
    FilterChain chain;

    @InjectMocks
    MDCFilter filter;


    @Test
    public void testDoFilter() throws Exception {
        final String uuid = "test";
        when(transactionIdGenerator.get()).thenReturn(uuid);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
