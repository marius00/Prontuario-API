CREATE TABLE `push_subscriptions` (
                                      `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
                                      `user_id` BIGINT(20) UNSIGNED NOT NULL,
                                      `endpoint` TEXT NOT NULL COLLATE 'utf8mb4_danish_ci',
                                      `p256dh` TEXT NOT NULL COLLATE 'utf8mb4_danish_ci',
                                      `auth` TEXT NOT NULL COLLATE 'utf8mb4_danish_ci',
                                      `created_at` BIGINT(20) NOT NULL,
                                      `modified_at` BIGINT(20) NULL DEFAULT NULL,
                                      `deleted_at` BIGINT(20) NULL DEFAULT NULL,
                                      PRIMARY KEY (`id`) USING BTREE,
                                      UNIQUE INDEX `Index 3` (`user_id`, `endpoint`) USING HASH,
                                      INDEX `idx_push_subscriptions_user_id` (`user_id`) USING BTREE,
                                      INDEX `idx_push_subscriptions_deleted_at` (`deleted_at`) USING BTREE,
                                      CONSTRAINT `push_subscriptions_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
)
    COLLATE='utf8mb4_danish_ci'
    ENGINE=InnoDB
;
