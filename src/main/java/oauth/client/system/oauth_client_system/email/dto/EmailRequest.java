package oauth.client.system.oauth_client_system.email.dto;

import lombok.Data;

@Data
public class EmailRequest {
    private String subject;
    private String senderEmail;
    private String senderName;
    private String receiverEmail;
    private String body;
}

