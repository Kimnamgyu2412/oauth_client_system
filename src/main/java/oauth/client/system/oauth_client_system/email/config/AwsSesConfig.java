package oauth.client.system.oauth_client_system.email.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
@RequiredArgsConstructor
public class AwsSesConfig {

    private final AwsSettingConfig awsConfig;

    @Value("${cloud.aws.region.static}")
    private String region;
    // AWS SDK v1: AmazonSQSAsync 클라이언트
    @Bean
    public SesClient sesClient() {
        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(
                awsConfig.getCredentials().getAccessKey(), // AWS Access Key
                awsConfig.getCredentials().getSecretKey()  // AWS Secret Key
        );

        return SesClient.builder()
                .region(Region.of(region)) // SES 리전
                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                .build();
    }
}
