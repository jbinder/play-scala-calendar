# Users schema

# --- !Ups

CREATE TABLE LOCATION (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    TITLE varchar(64) NOT NULL,
    ADDRESS varchar (255) NOT NULL,
    CITY varchar (32) NOT NULL,
    ZIP_CODE varchar (32) NOT NULL,
    STATE varchar (32) NOT NULL,
    COUNTRY varchar (2) NOT NULL,
    PRIMARY KEY (id)
);

# --- !Downs

DROP TABLE LOCATION;