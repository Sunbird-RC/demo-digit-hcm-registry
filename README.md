# demo-digit-hcm-registry
Demo Registry for issuing VCs for Health Campaign service delivery

## Quick setup 

```bash
cd demo-digit-hCM-registry
```

- Define your shema and store them in the /imports/schema directory (for reference we have added the ServiceDelivery schema)

- Dont alter the other existing schemas like common.json, Issuer.json, Localization.json and User.Json there are used by the elocker-service

- Once the schemas are defined your schema-templates in the /imports/schema/templates directory. 

- Refer to the existing ServiceDelivery.html template

- create a .env file. You can use the .env.example as a referance

- the docker-compose.yaml is up to date. 

- do a docker-compose up -d to start all the Sunbird-RC services  

```bash
sudo docker-compose up -d
```

- this creates two dataStorage volumes in your local directory with the names db-data and es-data

- the es-data volume that is created might have some permission issues and it might terminate your docker-compose up process 

- so do a sudo chmod -R 777 es-data 

```bash
sudo chmod -R 777 es-data 
```

- and up the the docker services after this.

```bash
sudo docker-compose up -d
```

## Registry setup 



- Once all the RC services are up. 

- Check on which port the keycloak service is running. Most likely it will be running on the port http://localhost:8080 

- open your browser and access the keycloak service as an admin (http://localhost:8080/auth)

- This will ask you for the password. 
    
    Username : admin 
    Password : admin

- Once you login 

- you could see a dashboard --> redirect to clients (you can see on the sidebar)

- once you click on clients, on the dashboard you could see multiple clients that are already created by default 

- click on the 'admin-api' 

- under the admin-api dashboard, click on the Credentials

- You could see a "Regenerate Secret"

- Copy the generated token 

- Paste it in the .env file with the variable - KEYCLOAK_SECRET (Ex: KEYCLOAK_SECRET=0123c45e-3378-4f70-a8ba-153ffb282b23)

- restart the registry service 

```bash
sudo docker-compose up -d --force-recreate --no-deps registry
```

- RC services are up and running!!

## Digit HCM Wrapper setup 

- for this you would need the access to the moz-health-qa (You can get the axis from HCM team)

- get the access to a the QA server 

- Port forward the mentioned services 

    - Project    (http://localhost:8093)
    - Household  (http://localhost:8094)
    - Individual (http://localhost:8095)

- Then update the application.properties of this service 

- sunbird.keycloak.admin.client.secret must be same as the KEYCLOAK_SECRET that you have added in the .env file while the registry setup

- you are go to go


## CURL's for testing

- You can refer to the /curls folder for the curls

-  "projectBeneficiaryClientReferenceId": "8d031b40-68e3-11ee-b8d1-4ba36535319c" in the HCM_Wrapper_CURL_For_Task_Creation.txt file is used as the user name for the elocker login to check the issued credentials for this task 

## Flow 

- This CURL creates a kafka event in the wrapper 

- This event will be consumed with in the wrapper (Production expectation: the event will be receved from the persister in digit)

- you can refer to the Consumer logic where were are receving the data from the persister 

- And this data is transformed based on the schemas you have deined in the registry setup 

- And a VC is generated for this task and the corresponding steps are about how we are storing the VC in the e-locker






