-- ============================================================================
-- V16: OWNER/STAFF Password Reset Token Storage and JWT Revocation Support
-- ============================================================================

-- 1. Add token_version to users for deterministic session revocation
ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 0;

-- 2. Create password_reset_tokens table
CREATE TABLE password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_prt_user_id (user_id),
    INDEX idx_prt_token_hash (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
