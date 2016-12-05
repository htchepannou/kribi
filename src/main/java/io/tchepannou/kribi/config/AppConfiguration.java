package io.tchepannou.kribi.config;

import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import io.tchepannou.kribi.aws.AwsContextFactory;
import io.tchepannou.kribi.aws.impl.S3AccountRepository;
import io.tchepannou.kribi.aws.impl.S3StorageService;
import io.tchepannou.kribi.filter.ApiKeyFilter;
import io.tchepannou.kribi.filter.MDCFilter;
import io.tchepannou.kribi.healthcheck.S3HealthCheck;
import io.tchepannou.kribi.services.AccountRepository;
import io.tchepannou.kribi.services.StorageService;
import io.tchepannou.kribi.services.TransactionIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.Arrays;
import java.util.TimeZone;

@Configuration
public class AppConfiguration {
    //-- Spring
    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder()
                .simpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .timeZone(TimeZone.getTimeZone("GMT"))
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .featuresToDisable(
                        DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
                );
    }


    //-- Services
    @Bean
    @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    TransactionIdGenerator transactionIdGenerator(){
        return new TransactionIdGenerator();
    }

    @Bean
    AccountRepository accountRepository(){
        return new S3AccountRepository();
    }

    @Bean
    AwsContextFactory awsContextFactory(){
        return new AwsContextFactory();
    }

    @Bean
    StorageService storageService(){
        return new S3StorageService();
    }


    //-- Filters
    @Bean
    FilterRegistrationBean MdcFilter(){
        final MDCFilter filter = new MDCFilter(transactionIdGenerator());
        FilterRegistrationBean bean = new FilterRegistrationBean(filter);
        bean.setUrlPatterns(Arrays.asList("/v1/*"));

        return bean;
    }

    @Bean
    FilterRegistrationBean ApiKeyFilter(){
        final ApiKeyFilter filter = new ApiKeyFilter(accountRepository());
        FilterRegistrationBean bean = new FilterRegistrationBean(filter);
        bean.setUrlPatterns(Arrays.asList("/v1/*"));

        return bean;
    }


    //-- HealthCheck
    @Bean
    S3HealthCheck s3HealthCheck(
            @Value("${kribi.aws.bucket}") final String bucket,
            final AmazonS3 s3
    ){
        return new S3HealthCheck(bucket, s3);
    }
}
