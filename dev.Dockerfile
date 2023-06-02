FROM amazoncorretto:17.0.6-al2023

RUN yum update -y

RUN yum install -y nodejs-1:18.12.1-1.amzn2023.0.3

RUN npm install yarn -g
