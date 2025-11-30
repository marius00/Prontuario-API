CREATE TABLE `document`
(
    `id`           bigint unsigned NOT NULL AUTO_INCREMENT,
    `number`       VARCHAR(32)  NOT NULL UNIQUE COMMENT 'Document number',
    `name`         VARCHAR(256) NOT NULL COMMENT 'Pacient name',
    `observations` VARCHAR(256) NULL DEFAULT NULL COMMENT 'Additional observations',
    `type`         ENUM('FICHA', 'PRONTUARIO') NOT NULL,
    `sector`       VARCHAR(32)  NOT NULL COMMENT 'Where is it now',
    `user_id`       bigint unsigned NOT NULL,
    `created_at`   bigint       NOT NULL,
    `modified_at`  bigint DEFAULT NULL,
    `deleted_at`   bigint DEFAULT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (sector) REFERENCES sector (name),
    FOREIGN KEY (user_id) REFERENCES user (id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_danish_ci;

INSERT INTO document (number, name, observations, type, sector, created_at, user_id) VALUES
('1', 'John Doe', 'Initial consultation', 'FICHA', 'Administração', UNIX_TIMESTAMP(), 1),
('2', 'Jane Smith', 'Follow-up required', 'PRONTUARIO', 'Outro Setor', UNIX_TIMESTAMP(), 1),
('3', 'Alice Johnson', NULL, 'FICHA', 'Administração', UNIX_TIMESTAMP(), 1);
