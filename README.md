[![Build Status](https://travis-ci.org/garystafford/voter-service.svg?branch=master)](https://travis-ci.org/garystafford/voter-service) [![Dependencies](https://app.updateimpact.com/badge/817200262778327040/voter-service.svg?config=compile)](https://app.updateimpact.com/latest/817200262778327040/voter-service)

# Voter Service

## Introduction

The Voter [Spring Boot](https://projects.spring.io/spring-boot/) RESTful Web Service, backed by [MongoDB](https://www.mongodb.com/), is used for DevOps-related training and testing. The Voter service exposes several HTTP API endpoints, listed below. API users can review a list candidates, submit a vote, view voting results, and inspect technical information about the running service. API users can also create random voting data by calling the `/simulation` endpoint.

## Quick Start for Local Development

The Voter service requires MongoDB to be pre-installed and running locally, on port `27017`. To clone, build, test, and run the Voter service, locally:

```bash
git clone https://github.com/garystafford/voter-service.git
cd voter-service
./gradlew clean cleanTest build
java -jar build/libs/voter-service-0.2.0.jar
```

## Service Endpoints

By default, the service runs on `localhost`, port `8099`. By default, the service looks for MongoDB on `localhost`, port `27017`.

Purpose                                                                                                                  | Method  | Endpoint
------------------------------------------------------------------------------------------------------------------------ | :------ | :----------------------------------------------------
Create Random Sample Data                                                                                                | GET     | [/simulation](http://localhost:8099/simulation)
List Candidates                                                                                                          | GET     | [/candidates](http://localhost:8099/candidates)
Submit Vote                                                                                                              | POST    | [/votes](http://localhost:8099/votes)
View Voting Results                                                                                                      | GET     | [/results](http://localhost:8099/results)
View Total Votes                                                                                                         | GET     | [/results/votes](http://localhost:8099/results/votes)
View Winner(s)                                                                                                           | GET     | [/winners](http://localhost:8099/winners)
View Winning Vote Count                                                                                                  | GET     | [/winners/votes](http://localhost:8091/winners/votes)
Service Info                                                                                                             | GET     | [/info](http://localhost:8099/info)
Service Health                                                                                                           | GET     | [/health](http://localhost:8099/health)
Service Metrics                                                                                                          | GET     | [/metrics](http://localhost:8099/metrics)
Other [Spring Actuator](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#production-ready) endpoints | GET     | `/actuator`, `/mappings`, `/env`, `/configprops`, etc.
Other [HATEOAS](https://spring.io/guides/gs/rest-hateoas) endpoints for `/votes`                                         | Various | DELETE, PATCH, PUT, page sort, size, etc.

## Voting

Submitting a new vote, requires an HTTP `POST` request to the `/votes` endpoint, as follows:

HTTPie

```text
http POST http://localhost:8099/votes candidate="Jill Stein"
```

cURL

```text
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{ "candidate": "Jill Stein" }' \
  "http://localhost:8099/votes"
```

wget

```text
wget --method POST \
  --header 'content-type: application/json' \
  --body-data '{ "candidate": "Jill Stein" }' \
  --no-verbose \
  --output-document - http://localhost:8099/votes
```

## Sample Output

Using [HTTPie](https://httpie.org/) command line HTTP client.

`http http://localhost:8099/candidates`

```json
{
    "candidates": [
        "Chris Keniston",
        "Darrell Castle",
        "Donald Trump",
        "Gary Johnson",
        "Hillary Clinton",
        "Jill Stein"
    ]
}
```

`http http://localhost:8099/simulation`

```json
{
    "message": "random simulation data created"
}
```

`http http://localhost:8099/results`

```json
{
    "results": [
        {
            "candidate": "Gary Johnson",
            "votes": 20
        },
        {
            "candidate": "Hillary Clinton",
            "votes": 15
        },
        {
            "candidate": "Donald Trump",
            "votes": 11
        },
        {
            "candidate": "Jill Stein",
            "votes": 8
        },
        {
            "candidate": "Chris Keniston",
            "votes": 3
        },
        {
            "candidate": "Darrell Castle",
            "votes": 2
        }
    ]
}
```

`http http://localhost:8099/results/votes`

```json
{
    "votes": 59
}
```

`http http://localhost:8099/winners`

```json
{
    "results": [
        {
            "candidate": "Gary Johnson",
            "votes": 20
        }
    ]
}
```

`http http://localhost:8099/winners/votes`

```json
{
    "votes": 20
}
```

`http POST http://localhost:8099/votes vote="Jill Stein"`

```json
{
    "_links": {
        "self": {
            "href": "http://localhost:8099/votes/58279bda909a021142712fe7"
        },
        "vote": {
            "href": "http://localhost:8099/votes/58279bda909a021142712fe7"
        }
    },
    "vote": "Jill Stein"
}
```

## Continuous Integration

The project's source code is continuously built and tested on every commit to [GitHub](https://github.com/garystafford/voter-service), using [Travis CI](https://travis-ci.org/garystafford/voter-service). If all unit tests pass, the resulting Spring Boot JAR is pushed to the `artifacts` branch of the [voter-service-artifacts](https://github.com/garystafford/voter-service-artifacts) GitHub repository. The JAR's filename is incremented with each successful build (i.e. `voter-service-0.2.10.jar`).

![Vote Continuous Integration Pipeline](Voter-CI.png)

## Spring Profiles

The Voter service includes (3) Spring Boot Profiles, in a multi-profile YAML document: `src/main/resources/application.yml`. The profiles are `default`, `aws-production`, and `docker-production`. You will need to ensure your MongoDB instance is available at that `host` address and port of the profile you choose, or you may override the profile's properties.


```yaml
server:
  port: 8099
data:
  mongodb:
    host: localhost
    port: 27017
    database: voters
logging:
  level:
    root: INFO
info:
  java:
    source: ${java.version}
    target: ${java.version}
---
spring:
  profiles: aws-production
data:
  mongodb:
    host: 10.0.1.6
logging:
  level:
    root: WARN
---
spring:
  profiles: docker-production
data:
  mongodb:
    host: mongodb
logging:
  level:
    root: WARN
```

All profile property values may be overridden on the command line, or in a .conf file. For example, to start the Voter service with the `aws-production` profile, but override the `mongodb.host` value with a new host address, you might use the following command:

```bash
java -jar <name_of_the_jar_file> \
  --spring.profiles.active=aws-production \
  --spring.data.mongodb.host=<new_host_address>
  -Djava.security.egd=file:/dev/./urandom
```

# README

- [Spring Data MongoDB - Reference Documentation](http://docs.spring.io/spring-data/mongodb/docs/current/reference/html/)
- [Accessing MongoDB Data with REST](https://spring.io/guides/gs/accessing-mongodb-data-rest/)
- [Spring Boot Testing](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-testing)
- [Installing Spring Boot applications](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment-install.html#deployment-install)
- [Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html)
- [2016 Presidential Candidates](http://www.politics1.com/p2016.htm)
