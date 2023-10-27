package org.egov.sunbird.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifiableCredentialResponseDTO {
    private Credential credential;
    private String credentialSchemaId;
    private String createdAt;
    private String updatedAt;
    private String createdBy;
    private String updatedBy;
    private List<String> tags;

    @Getter
    @Setter
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Credential {
        private String id;
        private List<String> type;
        private Proof proof;
        private String issuer;
        @JsonProperty(value = "@context")
        private List<String> context;
        private String issuanceDate;
        private String expirationDate;
        private CredentialSubject credentialSubject;
    }

    @Getter
    @Setter
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Proof {
        private String type;
        private String created;
        private String proofValue;
        private String proofPurpose;
        private String verificationMethod;
    }

    @Getter
    @Setter
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CredentialSubject {
        private String id;
        private BenificiaryDTO beneficiary;
        private List<ResourceDTO> benefitsDelivered;
        private String serviceDeliveryId;
    }

}

