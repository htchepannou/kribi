package io.tchepannou.kribi.config;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.PropertiesFileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class AwsConfiguration {
    @Autowired
    Environment env;

    //-- Beans
    @Bean
    AmazonS3 amazonS3() {
        final AmazonS3Client s3 = new AmazonS3Client(awsCredentialsProvider());
        return s3;
    }

    @Bean
    AWSCredentialsProvider awsCredentialsProvider() {
        if (env.acceptsProfiles("dev")){
            final String home = System.getProperty("user.home");
            return new PropertiesFileCredentialsProvider(home + "/.aws/credentials");
        } else {
            return new DefaultAWSCredentialsProviderChain();
        }
    }

}
