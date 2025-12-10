ALTER TABLE `document_history`
ADD COLUMN `user_id` bigint unsigned NULL AFTER `document_id`,
ADD CONSTRAINT `fk_document_history_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`);

