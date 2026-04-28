CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(16) NOT NULL,
  password_change_required BOOLEAN NOT NULL,
  enabled BOOLEAN NOT NULL,
  created_at TIMESTAMP NOT NULL,
  password_updated_at TIMESTAMP NULL
);

CREATE TABLE question_banks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL UNIQUE,
  bank_type VARCHAR(64) NOT NULL,
  description VARCHAR(512) NULL,
  status VARCHAR(16) NOT NULL,
  default_duration_minutes INT NOT NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE questions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_bank_id BIGINT NOT NULL,
  type VARCHAR(32) NOT NULL,
  content TEXT NOT NULL,
  source_section VARCHAR(128) NULL,
  knowledge_area VARCHAR(128) NULL,
  has_answer BOOLEAN NOT NULL,
  answer_locked BOOLEAN NOT NULL,
  external_code VARCHAR(128) NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_questions_bank FOREIGN KEY (question_bank_id) REFERENCES question_banks (id)
);

CREATE TABLE question_options (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_id BIGINT NOT NULL,
  option_key VARCHAR(8) NOT NULL,
  content TEXT NOT NULL,
  CONSTRAINT fk_options_question FOREIGN KEY (question_id) REFERENCES questions (id)
);

CREATE TABLE question_answers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_id BIGINT NOT NULL UNIQUE,
  answer_text TEXT NOT NULL,
  explanation TEXT NULL,
  source VARCHAR(32) NOT NULL,
  locked BOOLEAN NOT NULL,
  CONSTRAINT fk_answers_question FOREIGN KEY (question_id) REFERENCES questions (id)
);

CREATE TABLE import_batches (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_bank_id BIGINT NOT NULL,
  importer_type VARCHAR(64) NOT NULL,
  file_sha256 VARCHAR(128) NOT NULL,
  question_count INT NOT NULL,
  report TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_import_bank FOREIGN KEY (question_bank_id) REFERENCES question_banks (id),
  CONSTRAINT uk_import_once UNIQUE (question_bank_id, importer_type, file_sha256)
);

CREATE TABLE exam_papers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_bank_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  duration_minutes INT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_papers_bank FOREIGN KEY (question_bank_id) REFERENCES question_banks (id)
);

CREATE TABLE exam_paper_questions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  paper_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  question_order INT NOT NULL,
  type VARCHAR(32) NOT NULL,
  content_snapshot TEXT NOT NULL,
  options_snapshot TEXT NULL,
  answer_snapshot TEXT NOT NULL,
  CONSTRAINT fk_paper_questions_paper FOREIGN KEY (paper_id) REFERENCES exam_papers (id),
  CONSTRAINT fk_paper_questions_question FOREIGN KEY (question_id) REFERENCES questions (id)
);

CREATE TABLE exam_attempts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_bank_id BIGINT NOT NULL,
  paper_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(24) NOT NULL,
  started_at TIMESTAMP NOT NULL,
  submitted_at TIMESTAMP NULL,
  score INT NOT NULL,
  total_questions INT NOT NULL,
  correct_questions INT NOT NULL,
  CONSTRAINT fk_attempts_bank FOREIGN KEY (question_bank_id) REFERENCES question_banks (id),
  CONSTRAINT fk_attempts_paper FOREIGN KEY (paper_id) REFERENCES exam_papers (id),
  CONSTRAINT fk_attempts_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE exam_attempt_answers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  attempt_id BIGINT NOT NULL,
  paper_question_id BIGINT NOT NULL,
  user_answer TEXT NOT NULL,
  correct BOOLEAN NOT NULL,
  CONSTRAINT fk_attempt_answers_attempt FOREIGN KEY (attempt_id) REFERENCES exam_attempts (id),
  CONSTRAINT fk_attempt_answers_paper_question FOREIGN KEY (paper_question_id) REFERENCES exam_paper_questions (id)
);

CREATE TABLE wrong_questions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_bank_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  wrong_count INT NOT NULL,
  mastered BOOLEAN NOT NULL,
  last_wrong_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_wrong_bank FOREIGN KEY (question_bank_id) REFERENCES question_banks (id),
  CONSTRAINT fk_wrong_question FOREIGN KEY (question_id) REFERENCES questions (id),
  CONSTRAINT fk_wrong_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT uk_wrong_user_question UNIQUE (question_bank_id, question_id, user_id)
);

CREATE TABLE question_marks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_bank_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  hard BOOLEAN NOT NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_marks_bank FOREIGN KEY (question_bank_id) REFERENCES question_banks (id),
  CONSTRAINT fk_marks_question FOREIGN KEY (question_id) REFERENCES questions (id),
  CONSTRAINT fk_marks_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT uk_mark_user_question UNIQUE (question_bank_id, question_id, user_id)
);

CREATE INDEX idx_questions_bank_answer ON questions (question_bank_id, has_answer);
CREATE INDEX idx_questions_bank_type ON questions (question_bank_id, type);
