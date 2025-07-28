# Fake SMTP Server
[![Build Status](https://github.com/gessnerfl/fake-smtp-server/workflows/CI%2FCD/badge.svg)](https://github.com/gessnerfl/fake-smtp-server/workflows/CI%2FCD/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=de.gessnerfl.fake-smtp-server&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=de.gessnerfl.fake-smtp-server)

*Simple SMTP Server which stores all received emails in an in-memory database and renders the emails in a web interface*

## Introduction

The Fake SMTP Server is a simple SMTP server which is designed for development purposes. The server collects all
received emails, stores the emails in an in-memory database and provides access to the emails via a web interface.

There is no POP3 or IMAP interface included by intention. The basic idea of this software is that it is used during 
development to configure the server as target mail server. Instead of sending the emails to a real SMTP server which 
would forward the mails to the target recipient or return with mail undelivery for test email addresses (e.g. 
@example.com) the server just accepts all mails, stores them in the database so that they can be rendered in the UI. 
This allows you to use any test mail address and check the sent email in the web application of the Fake SMTP Server.

The server store a configurable maximum number of emails.
If the maximum number of emails is exceeded old emails will be deleted to avoid that the system consumes too much memory.

The server is also provided as docker image on docker hub [gessnerfl/fake-smtp-server](https://hub.docker.com/r/gessnerfl/fake-smtp-server/).
To change configuration parameters the corresponding configuration values have to be specified as environment variables for the docker container.
For details check the Spring Boot (http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-external-config)
and docker documentation (https://docs.docker.com/engine/reference/run/#env-environment-variables).

# Running Fake SMTP Server locally

> [!NOTE]  
> Starting with version 2.2.0 Java 21 is required to run Fake SMTP Server. 

> [!NOTE]  
> Starting with version 2.0.0 Java 17 is required to run Fake SMTP Server.

## Run from released JAR files

1. Download the latest `fake-smtp-server-<version>.jar` from 
[https://github.com/gessnerfl/fake-smtp-server/releases/latest](https://github.com/gessnerfl/fake-smtp-server/releases/latest)
2. Copy the file into the desired target folder
3. Execute the following command from the folder where the JAR file is located:
   
```
java -jar fake-smtp-server-<version>.jar
```

## Run from sources

In order to run this application locally from sources, execute:

    ./gradlew bootRun

Afterwards, the web interface is be availabe under http://localhost:8080.

# Configuration

As the application is based on Spring Boot the same rules applies to the configuration as described in the Spring Boot 
Documentation (http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-external-config).

The configuration file application.yaml can be placed next to the application jar, in a sub-directory config or 
in any other location when specifying the location with the parameter `-Dspring.config.location=<path to config file>`.

All configuration parameters can also be passed as environment variables using uppercase characters and underscores as 
separators such as `SERVER_PORT` or `MANAGEMENT_SERVER_PORT`.

The following paragraphs describe the application specific resp. pre-defined configuration parameters.

## Fake SMTP Server
The following snippet shows the configuration of a fake smtp server with its default values.

```yaml
fakesmtp:
  #The SMTP Server Port used by the Fake SMTP Server
  port: 8025

  #The binding address of the Fake SMTP Server; Bound to all interfaces by default / no value
  bindAddress: 127.0.0.1

  #List of recipient addresses which should be blocked/rejected
  blockedRecipientAddresses:
    - blocked@example.com
    - foo@eample.com

  #List of sender email addresses to ignore, as a comma-separated list of regex expressions.
  filteredEmailRegexList: john@doe\\.com,.*@google\\.com ; empty by default

  #Optional configuration option to specify the maximum allowed message size. The size can be 
  #defined using Spring Boot DataSize value type - https://docs.spring.io/spring-boot/docs/2.1.9.RELEASE/reference/html/boot-features-external-config.html#boot-features-external-config-conversion-datasize.
  #Default: no limit
  maxMessageSize: 10MB

  #Configure if TLS is required to connect to the SMTP server. Defaults to false. See TLS section below
  requireTLS: false

  #When set to true emails will be forwarded to a configured target email system. Therefore
  #the spring boot mail system needs to be configured. See also 
  # https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-email
  forwardEmails: false
```
    
### Authentication
Optionally authentication can be turned on. Configuring authentication does not mean the authentication is enforced. It
just allows you to test PLAIN and LOGIN SMTP Authentication against the server instance.

```yaml
fakesmtp:
  authentication:
    #Username of the client to be authenticated
    username: myuser
    #Password of the client to be authenticated
    password: mysecretpassword 
```


### TLS
Optionally TLS can be activated. To configure TLS support, a trust store needs to be provided 
containing the TLS certificate used by the FakeSMTP Server.

```yaml
fakesmtp:
  # true when TLS is mandatory otherwise TLS is optional
  requireTLS: true
  #configuration of the truststore to enable support for TLS.
  tlsKeystore:
    location: /path/to/truststore.p12
    password: changeit
    type: PKCS12 # or JKS
```
           
### Data Retention Settings

#### Emails

To keep memory resources under control, there is a parallel process that deletes the oldest emails considering the maximum number of emails to retain and the time span to periodically recheck this maximum number of emails, controlling also the initial time to wait to start this parallel process. The default values are:

- maxNumberEmails: 100
- fixedDelay: 300000 # 5 minutes
- initialDelay: 60000 # 1 minute

```yaml
fakesmtp:
  persistence:
    maxNumberEmails:
      emails:
        #max numbers of most recent emails to retain and not deleted by the parallel process
        maxNumberEmails: 10
        # configuration settings of the background process (timer) responsible to delete the oldest emails
        emailDataRetentionTimer:
          #each 5 minutes from 'initialDelay' (see below), the parallel process will check if the deletion is necessary
          fixedDelay: 300000
          #each 'initialDelay' (see above)  after 2 minutes from the start, the parallel process will start checking if the deletion is necessary
          initialDelay: 120000
```

## Web UI
The following snippet shows the pre-defined web application configuration

```yaml
#Port of the web interface
server:
  port: 8080

#Port of the http management api
management:
  server:
    port: 8081 
```

### Web UI Authentication

You can optionally enable Basic Authentication for the web interface and REST API. When authentication is enabled, users will need to log in to access the web interface and API endpoints.

To enable authentication, set the username and password in the application.yml file:

```yaml
fakesmtp:
  authentication:
    # Username for web UI and API authentication
    username: admin
    # Password for web UI and API authentication
    password: password
```

You can also set these values using environment variables:

```
FAKESMTP_AUTH_USERNAME=admin
FAKESMTP_AUTH_PASSWORD=password
```

If both username and password are not set, authentication will be disabled and the web interface and API endpoints will be accessible without authentication.

When authentication is enabled:
- The web interface will show a custom login form
- API endpoints will require Basic Authentication
- A logout button will be available in the navigation bar
    

## REST API

Documentation of exposed services is available at:
    
    localhost:8080/swagger-ui.html

## Developpment Environment

This requires to have docker installed.
If you need to implement a new feature, you will probably need an correct JDK version setup in an environement.

```sh
sh/dev
```

Then, in the dev container started by the command above, you can use various commands. 
The following commands should be the most common ones:
```bash
sh gradlew test
sh gradlew test --tests '*EmailRepositoryIntegration*' --info
sh gradlew build
```

Run UI & Backend tests
```bash
sh/test
```

Build UI & Backend
```bash
sh/build
```

Run app (UI & Backend)
```bash
sh/run
```

### Build & Push a new development docker image

To update/change the development image, update the `dev.Dockerfile`, dont forget to change the version in the `dev-image-tag` file and edit the registery if needed.
```bash
sh/push-dev-image
```
