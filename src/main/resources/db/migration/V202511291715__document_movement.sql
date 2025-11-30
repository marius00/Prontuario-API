CREATE TABLE `document_movement`
(
    `document_id` bigint unsigned NOT NULL,
    `user_id`     bigint unsigned NOT NULL,
    `from_sector` VARCHAR(32) NOT NULL,
    `to_sector`   VARCHAR(32) NOT NULL,
    PRIMARY KEY (`document_id`),
    FOREIGN KEY (document_id) REFERENCES document (id),
    FOREIGN KEY (user_id) REFERENCES user (id),
    FOREIGN KEY (from_sector) REFERENCES sector (name),
    FOREIGN KEY (to_sector) REFERENCES sector (name)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_danish_ci;



-- Sent to department
-- Sent by department
-- Send by user

-- Can be used to fetch both outbox and inbox
