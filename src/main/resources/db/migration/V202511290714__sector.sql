CREATE TABLE `sector`
(
    `name`             VARCHAR(32) NOT NULL,
    `created_at`        bigint      NOT NULL,
    `modified_at`       bigint               DEFAULT NULL,
    `deleted_at`        bigint               DEFAULT NULL,
    PRIMARY KEY (`name`)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_danish_ci;

-- "password"
INSERT INTO `sector` (name, created_at, modified_at, deleted_at)
VALUES ('Administração', UNIX_TIMESTAMP(), null, null);
