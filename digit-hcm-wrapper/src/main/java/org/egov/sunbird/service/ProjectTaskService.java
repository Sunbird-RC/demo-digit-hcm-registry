package org.egov.sunbird.service;
import java.text.SimpleDateFormat;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.protocol.types.Field;
import org.egov.common.http.client.ServiceRequestClient;
import org.egov.common.models.household.Household;
import org.egov.common.models.household.HouseholdMember;
import org.egov.common.models.individual.Individual;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.egov.common.models.project.ProjectBeneficiary;
import org.egov.common.models.project.Task;
import org.egov.common.models.project.TaskResource;
import org.egov.common.producer.Producer;
import org.egov.sunbird.Repository.VCServiceDeliveryRepository;
import org.egov.sunbird.config.SunbirdProperties;
import org.egov.sunbird.models.*;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static org.egov.sunbird.Constants.HOUSEHOLD_FETCH_ERROR;
import static org.egov.sunbird.Constants.HOUSEHOLD_FETCH_ERROR_MESSAGE;
import static org.egov.sunbird.Constants.HOUSEHOLD_MEMBER_FETCH_ERROR;
import static org.egov.sunbird.Constants.HOUSEHOLD_MEMBER_FETCH_ERROR_MESSAGE;
import static org.egov.sunbird.Constants.INDIVIDUAL_FETCH_ERROR;
import static org.egov.sunbird.Constants.INDIVIDUAL_FETCH_ERROR_MESSAGE;
import static org.egov.sunbird.Constants.PROJECT_BENEFICIARY_FETCH_ERROR;
import static org.egov.sunbird.Constants.PROJECT_BENEFICIARY_FETCH_ERROR_MESSAGE;
import static org.egov.sunbird.Constants.REGISTRY_VC_CREATION_ERROR;
import static org.egov.sunbird.Constants.REGISTRY_VC_CREATION_ERROR_MESSAGE;
import static org.egov.sunbird.Constants.CREATION_OF_SERVICE_DELIVERY_MAPPER_TABLE_ERROR;
import static org.egov.sunbird.Constants.CREATION_OF_SERVICE_DELIVERY_MAPPER_TABLE_ERROR_MESSAGE;

@Component
@Slf4j
public class ProjectTaskService {

    private final Producer producer;

    private final VCServiceDeliveryRepository vcServiceDeliveryRepository;
    private final SunbirdProperties properties;

    private final ServiceRequestClient serviceRequestClient;

    private final HouseholdService householdService;
    private final IndividualService individualService;
    private final ProjectService projectService;


    @Autowired
    protected ProjectTaskService(Producer producer, VCServiceDeliveryRepository vcServiceDeliveryRepository, SunbirdProperties properties,
                                 ServiceRequestClient serviceRequestClient, HouseholdService householdService,
                                 IndividualService individualService,
                                 ProjectService projectService) {
        this.producer = producer;
        this.vcServiceDeliveryRepository = vcServiceDeliveryRepository;
        this.properties = properties;
        this.serviceRequestClient = serviceRequestClient;
        this.householdService = householdService;
        this.individualService = individualService;
        this.projectService = projectService;
    }

    public void transform(List<Task> taskList, Boolean isCreate) {

        List<String> projectBeneficiaryClientReferenceIds = new ArrayList<>();
        List<String> householdClientReferenceIds = new ArrayList<>();
        List<String> individualClientReferenceIds = new ArrayList<>();
        String tenantId = taskList.get(0).getTenantId();

        Map<String, ProjectBeneficiary> projectBeneficiaryMap = new HashMap<>();
        Map<String, Household> householdMap = new HashMap<>();
        Map<String, Individual> individualMap = new HashMap<>();
        Map<String, HouseholdMember> hosueholdHeadMap = new HashMap<>();

        for (Task task : taskList) {
            projectBeneficiaryClientReferenceIds.add(task.getProjectBeneficiaryClientReferenceId());
        }

        setProjectBeneficiaries(projectBeneficiaryClientReferenceIds, tenantId, projectBeneficiaryMap);
        setHouseholds(projectBeneficiaryClientReferenceIds, tenantId, householdClientReferenceIds, householdMap);
        setHouseholdHeads(householdClientReferenceIds, tenantId, individualClientReferenceIds, hosueholdHeadMap);
        setIndividuals(individualClientReferenceIds, tenantId, individualMap);

        processTasks(taskList, projectBeneficiaryMap, householdMap, hosueholdHeadMap, individualMap, isCreate);

    }

    private void processTasks(List<Task> taskList,
                              Map<String, ProjectBeneficiary> projectBeneficiaryMap,
                              Map<String, Household> householdMap,
                              Map<String, HouseholdMember> hosueholdHeadMap,
                              Map<String, Individual> individualMap,
                              boolean isCreate) {
        for (Task task : taskList) {
            ProjectBeneficiary projectBeneficiary = projectBeneficiaryMap
                    .get(task.getProjectBeneficiaryClientReferenceId());
            if (projectBeneficiary == null) {
                throw new CustomException(PROJECT_BENEFICIARY_FETCH_ERROR,
                        PROJECT_BENEFICIARY_FETCH_ERROR_MESSAGE + task.getProjectBeneficiaryClientReferenceId());
            }
            Household household = householdMap.get(projectBeneficiary.getBeneficiaryClientReferenceId());
            if (household == null) {
                throw new CustomException(HOUSEHOLD_FETCH_ERROR,
                        HOUSEHOLD_FETCH_ERROR_MESSAGE + projectBeneficiary.getBeneficiaryClientReferenceId());
            }
            HouseholdMember householdHeadMember = hosueholdHeadMap.get(household.getClientReferenceId());

            if (householdHeadMember == null) {
                throw new CustomException(HOUSEHOLD_MEMBER_FETCH_ERROR,
                        HOUSEHOLD_MEMBER_FETCH_ERROR_MESSAGE + household.getClientReferenceId());
            }

            Individual individual = individualMap.get(householdHeadMember.getIndividualClientReferenceId());

            if (individual == null) {
                throw new CustomException(INDIVIDUAL_FETCH_ERROR,
                        INDIVIDUAL_FETCH_ERROR_MESSAGE + householdHeadMember.getIndividualClientReferenceId());
            }

//            RegistryRequest reqToCreateVC = registryRequestTransformer(task.getResources(), projectBeneficiary,task.getId());

            VerifiableCredentialDTO reqToCreateVC = registryRequestULPTransformer(task.getResources(), projectBeneficiary,task.getId());

            if (isCreate) {

                    StringBuilder uri = new StringBuilder();
//                    uri.append(properties.getRegistryHost()).append(properties.getRegistryURL());
                    uri.append(properties.getRegistryULPHost()).append(properties.getRegistryULPCredentialIssueURL());
                    VerifiableCredentialResponseDTO response = null;
                    try {
                        response = serviceRequestClient.fetchResult(uri,
                                reqToCreateVC,
                                VerifiableCredentialResponseDTO.class);
                    } catch (Exception exception) {
                        log.error("error occurred while creating a VC using the Registry service: {}", exception.getMessage());
                        throw new CustomException(REGISTRY_VC_CREATION_ERROR,
                                REGISTRY_VC_CREATION_ERROR_MESSAGE + exception);
                    }
                    try {
                        // Create the Mapper data
                        VcServiceDelivery auditDetailsToAddInDB = VcServiceDelivery.builder()
                                .id(UUID.randomUUID().toString())
                                .serviceTaskId(task.getId())
                                .certificateId(response.getCredential().getId())
                                .distributedBy(emptyIfNull(task.getCreatedBy()))
                                .beneficiaryId(emptyIfNull(task.getProjectBeneficiaryId()))
                                .auditDetails(task.getAuditDetails())
                                .build();
                        List<VcServiceDelivery> auditDetails = new ArrayList<VcServiceDelivery>();
                        auditDetails.add(auditDetailsToAddInDB );
                        vcServiceDeliveryRepository.save( auditDetails, properties.getSaveServiceDeliveryVCTaskTopic());
                    } catch (Exception exception) {
                        throw new CustomException(CREATION_OF_SERVICE_DELIVERY_MAPPER_TABLE_ERROR ,
                                CREATION_OF_SERVICE_DELIVERY_MAPPER_TABLE_ERROR_MESSAGE +  exception);
                    }


            } else {
                //     TODO update logic here
                //     get the VC_Id from the mapper table using the service_taskID
                //     update the vc in registry
                //     Emit an event to kafka topic to update using the taskID mapper with the new VC in the mapper table
            }
        }
    }


    public static String convertToISO8601(long unixTimestamp) {
        // Convert Unix timestamp to Instant
        Instant instant = Instant.ofEpochSecond(unixTimestamp);

        // Convert Instant to the ISO 8601 format with UTC timezone
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .withZone(ZoneId.of("UTC"));
        String iso8601DateTime = formatter.format(instant);

        return iso8601DateTime;
    }

    public static String convertTimestampToReadableDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata")); // Set the time zone to Indian Standard Time (IST)

        // Create a Date object from the timestamp
        Date date = new Date(timestamp);

        // Format the date as a string
        String formattedDateTime = sdf.format(date);

        return formattedDateTime;
    }

    public static String emptyIfNull(String input) {
    if (input == null) {
        return "";
    }
    return input;
    }

    public static RegistryRequest registryRequestTransformer(List<TaskResource> resources, ProjectBeneficiary projectBeneficiary, String serviceDeliveryId) {

        List<ResourceDTO> benefitsDelivered = new ArrayList<ResourceDTO>() ;
        for (TaskResource resource : resources) {

            ResourceDTO resourceToSend = ResourceDTO.builder()
                    .productVariantId(emptyIfNull(resource.getProductVariantId()))
                    .quantity(resource.getQuantity().toString() != null ? resource.getQuantity().toString() : "0")
                    .isDelivered(resource.getIsDelivered() ? true : false)
                    .deliveryComment(emptyIfNull(resource.getDeliveryComment()))
                    .deliveryDate(convertTimestampToReadableDate(resource.getAuditDetails().getCreatedTime()))
                    .deliveredBy(emptyIfNull(resource.getAuditDetails().getCreatedBy())).build();
            benefitsDelivered.add(resourceToSend);
        }
        BenificiaryDTO benificiaryDTO = BenificiaryDTO.builder()
                .beneficiaryId(emptyIfNull(projectBeneficiary.getId()))
                .beneficiaryType("HOUSEHOLD")
                .projectId(emptyIfNull(projectBeneficiary.getId()))
                .tenantId(emptyIfNull(projectBeneficiary.getTenantId()))
                .registrationDate(convertTimestampToReadableDate(projectBeneficiary.getDateOfRegistration()))
                .build();

        return new RegistryRequest(serviceDeliveryId, benificiaryDTO, benefitsDelivered);
    }

    public VerifiableCredentialDTO registryRequestULPTransformer(List<TaskResource> resources, ProjectBeneficiary projectBeneficiary, String serviceDeliveryId) {
        List<String> tags = new ArrayList<String>() ;
        tags.add("hcm"); tags.add("digit");
        List<String> type = new ArrayList<String>() ;
        type.add("VerifiableCredential"); type.add("ServiceDelivery");
        List<String> context = new ArrayList<String>() ;
        context.add("https://www.w3.org/2018/credentials/v1"); context.add("https://www.w3.org/2018/credentials/examples/v1");
        List<ResourceDTO> benefitsDelivered = new ArrayList<ResourceDTO>() ;
        for (TaskResource resource : resources) {

            ResourceDTO resourceToSend = ResourceDTO.builder()
                    .productVariantId(emptyIfNull(resource.getProductVariantId()))
                    .quantity(resource.getQuantity().toString() != null ? resource.getQuantity().toString() : "0")
                    .isDelivered(resource.getIsDelivered() ? true : false)
                    .deliveryComment(emptyIfNull(resource.getDeliveryComment()))
                    .deliveryDate(convertTimestampToReadableDate(resource.getAuditDetails().getCreatedTime()))
                    .deliveredBy(emptyIfNull(resource.getAuditDetails().getCreatedBy())).build();
            benefitsDelivered.add(resourceToSend);
        }
        BenificiaryDTO benificiaryDTO = BenificiaryDTO.builder()
                .beneficiaryId(emptyIfNull(projectBeneficiary.getId()))
                .beneficiaryType("HOUSEHOLD")
                .projectId(emptyIfNull(projectBeneficiary.getId()))
                .tenantId(emptyIfNull(projectBeneficiary.getTenantId()))
                .registrationDate(convertTimestampToReadableDate(projectBeneficiary.getDateOfRegistration()))
                .build();


        VerifiableCredentialDTO reqToReturn = VerifiableCredentialDTO.builder()
                .tags(tags)
                .credentialSchemaVersion(properties.getRegistryULPSchemaVersion())
                .credentialSchemaId(properties.getRegistryULPSchemaId())
                .method("VC")
                .credential(VerifiableCredentialDTO.Credential.builder()
                        .type(type)
                        .context(context)
                        .issuer(properties.getRegistryULPIssuerId())
                        .issuanceDate("2023-02-06T11:56:27.259Z")
                        .expirationDate("2023-02-08T11:56:27.259Z")
                        .options(VerifiableCredentialDTO.Options.builder()
                                .created("2020-04-02T18:48:36Z")
                                .credentialStatus(VerifiableCredentialDTO.CredentialStatus.builder()
                                        .type("RevocationList2020Status")
                                        .build())
                                .build())
                        .credentialSubject(VerifiableCredentialDTO.CredentialSubject.builder()
                                .id("")
                                .serviceDeliveryId(serviceDeliveryId)
                                .beneficiary(benificiaryDTO)
                                .benefitsDelivered(benefitsDelivered)
                                .build())
                        .build())
                .build();
        return reqToReturn;
    }

    private void setIndividuals(List<String> individualClientReferenceIds,
                                String tenantId, Map<String, Individual> individualMap) {
        List<Individual> individuals = individualService
                .searchIndividuals(individualClientReferenceIds, tenantId);

        for (Individual individual : individuals) {
            individualMap.put(individual.getClientReferenceId(), individual);
        }
    }

    private void setHouseholdHeads(List<String> householdClientReferenceIds,
                                   String tenantId, List<String> individualClientReferenceIds,
                                   Map<String, HouseholdMember> hosueholdHeadMap) {
        for (String householdClientReferenceId : householdClientReferenceIds) {
            List<HouseholdMember> members = householdService
                    .searchHouseholdMembers(householdClientReferenceId, tenantId, true);
            if (!CollectionUtils.isEmpty(members)) {
                HouseholdMember member = members.get(0);
                individualClientReferenceIds.add(member.getIndividualClientReferenceId());
                hosueholdHeadMap.put(member.getHouseholdClientReferenceId(), member);
            }
        }
    }

    private void setHouseholds(List<String> projectBeneficiaryClientReferenceIds,
                               String tenantId, List<String> householdClientReferenceIds,
                               Map<String, Household> householdMap) {
        List<Household> households = householdService
                .searchHouseholds(projectBeneficiaryClientReferenceIds, tenantId);
        for (Household household: households) {
            householdClientReferenceIds.add(household.getClientReferenceId());
            householdMap.put(household.getClientReferenceId(), household);
        }
    }

    private void setProjectBeneficiaries(List<String> projectBeneficiaryClientReferenceIds,
                                         String tenantId, Map<String, ProjectBeneficiary> projectBeneficiaryMap) {
        List<ProjectBeneficiary> beneficiaries = projectService
                .searchBeneficiaries(projectBeneficiaryClientReferenceIds, tenantId);
        for (ProjectBeneficiary projectBeneficiary : beneficiaries) {
            projectBeneficiaryClientReferenceIds.add(projectBeneficiary.getBeneficiaryClientReferenceId());
            projectBeneficiaryMap.put(projectBeneficiary.getClientReferenceId(), projectBeneficiary);
        }
    }

    public List<VcServiceDelivery> findByServiceTaskId(String serviceTaskId) {
        List<String> serviceDeliveryId = new ArrayList<>();
        serviceDeliveryId.add(serviceTaskId);
        return vcServiceDeliveryRepository.findById(serviceDeliveryId, "serviceTaskId");
    }
}
