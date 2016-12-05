package io.tchepannou.kribi.config;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsConfiguration {

    //-- Beans
    @Bean
    AmazonS3 amazonS3() {
        final AmazonS3Client s3 = new AmazonS3Client(awsCredentialsProvider());
        return s3;
    }

    @Bean
    AWSCredentialsProvider awsCredentialsProvider(){
        return new DefaultAWSCredentialsProviderChain();
    }
}
