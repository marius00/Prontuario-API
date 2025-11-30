CREATE TABLE `document_history`
(
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT,
    `document_id` bigint unsigned NOT NULL,
    `action`      ENUM('CREATED', 'SENT', 'RECEIVED', 'REJECTED', 'UPDATED', 'REQUESTED', 'DELETED') NOT NULL,
    `sector` VARCHAR(32) NOT NULL,
    `description`  VARCHAR(128) NOT NULL,
    `created_at`  bigint       NOT NULL,
    `modified_at` bigint DEFAULT NULL,
    `deleted_at`  bigint DEFAULT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (document_id) REFERENCES document (id),
    FOREIGN KEY (sector) REFERENCES sector (name)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_danish_ci;

INSERT INTO document_history (document_id, action, sector, description, created_at) VALUES
(1, 'CREATED', 'Administração', 'Document created at reception', UNIX_TIMESTAMP()),
(1, 'SENT', 'Administração', 'Document sent to CARDIOLOGY', UNIX_TIMESTAMP()),
(2, 'CREATED', 'Administração', 'Document created at cardiology', UNIX_TIMESTAMP()),
(2, 'REJECTED', 'Administração', 'Document rejected due to incomplete information', UNIX_TIMESTAMP()),
(3, 'CREATED', 'Administração', 'Document created at neurology', UNIX_TIMESTAMP());
