package com.bytemovr.prod.aws;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsConfig {

    @Bean
    public AmazonS3 amazonS3() {
        // Uses instance/task role + default region
        return AmazonS3ClientBuilder.defaultClient();
    }

    @Bean
    public AmazonDynamoDB amazonDynamoDB() {
        return AmazonDynamoDBClientBuilder.defaultClient();
    }

    @Bean
    public DynamoDB dynamoDB(AmazonDynamoDB ddb) {
        return new DynamoDB(ddb);
    }
}
