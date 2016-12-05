package io.tchepannou.kribi.services;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TransactionIdGeneratorTest {

    @Test
    public void testGet() throws Exception {
        assertThat(new TransactionIdGenerator().get()).isNotEmpty();
    }
}
