CREATE TABLE `sector`
(
    `name`             VARCHAR(32) NOT NULL,
    `code`             VARCHAR(4) NULL DEFAULT NULL,
    `created_at`        bigint      NOT NULL,
    `modified_at`       bigint               DEFAULT NULL,
    `deleted_at`        bigint               DEFAULT NULL,
    PRIMARY KEY (`name`)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_danish_ci;

INSERT INTO `sector` (name, code, created_at, modified_at, deleted_at)
VALUES ('Administração', 'ADM', UNIX_TIMESTAMP(), null, null);


INSERT INTO `sector` (name, code, created_at, modified_at, deleted_at)
VALUES ('Outro Setor', 'OUT', UNIX_TIMESTAMP(), null, null);
