CREATE TABLE document_requests (
                                     id BIGINT NOT NULL AUTO_INCREMENT,
                                     document_id BIGINT unsigned NOT NULL,
                                     to_sector VARCHAR(255) NOT NULL,
                                     user_id BIGINT unsigned NOT NULL,
                                     reason VARCHAR(4096),
                                     created_at BIGINT NOT NULL,
                                     modified_at BIGINT,
                                     deleted_at BIGINT,
                                     PRIMARY KEY (id),
                                     FOREIGN KEY (document_id) REFERENCES document(id),
                                     FOREIGN KEY (user_id) REFERENCES user(id)
);

