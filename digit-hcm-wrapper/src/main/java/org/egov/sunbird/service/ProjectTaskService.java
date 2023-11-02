package org.egov.sunbird.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.egov.common.http.client.ServiceRequestClient;
import org.egov.common.models.household.Household;
import org.egov.common.models.household.HouseholdMember;
import org.egov.common.models.individual.Individual;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.text.SimpleDateFormat;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.egov.sunbird.Constants.*;

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
    String password = "abcd@123";



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

    public void transform(List<Task> taskList, Boolean isCreate) throws InterruptedException {

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
                              boolean isCreate) throws InterruptedException {
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

            RegistryRequest reqToCreateVC = registryRequestTransformer(task.getResources(), projectBeneficiary,task.getId(), task);
            String mobileNumber = reqToCreateVC.getBeneficiary().getMobileNumber();
            if (isCreate) {
                    String serviceDeliveryOsId = "";
                    StringBuilder uri = new StringBuilder();
                    uri.append(properties.getRegistryHost()).append(properties.getRegistryURL());
                    RegistryResponse response = null;
                    try {
                        response = serviceRequestClient.fetchResult(uri,
                                reqToCreateVC,
                                RegistryResponse.class);
                        serviceDeliveryOsId = response.getResult().getServiceDelivery().getOsid();

                        try {
                            // Create the Mapper data
                            VcServiceDelivery auditDetailsToAddInDB = VcServiceDelivery.builder()
                                    .id(UUID.randomUUID().toString())
                                    .serviceTaskId(task.getId())
                                    .certificateId(serviceDeliveryOsId)
                                    .distributedBy(emptyIfNull(task.getCreatedBy()))
                                    .beneficiaryId(emptyIfNull(task.getProjectBeneficiaryClientReferenceId()))
                                    .auditDetails(task.getAuditDetails())
                                    .build();
                            List<VcServiceDelivery> auditDetails = new ArrayList<VcServiceDelivery>();
                            auditDetails.add(auditDetailsToAddInDB );
                            vcServiceDeliveryRepository.save( auditDetails, properties.getSaveServiceDeliveryVCTaskTopic());
                        } catch (Exception exception) {
                            throw new CustomException(CREATION_OF_SERVICE_DELIVERY_MAPPER_TABLE_ERROR ,
                                    CREATION_OF_SERVICE_DELIVERY_MAPPER_TABLE_ERROR_MESSAGE +  exception);
                        }

                    } catch (Exception exception) {
                        log.error("error occurred while creating a VC using the Registry service: {}", ExceptionUtils.getStackTrace(exception));
                        try {
                            serviceDeliveryOsId = checkForExistingServiceTask(reqToCreateVC.getServiceDeliveryId());
                        } catch (Exception searchException) {
                            throw new CustomException(REGISTRY_VC_CREATION_ERROR,
                                    REGISTRY_VC_CREATION_ERROR_MESSAGE + ExceptionUtils.getStackTrace(searchException));
                        }
                    }
//                   fetchTheServiceDeliveryPDF(response.getResult().getServiceDelivery().getOsid());
                    try {
                        byte[] document =  fetchTheServiceDeliveryPDF(serviceDeliveryOsId);;
                        String beneficiaryId = task.getProjectBeneficiaryClientReferenceId();
                        String mobile = beneficiaryId;
                        String userOsid = getELockerUser(beneficiaryId, mobile);
                        String documentName = String.format("%s-%s.pdf", beneficiaryId, new Date().getTime());
                        String userToken = generateToken(mobile, properties.getKeycloakUserDefaultPassword());
                        String documentLocation = saveDocumentToELocker(documentName, userOsid, document, userToken);
                        sendDocumentForSelfAttestation(mobile, userOsid, documentName, documentLocation);
                    } catch (Exception e) {
                        log.error("Exception occurred while saving document to ELocker: {}", ExceptionUtils.getStackTrace(e));
                        throw new RuntimeException(e);
                    }
            }
                //     TODO update logic here
                //     get the VC_Id from the mapper table using the service_taskID
                //     update the vc in registry
                //     Emit an event to kafka topic to update using the taskID mapper with the new VC in the mapper table
        }
    }


    private String checkForExistingServiceTask (String serviceDeliveryId) {

        StringBuilder uri = new StringBuilder();
        uri.append(properties.getRegistryHost()).append(properties.getServiceDeliverySearchURL());

        // Create a RestTemplate instance
        RestTemplate restTemplate = new RestTemplate();

        // Set the request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // Define the request body
        String requestBody = "{\n" +
                "  \"offset\": 0,\n" +
                "  \"limit\": 2,\n" +
                "  \"filters\": {\n" +
                "    \"serviceDeliveryId\": {\n" +
                "      \"eq\": \""+serviceDeliveryId+"\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        // Create a request entity with the headers and request body
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
        // Send the POST request and handle the response
        List<Map<String, Object>> responseEntity = serviceRequestClient.fetchResult(uri, requestEntity, List.class);
        // Check the response status and handle the JSON response as needed
        if (responseEntity.size() == 0 ) {
            log.error("error occurred while searching a VC using the Registry service: {}", responseEntity);
            throw new CustomException(REGISTRY_VC_SEARCH_ERROR,
                    REGISTRY_VC_SEARCH_ERROR_MESSAGE + responseEntity.toString());
        }
        return (String) responseEntity.get(0).get("osid");
    }

    private String generateToken(String username, String password) {
        StringBuilder uri = new StringBuilder();
        uri.append(properties.getKeycloakHost()).append(properties.getKeycloakTokenUri());

        // Set the headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Create the request body with the form data
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", properties.getKeycloakClientId());
        formData.add("username", username);
        formData.add("password", password);
        formData.add("grant_type", "password");

        // Create an HttpEntity with the headers and request body
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);

        Map results = serviceRequestClient.fetchResult(uri, requestEntity, Map.class);
        return ((Map<String, String>) results).get("access_token");
    }

    private String getELockerUser(String beneficiaryId, String mobileNumber) throws InterruptedException {
        StringBuilder uri = new StringBuilder();
        uri.append(properties.getRegistryHost()).append(properties.getRegistryElockerUserSearchURL());

        // Set the headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create the request body with the form data
        String requestBody = "{" +
                "\"offset\": 0," +
                "\"limit\": 1," +
                "\"filters\": {" +
                "\"identityDetails.fullName\": {" +
                "\"eq\": \""+ beneficiaryId +"\"" +
                "}" +
                "}" +
                "}";

        // Create an HttpEntity with the headers and request body
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        List<Map<String, Object>> results = serviceRequestClient.fetchResult(uri, requestEntity, List.class);
        if(results.size() == 0) {
            this.inviteElockerUser(beneficiaryId, mobileNumber);
            TimeUnit.SECONDS.sleep(5);
            return this.getELockerUser(beneficiaryId, mobileNumber);
        }
        return (String) results.get(0).get("osid");
    }

    private void inviteElockerUser(String beneficiaryId, String mobileNumber) {
        StringBuilder uri = new StringBuilder();
        uri.append(properties.getRegistryHost()).append(properties.getRegistryElockerUserInviteURL());

        // Set the headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create the request body with the form data
        String requestBody = "{" +
                "\"identityDetails\": {" +
                "\"fullName\": \"" + beneficiaryId + "\"" +
                "}," +
                "\"contactDetails\": {" +
                "\"email\": \"" + mobileNumber +"\"," +
                "\"mobile\": \""+ mobileNumber + "\"" +
                "}," +
                "\"documents\": []" +
                "}";

        // Create an HttpEntity with the headers and request body
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        Map<String, Object> results = (Map<String, Object>) serviceRequestClient.fetchResult(uri, requestEntity, Map.class);
        Map<String, String> params = (Map<String, String>) results.get("params");
        if(!params.get("status").equals("SUCCESSFUL")) {
            throw new RuntimeException("Error while creating elocker user for beneficiary");
        }
    }

    private String saveDocumentToELocker(String documentName, String userOsid, byte[] document, String token) {
        StringBuilder uri = new StringBuilder();
        uri.append(properties.getRegistryHost())
                .append(properties.getRegistryElockerUserGetURL())
                .append("/").append(userOsid)
                .append("/attestation/documents");

        // Define the request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Accept", "application/json");
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        // Define the request body as a MultiValueMap (for file upload)
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();

        // Replace 'fileBytes' with your byte array variable
        byte[] fileBytes = document;
        ByteArrayResource byteArrayResource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return documentName; // Set the desired file name here
            }
        };

        formData.add("files", byteArrayResource);

        // Create an HttpEntity with the headers and request body
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(formData, headers);

        Map<String, Object> results = (Map<String, Object>) serviceRequestClient.fetchResult(uri, requestEntity, Map.class);
        List<String> documentLocations = (List<String>) results.get("documentLocations");
        return documentLocations.get(0);
    }

    private void sendDocumentForSelfAttestation(String mobile, String userOsid, String documentName, String documentLocation) {
        String token = generateToken(mobile, properties.getKeycloakUserDefaultPassword()); // generate user token
        StringBuilder uri = new StringBuilder();
        uri.append(properties.getRegistryHost()).append(properties.getRegistryElockerSendForAttestationURL());
        // Define the request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");
        headers.set("Authorization", "Bearer " + token);

        // Define the request body as a JSON string
        String requestBody = "{" +
                "\"name\": \"attestation-SELF\"," +
                "\"entityName\": \"User\"," +
                "\"entityId\": \"" + userOsid + "\"," +
                "\"additionalInput\": {" +
                "\"name\": \"" + documentName + "\"," +
                "\"fileUrl\": [" +
                "\"" + documentLocation + "\"" +
                "]" +
                "}" +
                "}";

        // Create an HttpEntity with the headers and request body
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        // Create a Rest
        Map<String, Object> results = (Map<String, Object>) serviceRequestClient.fetchResult(uri, requestEntity, Map.class);
        Map<String, String> params = (Map<String, String>) results.get("params");
        if(!params.get("status").equals("SUCCESSFUL")) {
            throw new RuntimeException("Error while creating elocker user for beneficiary");
        }
    }

    public byte[] fetchTheServiceDeliveryPDF(String osid) {
        RestTemplate restTemplate = new RestTemplate();

        // Define the URL and headers for the request
        StringBuilder url = new StringBuilder();
        String value = properties.getProjectBeneficiarySearchUrl();
        url.append(properties.getRegistryHost()).append(properties.getRegistryURL()).append("/"+ osid);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_PDF));

        // Create a request entity with the headers
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        // Send the request to fetch the PDF
        ResponseEntity<byte[]> responseEntity = restTemplate.exchange(url.toString(), HttpMethod.GET, requestEntity, byte[].class);

        // Check the response status
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            byte[] responseBody = responseEntity.getBody();
            System.out.println("response from pdf"+responseBody);
            return responseBody;
            // Handle the PDF response as needed (e.g., save it to a file)
        } else {
            System.out.println("Request failed with status code: " + responseEntity.getStatusCodeValue());
            throw new CustomException(CREATION_OF_SERVICE_DELIVERY_MAPPER_TABLE_ERROR ,
                    CREATION_OF_SERVICE_DELIVERY_MAPPER_TABLE_ERROR_MESSAGE);
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

    public static String convertUnixTimestampToISO8601(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Set the time zone to UTC
        return sdf.format(date);
    }

    public static String generateRandomPhoneNumber() {
        Random random = new Random();
        // Generate a random 3-digit area code (between 100 and 999)
        int areaCode = 100 + random.nextInt(900);
        // Generate a random 7-digit local number (between 1000000 and 9999999)
        int localNumber = 1000000 + random.nextInt(9000000);
        // Format the phone number with dashes or spaces
        String formattedPhoneNumber = String.format("%03d%07d", areaCode, localNumber);
        return formattedPhoneNumber;
    }


    public static String emptyIfNull(String input) {
    if (input == null) {
        return "";
    }
    return input;
    }

    public static RegistryRequest registryRequestTransformer(List<TaskResource> resources, ProjectBeneficiary projectBeneficiary, String serviceDeliveryId , Task task) {

        List<ResourceDTO> benefitsDelivered = new ArrayList<ResourceDTO>() ;
        for (TaskResource resource : resources) {

            ResourceDTO resourceToSend = ResourceDTO.builder()
                    .productVariantId(emptyIfNull(resource.getProductVariantId()))
                    .quantity(Integer.parseInt(resource.getQuantity().toString() != null ? resource.getQuantity().toString() : "0"))
                    .isDelivered(resource.getIsDelivered() ? true : false)
                    .deliveryComment(emptyIfNull(resource.getDeliveryComment()))
                    .deliveryDate(convertUnixTimestampToISO8601(resource.getAuditDetails().getCreatedTime()))
                    .deliveredBy(emptyIfNull(resource.getAuditDetails().getCreatedBy())).build();
            benefitsDelivered.add(resourceToSend);
        }
        BenificiaryDTO benificiaryDTO = BenificiaryDTO.builder()
                .beneficiaryId(emptyIfNull(task.getProjectBeneficiaryClientReferenceId()))
                .beneficiaryType("HOUSEHOLD")
                .projectId(emptyIfNull(projectBeneficiary.getId()))
                .tenantId(emptyIfNull(projectBeneficiary.getTenantId()))
                .registrationDate(convertUnixTimestampToISO8601(projectBeneficiary.getDateOfRegistration()))
                .mobileNumber(generateRandomPhoneNumber())
                .build();

        return new RegistryRequest(serviceDeliveryId, benificiaryDTO, benefitsDelivered);
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
}
