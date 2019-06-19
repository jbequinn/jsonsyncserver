# Json Sync Server [![Build Status](https://travis-ci.org/jbequinn/jsonsyncserver.svg?branch=master)](https://travis-ci.org/jbequinn/jsonsyncserver)
Synchronization service for [Everdo](https://everdo.net/)

## How to run
* This service is intended to be run with Docker. An example of a docker-compose file would be:
```
version: "3.5"
services:
  db:
    image: mongo:4.0.10
    restart: unless-stopped
    environment:
      - MONGO_INITDB_ROOT_USERNAME=root
      - MONGO_INITDB_ROOT_PASSWORD=mypassword
    volumes:
      - type: volume
        source: db-data
        target: /data/db
      - type: volume
        source: db-config
        target: /data/configdb
    networks:
      - internal
  app:
    image: jbequinn/jsonsyncserver:9
    restart: unless-stopped
    depends_on:
      - db
    ports:
      - target: 443
        published: 8443
    environment:
      - api.key=my-everdo-key
      - mongo.username=root
      - mongo.password=mypassword
      # uncomment to change the logging level
      #- logging.level.com.jbequinn.jsonsyncserver=TRACE
    networks:
      - internal
networks:
  internal:
volumes:
  db-data:
  db-config:
```
