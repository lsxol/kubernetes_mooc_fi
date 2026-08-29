--liquibase formatted sql
--changeset danielkazmierczak:1
CREATE TABLE counter (
    id int PRIMARY KEY,
    value BIGINT NOT NULL);
Insert into counter (id, value) values (1, 0);