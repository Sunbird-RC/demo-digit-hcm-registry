package org.egov.sunbird.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;

@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifiableCredentialDTO {
    private Credential credential;
    private String credentialSchemaId;
    private String credentialSchemaVersion;
    private List<String> tags;
    private String method;

    @Data
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Credential {

        @JsonProperty(value = "@context")
        private List<String> context;
        private List<String> type;
        private String issuer;
        private String issuanceDate;
        private String expirationDate;
        private CredentialSubject credentialSubject;
        private Options options;
    }

    @Data
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CredentialSubject {
        @Value("")
        private String id;
        private String serviceDeliveryId;
        private BenificiaryDTO beneficiary;
        private List<ResourceDTO> benefitsDelivered;
    }

    @Data
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Options {
        private String created;
        private CredentialStatus credentialStatus;
    }

    @Data
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CredentialStatus {
        private String type;
    }
}

