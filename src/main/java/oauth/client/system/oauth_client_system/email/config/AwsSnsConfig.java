package oauth.client.system.oauth_client_system.email.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AwsSnsConfig {

    private final AwsSettingConfig awsConfig;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Bean
    public AmazonSNS amazonSnsClient() {
        BasicAWSCredentials basicAwsCredentials = new BasicAWSCredentials(awsConfig.getCredentials().getAccessKey(), awsConfig.getCredentials().getSecretKey());

        AmazonSNSClientBuilder builder = AmazonSNSClient.builder();

        return builder
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(basicAwsCredentials))
                .build();
    }

}
