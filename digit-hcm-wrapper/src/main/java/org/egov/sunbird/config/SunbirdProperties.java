package org.egov.sunbird.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Component
public class SunbirdProperties {

    @Value("${egov.project.host}")
    private String projectHost;

    @Value("${egov.search.project.url}")
    private String projectSearchUrl;

    @Value("${egov.search.project.beneficiary.url}")
    private String projectBeneficiarySearchUrl;

    @Value("${egov.household.host}")
    private String householdHost;

    @Value("${egov.search.household.url}")
    private String householdSearchUrl;

    @Value("${egov.search.household.member.url}")
    private String householdMemberSearchUrl;

    @Value("${egov.individual.host}")
    private String individualHost;

    @Value("${egov.search.individual.url}")
    private String individualSearchUrl;

    @Value("${search.api.limit:100}")
    private String searchApiLimit;

    @Value("${transformer.consumer.bulk.create.project.task.topic}")
    private String saveProjectTaskTopic;

    @Value("${transformer.consumer.bulk.update.project.task.topic}")
    private String updateProjectTaskTopic;

    @Value("${sunbird.registry.host}")
    private String registryHost;

    @Value("${sunbird.registry.serviceDelivery.create}")
    private String registryURL;

    @Value("${sunbird.registry.serviceDelivery.search}")
    private String serviceDeliverySearchURL;

    @Value("${sunbird.registry.elocker.user.search}")
    private String registryElockerUserSearchURL;

    @Value("${sunbird.registry.elocker.user}")
    private String registryElockerUserGetURL;

    @Value("${sunbird.registry.elocker.user.invite}")
    private String registryElockerUserInviteURL;

    @Value("${sunbird.registry.elocker.send}")
    private String registryElockerSendForAttestationURL;

    @Value("${transformer.consumer.vc.create.serviceDelivery.task.topic}")
    private String saveServiceDeliveryVCTaskTopic;

    @Value("${transformer.consumer.vc.update.serviceDelivery.task.topic}")
    private String updateServiceDeliveryVCTaskTopic;

    @Value("${sunbird.keycloak.host}")
    private String keycloakHost;

    @Value("${sunbird.keycloak.tokenUri}")
    private String keycloakTokenUri;

    @Value("${sunbird.keycloak.clientId}")
    private String keycloakClientId;

    @Value("${sunbird.keycloak.user.default.password}")
    private String keycloakUserDefaultPassword;

    @Value("${sunbird.keycloak.admin.client.secret}")
    private String keycloakAdminSecret;

}
