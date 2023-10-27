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

    @Value("${sunbird.registry.url}")
    private String registryURL;

    @Value("${transformer.consumer.vc.create.serviceDelivery.task.topic}")
    private String saveServiceDeliveryVCTaskTopic;

    @Value("${transformer.consumer.vc.update.serviceDelivery.task.topic}")
    private String updateServiceDeliveryVCTaskTopic;

    @Value("${sunbird.registry.ulp.credential.host}")
    private String registryULPHost;

    @Value("${sunbird.registry.ulp.credential.issue.url}")
    private String registryULPCredentialIssueURL;

    @Value("${sunbird.registry.ulp.issuerId}")
    private String registryULPIssuerId;

    @Value("${sunbird.registry.ulp.templateId}")
    private String registryULPTemplateId;

    @Value("${sunbird.registry.ulp.schemaId}")
    private String registryULPSchemaId;

    @Value("${sunbird.refistry.ulp.schemaVersion}")
    private String registryULPSchemaVersion;


}
