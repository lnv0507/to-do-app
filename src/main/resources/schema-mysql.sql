DROP TABLE IF EXISTS SPRING_AI_CHAT_MEMORY;

CREATE TABLE SPRING_AI_CHAT_MEMORY (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY, -- Fix lỗi Field 'id' doesn't have a default value
                                       conversation_id VARCHAR(36) NOT NULL,
                                       content TEXT,
                                       metadata JSON NULL,                   -- Thêm NULL để tránh lỗi strict mode
                                       type VARCHAR(100),
                                       `timestamp` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                       INDEX idx_conversation_id (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
