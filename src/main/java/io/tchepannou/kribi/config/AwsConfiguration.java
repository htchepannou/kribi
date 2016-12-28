package io.tchepannou.kribi.config;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.PropertiesFileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.transfer.TransferManager;
import io.tchepannou.kribi.aws.AwsContext;
import io.tchepannou.kribi.aws.JavaAppDeployer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
public class AwsConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsConfiguration.class);
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
        if (env.acceptsProfiles("dev")) {
            final String home = System.getProperty("user.home");
            return new PropertiesFileCredentialsProvider(home + "/.aws/credentials");
        } else {
            return new DefaultAWSCredentialsProviderChain();
        }
    }

    @Bean(destroyMethod = "shutdownNow")
    TransferManager transferManager() {
        return new TransferManager(awsCredentialsProvider());
    }

    //-- Cron
    @Scheduled(cron = "0 0/30 * * * ?")
    public void vaccum(){
        for (Regions region : Regions.values()){
            try {
                AwsContext ctx = new AwsContext(awsCredentialsProvider(), region.getName());
                new JavaAppDeployer(ctx).vacuum();
            } catch (Exception e){
                LOGGER.error("Unexcepted error when running the vaccum on {}", region.getName(), e);
            }
        }
    }
}
