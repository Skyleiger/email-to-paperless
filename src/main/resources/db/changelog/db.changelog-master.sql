-- liquibase formatted sql

-- changeset skyleiger:1669580250645-1
CREATE SEQUENCE email_seq INCREMENT BY 50 START WITH 1;

-- changeset skyleiger:1669580250645-2
CREATE TABLE email
(
    id        BIGINT       NOT NULL,
    subject   VARCHAR(255) NULL,
    sent_date datetime     NULL,
    CONSTRAINT pk_email PRIMARY KEY (id)
);