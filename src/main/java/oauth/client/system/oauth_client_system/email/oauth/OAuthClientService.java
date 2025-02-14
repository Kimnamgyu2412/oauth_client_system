package oauth.client.system.oauth_client_system.email.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@Service
public class OAuthClientService {

    @Value("${oauth.server.url}")
    private String serverUrl;

    @Value("${oauth.client.id}")
    private String clientId;

    @Value("${oauth.client.secret}")
    private String clientSecret;

    @Value("${oauth.client.grantType}")
    private String grantType;

    @Value("${oauth.client.scope}")
    private String scope;

    private String accessToken;
    private Long accessTokenExpiryTime = 0L;  // 만료 시간을 추적하기 위한 변수

    private final RestTemplate restTemplate;

    public OAuthClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ✅ API 요청 메서드
    public  <T> ResponseEntity<T>  callApiWithToken(String apiEndpoint, HttpMethod httpMethod, Object data, Class<T> responseType) {
        String token = getAccessToken(); // 유효한 토큰 가져오기
        String url = serverUrl + apiEndpoint;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Object> entity = new HttpEntity<>(data,headers);

        try {
            return restTemplate.exchange(url, httpMethod, entity, responseType);
        }catch (HttpClientErrorException.Unauthorized e) {
            System.out.println("🔄 토큰 만료됨, 재발급 후 재시도...");
            obtainAccessToken();  // 새 토큰 발급

            headers.set("Authorization", "Bearer " + accessToken);
            entity = new HttpEntity<>(headers);
            return restTemplate.exchange(url, httpMethod, entity, responseType);
        }catch (Exception e){
            throw new NullPointerException(e.getMessage());
        }
    }

    // 클라이언트 크리덴셜을 사용하여 액세스 토큰 발급
    public void obtainAccessToken() {
        // 요청 본문 설정
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", grantType);
        body.add("client_id", clientId);
        // 암호화하지 않은 원본 시크릿을 body에 포함
        body.add("client_secret", clientSecret);
        body.add("scope", scope);

        // Basic Auth 헤더 설정 (원본 크리덴셜 사용)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Basic Auth에는 원본 크리덴셜을 사용
        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    serverUrl+"/oauth/token",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, String> responseBody = response.getBody();
                accessToken = responseBody.get("access_token");
                // 토큰 만료 시간 저장 (현재 시간 + expires_in)
                Integer expiresIn = Integer.valueOf(String.valueOf(responseBody.get("expires_in")));
                accessTokenExpiryTime = System.currentTimeMillis() + (expiresIn * 1000);

                System.out.println("Access Token obtained successfully");
            }
        } catch (Exception e) {
            System.err.println("Token request failed: " + e.getMessage());
            throw new RuntimeException("Failed to obtain access token", e);
        }
    }


    // 액세스 토큰을 얻거나 갱신된 토큰 반환
    public String getAccessToken() {
        // 만약 토큰이 없거나 만료되었다면 새로 발급 요청
        if (accessToken == null || System.currentTimeMillis() >= accessTokenExpiryTime) {
            obtainAccessToken();  // 토큰을 갱신
        }
        return accessToken;
    }
}
