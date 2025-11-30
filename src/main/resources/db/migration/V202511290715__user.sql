CREATE TABLE `user`
(
    `id`                bigint unsigned NOT NULL AUTO_INCREMENT,
    `login`             VARCHAR(32) NOT NULL,
    `password`          VARCHAR(256) NULL,
    `sector`            VARCHAR(32) NOT NULL,
    `role`              ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    `require_pwd_reset` TINYINT     NOT NULL DEFAULT 0,
    `created_at`        bigint      NOT NULL,
    `modified_at`       bigint               DEFAULT NULL,
    `deleted_at`        bigint               DEFAULT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (sector) REFERENCES sector (name),
    UNIQUE KEY (`login`, `sector`)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_danish_ci;

-- "password"
INSERT INTO `user` (login, `role`, password, sector, require_pwd_reset, created_at, modified_at, deleted_at)
VALUES ('marius', 'ADMIN', '$argon2i$v=19$m=16,t=2,p=1$Y2Rld3Fld3FlcXc$KVmGoGNoeeW0rvLeGYXPig', 'Administração', 0,
        UNIX_TIMESTAMP(), null, null);

INSERT INTO `user` (login, password, sector, require_pwd_reset, created_at, modified_at, deleted_at)
VALUES ('marius', '$argon2i$v=19$m=16,t=2,p=1$Y2Rld3Fld3FlcXc$KVmGoGNoeeW0rvLeGYXPig', 'Outro Setor', 0,
        UNIX_TIMESTAMP(), null, null);
