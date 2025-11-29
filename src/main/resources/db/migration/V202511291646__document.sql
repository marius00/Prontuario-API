CREATE TABLE `document`
(
    `id`           bigint unsigned NOT NULL AUTO_INCREMENT,
    `number`       VARCHAR(32)  NOT NULL COMMENT 'Document number',
    `name`         VARCHAR(256) NOT NULL COMMENT 'Pacient name',
    `observations` VARCHAR(256) NULL DEFAULT NULL COMMENT 'Additional observations',
    `type`         ENUM('FICHA', 'PRONTUARIO') NOT NULL,
    `sector`       VARCHAR(32)  NOT NULL COMMENT 'Where is it now',
    `created_at`   bigint       NOT NULL,
    `modified_at`  bigint DEFAULT NULL,
    `deleted_at`   bigint DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `number`,
    FOREIGN KEY (sector) REFERENCES sector (name)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_danish_ci;
