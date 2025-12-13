CREATE TABLE `push_subscriptions` (
                                         `id` BIGINT(20) NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                         `user_id` BIGINT(20) UNSIGNED NOT NULL,
                                         `endpoint` VARCHAR(4096) NOT NULL COLLATE 'utf8mb4_danish_ci',
                                         `p256dh` VARCHAR(4096) NOT NULL COLLATE 'utf8mb4_danish_ci',
                                         `auth` VARCHAR(4096) NOT NULL COLLATE 'utf8mb4_danish_ci',
                                         `created_at` BIGINT(20) NOT NULL,
                                         `modified_at` BIGINT(20) NULL DEFAULT NULL,
                                         `deleted_at` BIGINT(20) NULL DEFAULT NULL,
                                         CONSTRAINT FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
                                         INDEX (`deleted_at`)
)
    COLLATE='utf8mb4_danish_ci'
    ENGINE=InnoDB
;
