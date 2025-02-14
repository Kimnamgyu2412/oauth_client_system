package oauth.client.system.oauth_client_system.email.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AWS 관련 설정값 매핑 클래스
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "cloud.aws")
public class AwsSettingConfig {

    private Sqs sqs;      // SQS 관련 설정
    private Ses ses;      // SES 관련 설정
    private Sns sns;      // SNS 관련 설정
    private Credentials credentials;      // SNS 관련 설정

    @Getter
    @Setter
    public static class Sqs {
        private String queueUrl;  // SQS 큐 URL
    }

    @Getter
    @Setter
    public static class Ses {
        private String region;  // SES 리전
        private String configSet;  // 구성 세트
    }

    @Getter
    @Setter
    public static class Sns {
        private String topicArn;  // SNS Topic ARN
    }

    @Getter
    @Setter
    public static class Credentials {
        private String accessKey;  // accessKey
        private String secretKey;  // secretKey
    }
}

