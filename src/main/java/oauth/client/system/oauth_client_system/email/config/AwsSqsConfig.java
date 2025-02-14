package oauth.client.system.oauth_client_system.email.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory;
import org.springframework.cloud.aws.messaging.config.annotation.EnableSqs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
@EnableSqs
public class AwsSqsConfig {

    private final AwsSettingConfig awsConfig;

    @Value("${cloud.aws.region.static}")
    private String region;
    // AWS SDK v1: AmazonSQSAsync 클라이언트
    @Primary  // AmazonSQSAsync를 기본 빈으로 설정
    @Bean
    public AmazonSQSAsync amazonSQSAsync() {
        BasicAWSCredentials awsBasicCredentials = new BasicAWSCredentials(
                awsConfig.getCredentials().getAccessKey(),
                awsConfig.getCredentials().getSecretKey()
        );

        return AmazonSQSAsyncClientBuilder.standard()
                .withEndpointConfiguration(new AmazonSQSAsyncClientBuilder.EndpointConfiguration(awsConfig.getSqs().getQueueUrl(), region))
                .withCredentials(new AWSStaticCredentialsProvider(awsBasicCredentials))
                .build();
    }

    // AWS SDK v2: SqsClient
    @Bean
    public SqsClient sqsClient() {
        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(
                awsConfig.getCredentials().getAccessKey(),
                awsConfig.getCredentials().getSecretKey()
        );

        return SqsClient.builder()
                .endpointOverride(URI.create(awsConfig.getSqs().getQueueUrl()))
                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                .region(Region.of(region))
                .build();
    }

    // SimpleMessageListenerContainerFactory: AmazonSQSAsync 사용
    @Bean
    public SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory(AmazonSQSAsync amazonSQSAsync) {
        SimpleMessageListenerContainerFactory factory = new SimpleMessageListenerContainerFactory();
        factory.setAmazonSqs(amazonSQSAsync);  // AmazonSQSAsync 클라이언트를 전달
        factory.setWaitTimeOut(10);
        return factory;
    }
}
