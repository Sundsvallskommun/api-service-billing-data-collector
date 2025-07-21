# BillingDataCollector

_The service schedules and perform billings._

### Prerequisites

- **Java 21 or higher**
- **Maven**
- **MariaDB**
- **Git**

### Installation

1. **Clone the repository:**

```bash
git clone https://github.com/Sundsvallskommun/api-service-billing-data-collector.git
cd api-service-billing-data-collector
```

2. **Configure the application:**

   Before running the application, you need to set up configuration settings.
   See [Configuration](#configuration)

   **Note:** Ensure all required configurations are set; otherwise, the application may fail to start.

3. **Ensure dependent services are running:**

   *Messaging*

   - Purpose: Used to send emails.
   - Repository: https://github.com/Sundsvallskommun/api-service-messaging
   - Setup Instructions: See documentation in repository above for installation and configuration steps.

   *Open-E Platform*
   - Purpose: Used to fetch data from the Open-E platform.

   *Party*
   - Purpose: Used to translate party ids to legal ids.
   - Repository: https://github.com/Sundsvallskommun/api-service-party
   - Setup Instructions: See documentation in repository above for installation and configuration steps.

   *Billing-Preprocessor*
   - Purpose: Used to create billing records.
   - Repository: https://github.com/Sundsvallskommun/api-service-billing-preprocessor
   - Setup Instructions: See documentation in repository above for installation and configuration steps.

4. **Build and run the application:**

- Using Maven:

```bash
mvn spring-boot:run
```

- Using Gradle:

```bash
gradle bootRun
```

## API Documentation

Access the API documentation via:

- **Swagger UI:** [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

## Usage

### API Endpoints

See the [API Documentation](#api-documentation) for detailed information on available endpoints.

### Example Request

```bash
curl -X 'POST' 'https://localhost:8080/2281/trigger/12345'
```

## Configuration

Configuration is crucial for the application to run successfully. Ensure all necessary settings are configured in
`application.yml`.

### Key Configuration Parameters

- **Server Port:**

```yaml
server:
  port: 8080
```

- **Database Settings**

```yaml
spring:
  datasource:
    driver-class-name: org.mariadb.jdbc.Driver
    username: <db_username>
    password: <db_password>
    url: jdbc:mariadb://<db_host>:<db_port>/<database>
  jpa:
    properties:
      jakarta:
        persistence:
          schema-generation:
            database:
              action: validate
  flyway:
    enabled: <true|false> # Enable if you want to run Flyway migrations
```

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
		  messaging:
		    client-id: <client-id>
		    client-secret: <client-secret>
		    authorization-grant-type: client_credentials
		  billing-preprocessor:
		    client-id: <client-id>
		    client-secret: <client-secret>
		    authorization-grant-type: client_credentials
		  party:
		    client-id: <client-id>
		    client-secret: <client-secret>
		    authorization-grant-type: client_credentials
		provider:
		  messaging:
		  	token-uri: <token-uri>
		  billing-preprocessor:
		    token-uri: <token-uri>
		  party:
		  	token-uri: <token-uri>
integration:
  messaging:
  	base-url: <messaging-url>
  	connect-timeout: 5000
  	read-timeout: 30000
  	oauth2:
	  token-url: <token-url>
	  authorization-grant-type: <authorization-grant-type>
  party:
  	base-url: <party-url>
  	connect-timeout: 5000
  	read-timeout: 30000
  	oauth2:
      token-url: <token-url>
      authorization-grant-type: <authorization-grant-type>
  billing-preprocessor:
  	base-url: <billing-preprocessor-url>
  	connect-timeout: 5000
  	read-timeout: 30000
  	oauth2:
      token-url: <token-url>
      authorization-grant-type: <authorization-grant-type>
  opene:
  	username: <opene-username>
  	password: <opene-password>
  	base-url: <opene-url>
  	connect-timeout: 5000
  	read-timeout: 30000
  	oauth2:
      token-url: <token-url>
      authorization-grant-type: <authorization-grant-type>
```

- **Scheduler Settings**

```yaml
scheduler:
  opene:
    cron:
      expression: <cron-expression>
  fallout:
    cron:
      expression: <cron-expression>
falloutreport:
  recipients: <list-of-recipients>
  sender: <sender-email-address>
  sender-name: <sender-name>
  fallout-mail-template:
  	subject: <email-subject>
  	html-prefix: <html-prefix>
  	body-prefix: <body-prefix>
  	list-prefix: <html-suffix>
  	list-item: <html-list-item>
  	list-suffix: <html-list-suffix>
  	body-suffix: <body-suffix>
  	html-suffix: <html-suffix>
```

### Database Initialization

The project is set up with [Flyway](https://github.com/flyway/flyway) for database migrations. Flyway is disabled by
default so you will have to enable it to automatically populate the database schema upon application startup.

```yaml
spring:
  flyway:
    enabled: true
```

- **No additional setup is required** for database initialization, as long as the database connection settings are
  correctly configured.

### Additional Notes

- **Application Profiles:**

  Use Spring profiles (`dev`, `prod`, etc.) to manage different configurations for different environments.

- **Logging Configuration:**

  Adjust logging levels if necessary.

## Contributing

Contributions are welcome! Please
see [CONTRIBUTING.md](https://github.com/Sundsvallskommun/.github/blob/main/.github/CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the [MIT License](LICENSE).

## Status

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-billing-data-collector&metric=alert_status)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-billing-data-collector)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-billing-data-collector&metric=reliability_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-billing-data-collector)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-billing-data-collector&metric=security_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-billing-data-collector)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-billing-data-collector&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-billing-data-collector)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-billing-data-collector&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-billing-data-collector)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-billing-data-collector&metric=bugs)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-billing-data-collector)

## 

Copyright (c) 2024 Sundsvalls kommun
