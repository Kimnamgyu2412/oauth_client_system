package oauth.client.system.oauth_client_system.email.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oauth.client.system.oauth_client_system.email.config.AwsSettingConfig;
import oauth.client.system.oauth_client_system.email.dto.EmailRequest;
import oauth.client.system.oauth_client_system.email.oauth.OAuthClientService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import javax.validation.constraints.Email;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewEmailService {

    private final SqsClient sqsClient;
    private final SesClient sesClient;
    private final AwsSettingConfig awsConfig;
    @Autowired
    private OAuthClientService oAuthClientService;

    /**
     *  메일 전송(동기방식)
     * @param emailContent
     * @return SendEmailResponse
     */
    public SendEmailResponse sendMail(Object emailContent) {
        return sendSesEmail(emailContent);
    }

    /**
     * 메일 전송(비동기 방식)
     * @param emailContent
     * @return SendEmailResponse
     */
    @Async
    public SendEmailResponse sendMailAsync(Object emailContent) {
        return sendSesEmail(emailContent);
    }

    /**
     * SQS 이메일 전송
     * @param emailContent
     */

    @SqsListener(value = "intergrated-sqs") // 큐 이름
    public void sendSQSMail(Object emailContent) {
        sendSesEmail(emailContent);
    }

    /**
     * 메일 대량 발송
     * @param emailContent
     */
    @Async
    public void sendMailBulk(Object emailContent) {
        ObjectMapper objectMapper = new ObjectMapper();
            // messageBody를 List<EmailRequest>로 변환
        List<EmailRequest> emailRequestList = convertToEmailRequestList(emailContent);

        if(CollectionUtils.isEmpty(emailRequestList)) {
            log.error("Email Data Null");
        }
        ExecutorService executor = Executors.newFixedThreadPool(3);
        try{
            for(EmailRequest emailRequest : emailRequestList){
                executor.submit(() -> {
                    try {
                        String msgBody = objectMapper.writeValueAsString(emailRequest);
                        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                                .queueUrl(awsConfig.getSqs().getQueueUrl())
                                .messageBody(msgBody)
                                .build();

                        sqsClient.sendMessage(sendMessageRequest);
                    } catch (JsonProcessingException e) {
                        log.error("SQS Queue Save Fail {}",e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e){
            log.error("SQS Queue Save Fail {}",e.getMessage());
            e.printStackTrace();
        } finally{

            executor.shutdown();

            try {
                //60초 대기후
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }

    private SendEmailResponse sendSesEmail(Object emailContent) {

        SendEmailResponse emailResponse;
        EmailRequest emailRequest = convertToEmailRequest(emailContent);
        // 수신자 이메일 주소 설정
        Destination destination = Destination.builder().toAddresses(emailRequest.getReceiverEmail()).build();
        // HTML 본문 설정
        Content content = Content.builder().data(emailRequest.getBody()).charset("UTF-8").build();
        // 제목 설정
        Content sub = Content.builder().data(emailRequest.getSubject()) .charset("UTF-8").build();
        // 본문 메시지 설정 (HTML 형식)
        Body body = Body.builder().html(content) .build();

        // 메시지 설정
        Message msg = Message.builder()
                .subject(sub) // 제목
                .body(body) // 본문
                .build();

        // 발신자 이름을 Base64로 인코딩하여 설정
        String encodedSenderName = Base64.getEncoder().encodeToString(emailRequest.getSenderName().getBytes(StandardCharsets.UTF_8));
        String source = "=?UTF-8?B?" + encodedSenderName + "?= <" + emailRequest.getSenderEmail() + ">";

        // SendEmailRequest 생성
        SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
                .destination(destination) // 수신자
                .configurationSetName(awsConfig.getSes().getConfigSet()) // SES 설정
                .message(msg) // 메시지 내용
                .source(source) // 발신자 (Base64 인코딩된 이름 포함)
                .build();

        log.info("이메일 발송 요청합니다.");
        try {
            emailResponse = sesClient.sendEmail(sendEmailRequest);
            //예제 데이터
//            Email email = Email.builder()
//                    .senderEmail("nick1961@micehub.com")
//                    .senderName("김남규")
//                    .personIdx(0L)
//                    .personGubun("customer")
//                    .personEmail("nick1961@micehub.com")
//                    .title(emailRequest.getSubject())
//                    .body(emailRequest.getBody())
//                    .campaignIdx(0l)
//                    .build();
            //oAuthClientService.callApiWithToken("/api/email/save", HttpMethod.POST, email,String.class);
        } catch (SesException e) {
            log.error("이메일 발송실패 {}", e.awsErrorDetails().errorMessage());
            throw e;
        }
        log.info("이메일발송이 완료되었습니다.");
        return emailResponse;
    }

    private static EmailRequest convertToEmailRequest(Object emailContent){
        Class<?> clazz = emailContent.getClass();
        ObjectMapper objectMapper = new ObjectMapper();
        EmailRequest emailRequest = null;
        try {
            if (clazz.equals(Map.class) || clazz.equals(HashMap.class)) {
                 emailRequest = objectMapper.convertValue(emailContent,EmailRequest.class);
            } else if (clazz.equals(String.class)) {
                if("OBJECT".equals(checkJsonType(emailContent))) {
                    emailRequest = objectMapper.readValue(String.valueOf(emailContent),EmailRequest.class);
                }
            }
        } catch (JsonProcessingException e) {
            log.error("JSON 처리 오류");
        } catch (Exception e){
            e.printStackTrace();
        }
        return emailRequest;
    }

    private static List<EmailRequest> convertToEmailRequestList(Object emailContent){
        Class<?> clazz = emailContent.getClass();
        ObjectMapper objectMapper = new ObjectMapper();
        List<EmailRequest> emailRequestList = null;
        try {
            if (clazz.equals(ArrayList.class)) {
                emailRequestList = objectMapper.convertValue(emailContent, new TypeReference<List<EmailRequest>>() {});
            } else if (clazz.equals(String.class)) {
                if("ARRAY".equals(checkJsonType(emailContent))) {
                    emailRequestList = objectMapper.readValue(String.valueOf(emailContent), new TypeReference<List<EmailRequest>>() {});
                }
            }
        } catch (JsonProcessingException e) {
            log.error("JSON 처리 오류");
        } catch (Exception e){
            e.printStackTrace();
        }
        return emailRequestList;
    }

    private static String checkJsonType(Object object) {
        // Object가 String 타입인지 확인
        if (object instanceof String) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                // JSON 문자열로 변환을 시도
                JsonNode jsonNode = objectMapper.readTree((String) object);
                // JSON 객체인지 배열인지 구분
                if (jsonNode.isObject()) {
                    return "OBJECT";  // JSON 객체
                } else if (jsonNode.isArray()) {
                    return "ARRAY";   // JSON 배열
                }
            } catch (JsonProcessingException e) {
                // JSON 형식이 아니면 에러를 처리
                return "INVALID";  // 잘못된 JSON 형식
            }
        }
        return "INVALID";
    }

}

