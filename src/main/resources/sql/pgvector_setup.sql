-- ==========================================
-- PGVECTOR SETUP FOR GRABPIC BACKEND
-- ==========================================

-- 1. Install pgvector extension (run once per database)
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Create tables with proper vector columns
-- Note: These are the correct table definitions for pgvector

-- Face embeddings table with vector(512) column
CREATE TABLE IF NOT EXISTS td_face_embeddings (
    id BIGSERIAL PRIMARY KEY,
    asset_id BIGINT NOT NULL,
    embedding vector(512) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Create indexes for vector similarity search
-- HNSW index for fast approximate nearest neighbor search
CREATE INDEX IF NOT EXISTS td_face_embeddings_embedding_idx 
ON td_face_embeddings 
USING hnsw (embedding vector_cosine_ops);

-- 4. Create foreign key constraints
ALTER TABLE td_face_embeddings 
ADD CONSTRAINT IF NOT EXISTS fk_face_embeddings_asset 
FOREIGN KEY (asset_id) REFERENCES td_asset_details(id);

-- 5. Example vector similarity queries
-- Find similar faces (distance < 0.7):
-- SELECT asset_id, embedding <-> '[0.1,0.2,0.3,...]' as distance
-- FROM td_face_embeddings 
-- WHERE embedding <-> '[0.1,0.2,0.3,...]' < 0.7
-- ORDER BY embedding <-> '[0.1,0.2,0.3,...]'
-- LIMIT 20;

-- 6. Verify pgvector is working
SELECT version();
SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';
