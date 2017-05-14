# Fake SMTP Server
[![Build Status](https://travis-ci.org/gessnerfl/fake-smtp-server.svg?branch=master)](https://travis-ci.org/gessnerfl/fake-smtp-server)

*Simple SMTP Server which stores all received emails in an in-memory database and renders the emails in a web interface*

## Introduction

The Fake SMTP Server is a simple SMTP server which is designed for development purposes. The server collects all
received emails, stores the emails in an in-memory database and provides access to the emails via a web interface.

There is no POP3 or IMAP interface included by intention. The basic idea of this software is that it is used during 
development to configure the server as target mail server. Instead of sending the emails to a real SMTP server which 
would forward the mails to the target recipient or return with mail undelivery for test email addresses (e.g. 
@example.com) the server just accepts all mails, stores them in the database so that they can be rendered in the UI. 
This allows you to use any test mail address and check the sent email in the web application of the Fake SMTP Server.

The server store a configurable maximum number of emails. If the maximum number of emails is exceeded old emails will
be deleted to avoid that the system consumes too much memory

# Configuration

As the application is based on Spring Boot the same rules applies to the configuration as described in the Spring Boot 
Documentation (http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-external-config).

The configuration file application.properties can be placed next to the application jar, in a sub-directory config or 
in any other location when specifying the location with the parameter `-Dspring.config.location=<path to config file>`.

The following paragraphs describe the application specific resp. pre-defined configuration parameters.

## Fake SMTP Server
The following snippet shows the configuration of a fake smtp server with its default values.

    fakesmtp.port=5025                        #The SMTP Server Port used by the Fake SMTP Server
    fakesmtp.bindAddress                      #The binding address of the Fake SMTP Server; By default it is bound to 
                                              #all interfaces as no value is defined
    fakesmtp.persistence.maxNumberEmails=100  #The maximum number of emails which should be stored in the database; 
                                              #default 100
    

### Authentication
Optionally authentication can be turned on. Configuring authentication does not mean the authentication is enforced. It
just allows you to test PLAIN and LOGIN SMTP Authentication against the server instance.

    fakesmtp.authentication.username          #Username of the client to be authenticated
    fakesmtp.authentication.password          #Password of the client to be authenticated

## Web UI
The following snippet shows the pre-defined web application configuration

    server.port=5080     #Port of the web interface
    management.port=5081 #Port of the http management api
