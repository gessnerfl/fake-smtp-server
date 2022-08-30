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

**Note:** You need Java 11 installed to run Fake SMTP Server. 

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

Afterwards, the web interface is be availabe under http://localhost:5080.

# Configuration

As the application is based on Spring Boot the same rules applies to the configuration as described in the Spring Boot 
Documentation (http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-external-config).

The configuration file application.properties can be placed next to the application jar, in a sub-directory config or 
in any other location when specifying the location with the parameter `-Dspring.config.location=<path to config file>`.

The following paragraphs describe the application specific resp. pre-defined configuration parameters.

## Fake SMTP Server
The following snippet shows the configuration of a fake smtp server with its default values.
    
    #The SMTP Server Port used by the Fake SMTP Server
    fakesmtp.port=5025
    
    #The binding address of the Fake SMTP Server; Bound to all interfaces by default / no value
    fakesmtp.bindAddress
    
    #The maximum number of emails which should be stored in the database; Defualts to 100
    fakesmtp.persistence.maxNumberEmails=100  
    
    #List of sender email addresses to ignore, as a comma-separated list of regex expressions.
    fakesmtp.filteredEmailRegexList=john@doe\\.com,.*@google\\.com ; empty by default
    
    #When set to true emails will be forwarded to a configured target email system. Therefore
    #the spring boot mail system needs to be configured. See also 
    https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-email
    fakesmtp.forwardEmails=false
    
### Authentication
Optionally authentication can be turned on. Configuring authentication does not mean the authentication is enforced. It
just allows you to test PLAIN and LOGIN SMTP Authentication against the server instance.

    #Username of the client to be authenticated
    fakesmtp.authentication.username
    
    #Password of the client to be authenticated
    fakesmtp.authentication.password          

## Web UI
The following snippet shows the pre-defined web application configuration

    #Port of the web interface
    server.port=5080     
    
    #Port of the http management api
    management.server.port=5081 

## REST API

Documentation of exposed services is available at:
    
    localhost:5080/swagger-ui.html
