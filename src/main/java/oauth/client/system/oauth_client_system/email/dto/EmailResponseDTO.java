package oauth.client.system.oauth_client_system.email.dto;

import lombok.Getter;

@Getter
public class EmailResponseDTO {

    private String senderEmail;
    private String senderName;

    private Long personIdx;
    private String personEmail;
    private String personGubun;

    private String title;
    private String body;

    private Long campaignIdx;

    private boolean noRequest;
    private String noRequestReason;

    private String sentDate;
    private String sentResultCode;

    private String messageId;
    private String lastEventType;
    private String clientId;

}
