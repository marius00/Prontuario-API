ALTER TABLE document_movement
    ADD COLUMN `to_user` VARCHAR(32) NULL DEFAULT NULL AFTER `to_sector`,
    ADD FOREIGN KEY (to_user) REFERENCES user (login);
