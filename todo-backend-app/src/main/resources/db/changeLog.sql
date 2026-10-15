--liquibase formatted sql
--changeset danielkazmierczak:1
CREATE TABLE todo (
    id int PRIMARY KEY,
    value VARCHAR(255) NOT NULL);
    
--changeset danielkazmierczak:2
Create sequence todo_seq START WITH 1 INCREMENT BY 1;

--changeset danielkazmierczak:3
ALTER TABLE todo ADD COLUMN done BOOLEAN NOT NULL DEFAULT FALSE;
