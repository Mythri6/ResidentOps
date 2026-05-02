-- ============================================================
--  ResidentOps — Full SQL Schema + Seed Data
--  Run this ONCE manually in MySQL before starting the app,
--  OR let Spring Boot auto-create tables via ddl-auto=update
--  and then run only the INSERT section.
-- ============================================================

CREATE DATABASE IF NOT EXISTS residentops;
USE residentops;

-- ── Users ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        ENUM('RESIDENT','COMMITTEE_ADMIN','VENDOR','AUDITOR') NOT NULL,
    apartment_no VARCHAR(20),
    owner_type  ENUM('OWNER','TENANT') DEFAULT NULL,
    service_type VARCHAR(100) DEFAULT NULL,
    rating      DOUBLE DEFAULT 0.0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── Maintenance Requests ─────────────────────────────────
CREATE TABLE IF NOT EXISTS maintenance_requests (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    category     ENUM('PLUMBING','ELECTRICAL','CIVIL','CLEANING','SECURITY','OTHER') NOT NULL,
    priority     ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL,
    status       ENUM('RAISED','ASSIGNED','IN_PROGRESS','WAITING_FOR_PARTS','RESOLVED','ESCALATED','CLOSED','REJECTED') DEFAULT 'RAISED',
    raised_by    BIGINT NOT NULL,
    assigned_to  BIGINT DEFAULT NULL,
    sla_deadline DATETIME DEFAULT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    closed_at    DATETIME DEFAULT NULL,
    work_summary TEXT DEFAULT NULL,
    cost_incurred DOUBLE DEFAULT 0.0,
    FOREIGN KEY (raised_by) REFERENCES users(id),
    FOREIGN KEY (assigned_to) REFERENCES users(id)
);

-- ── Feedback ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS feedback (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    given_by   BIGINT NOT NULL,
    rating     INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (request_id) REFERENCES maintenance_requests(id),
    FOREIGN KEY (given_by) REFERENCES users(id)
);

-- ── Complaints ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS complaints (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    complaint_type ENUM('NOISE','PARKING','PET','HARASSMENT','MISUSE','OTHER') NOT NULL,
    status       ENUM('FILED','UNDER_REVIEW','ESCALATED','RESOLVED','CLOSED') DEFAULT 'FILED',
    filed_by     BIGINT NOT NULL,
    against_user BIGINT DEFAULT NULL,
    evidence_url VARCHAR(500) DEFAULT NULL,
    resolution   TEXT DEFAULT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at  DATETIME DEFAULT NULL,
    FOREIGN KEY (filed_by) REFERENCES users(id),
    FOREIGN KEY (against_user) REFERENCES users(id)
);

-- ── Polls ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS polls (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    question    VARCHAR(500) NOT NULL,
    description TEXT,
    created_by  BIGINT NOT NULL,
    quorum      INT NOT NULL DEFAULT 5,
    status      ENUM('OPEN','CLOSED','INCONCLUSIVE') DEFAULT 'OPEN',
    deadline    DATETIME NOT NULL,
    result      VARCHAR(200) DEFAULT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- ── Votes ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS votes (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    poll_id    BIGINT NOT NULL,
    voted_by   BIGINT NOT NULL,
    choice     ENUM('YES','NO','ABSTAIN') NOT NULL,
    voted_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_vote (poll_id, voted_by),
    FOREIGN KEY (poll_id) REFERENCES polls(id),
    FOREIGN KEY (voted_by) REFERENCES users(id)
);

-- ── Expenses ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS expenses (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    amount       DOUBLE NOT NULL,
    category     ENUM('MAINTENANCE','SALARY','UTILITIES','SECURITY','REPAIR','OTHER') NOT NULL,
    approved_by  BIGINT NOT NULL,
    vendor_id    BIGINT DEFAULT NULL,
    request_id   BIGINT DEFAULT NULL,
    receipt_url  VARCHAR(500) DEFAULT NULL,
    notes        TEXT,
    expense_date DATE NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (approved_by) REFERENCES users(id),
    FOREIGN KEY (vendor_id) REFERENCES users(id),
    FOREIGN KEY (request_id) REFERENCES maintenance_requests(id)
);

-- ── Notices ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notices (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    body         TEXT NOT NULL,
    notice_type  ENUM('GENERAL','WATER_CUT','POWER_CUT','EMERGENCY','MEETING','EVENT') NOT NULL,
    posted_by    BIGINT NOT NULL,
    is_emergency BOOLEAN DEFAULT FALSE,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (posted_by) REFERENCES users(id)
);

-- ── Audit Logs ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_logs (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    action       VARCHAR(200) NOT NULL,
    entity_type  VARCHAR(100) NOT NULL,
    entity_id    BIGINT NOT NULL,
    performed_by BIGINT NOT NULL,
    details      TEXT,
    performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (performed_by) REFERENCES users(id)
);

-- ============================================================
--  Seed Data
-- ============================================================

INSERT INTO users (name, email, password, role, apartment_no, owner_type) VALUES
('Arun Sharma',     'arun@resident.com',   'pass123', 'RESIDENT',         'A-101', 'OWNER'),
('Priya Nair',      'priya@resident.com',  'pass123', 'RESIDENT',         'B-202', 'TENANT'),
('Ravi Kumar',      'ravi@admin.com',      'pass123', 'COMMITTEE_ADMIN',  NULL,    NULL),
('Sunita Reddy',    'sunita@admin.com',    'pass123', 'COMMITTEE_ADMIN',  NULL,    NULL),
('Ramesh Plumber',  'ramesh@vendor.com',   'pass123', 'VENDOR',           NULL,    NULL),
('Suresh Electric', 'suresh@vendor.com',   'pass123', 'VENDOR',           NULL,    NULL),
('Audit Officer',   'auditor@ops.com',     'pass123', 'AUDITOR',          NULL,    NULL);

UPDATE users SET service_type = 'PLUMBING'    WHERE email = 'ramesh@vendor.com';
UPDATE users SET service_type = 'ELECTRICAL'  WHERE email = 'suresh@vendor.com';

INSERT INTO maintenance_requests (title, description, category, priority, status, raised_by, assigned_to, sla_deadline, cost_incurred) VALUES
('Bathroom tap leaking',   'Tap in master bathroom dripping continuously',  'PLUMBING',   'HIGH',   'ASSIGNED',    1, 5, DATE_ADD(NOW(), INTERVAL 4 HOUR),   0),
('Light not working',      'Hall light flickering and died',                 'ELECTRICAL', 'MEDIUM', 'IN_PROGRESS', 2, 6, DATE_ADD(NOW(), INTERVAL 24 HOUR),  0),
('Lift out of order',      'Lift stuck between floor 2 and 3',              'CIVIL',      'CRITICAL','ESCALATED',  1, NULL, DATE_ADD(NOW(), INTERVAL 2 HOUR), 0),
('Common area cleaning',   'Lobby needs cleaning urgently',                 'CLEANING',   'LOW',    'RAISED',      2, NULL, DATE_ADD(NOW(), INTERVAL 48 HOUR), 0);

INSERT INTO complaints (title, description, complaint_type, status, filed_by) VALUES
('Loud music after midnight', 'Flat C-301 plays loud music after 12am every weekend', 'NOISE', 'FILED', 1),
('Parking slot occupied',     'My assigned parking B-12 occupied by unknown vehicle',  'PARKING', 'UNDER_REVIEW', 2);

INSERT INTO polls (question, description, created_by, quorum, status, deadline) VALUES
('Install CCTV cameras in all corridors?',
 'Proposal to install 8 CCTV cameras covering all corridors and entrance. Estimated cost: Rs 45,000',
 3, 5, 'OPEN', DATE_ADD(NOW(), INTERVAL 7 DAY)),
('Repaint building exterior?',
 'Annual repainting of building exterior. Contractor quote: Rs 1,20,000',
 3, 5, 'OPEN', DATE_ADD(NOW(), INTERVAL 14 DAY));

INSERT INTO votes (poll_id, voted_by, choice) VALUES
(1, 1, 'YES'),
(1, 2, 'YES');

INSERT INTO expenses (title, amount, category, approved_by, vendor_id, expense_date, notes) VALUES
('Generator fuel refill',     3500.00, 'UTILITIES',    3, NULL, CURDATE(), 'Monthly diesel refill'),
('Security guard salary',    15000.00, 'SALARY',       3, NULL, CURDATE(), 'March salary'),
('Plumbing repair - Block A',  800.00, 'MAINTENANCE',  3, 5,   CURDATE(), 'Tap replacement flat A-101');

INSERT INTO notices (title, body, notice_type, posted_by, is_emergency) VALUES
('Water supply interruption',
 'Water supply will be interrupted on Saturday 9am-1pm due to tank cleaning. Please store water in advance.',
 'WATER_CUT', 3, FALSE),
('Society meeting - April 20',
 'Monthly committee meeting scheduled for April 20 at 6pm in the community hall. All residents are requested to attend.',
 'MEETING', 3, FALSE),
('Gas leak alert',
 'Suspected gas leak in Block B ground floor. All residents please evacuate immediately and call 101.',
 'EMERGENCY', 3, TRUE);

-- ── NEW TABLES (added in v3) ──────────────────────────────
CREATE TABLE IF NOT EXISTS flat_members (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    member_name VARCHAR(100) NOT NULL,
    phone_no    VARCHAR(15) DEFAULT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS flat_update_requests (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    resident_id      BIGINT NOT NULL,
    new_member_name  VARCHAR(100) NOT NULL,
    new_member_phone VARCHAR(15) DEFAULT NULL,
    proof_filename   VARCHAR(255) DEFAULT NULL,
    status           ENUM('PENDING','UNDER_VERIFICATION','APPROVED','REJECTED') DEFAULT 'UNDER_VERIFICATION',
    admin_remarks    TEXT DEFAULT NULL,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at     DATETIME DEFAULT NULL,
    FOREIGN KEY (resident_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS vendor_concerns (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_id           BIGINT NOT NULL,
    concern_type        ENUM('WORK_RELATED','LEAVE','OTHERS') NOT NULL,
    related_request_id  BIGINT DEFAULT NULL,
    description         TEXT DEFAULT NULL,
    leave_from          DATE DEFAULT NULL,
    leave_to            DATE DEFAULT NULL,
    leave_reason        TEXT DEFAULT NULL,
    status              ENUM('PENDING','APPROVED','REJECTED','DROPPED') DEFAULT 'PENDING',
    admin_message       TEXT DEFAULT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at        DATETIME DEFAULT NULL,
    FOREIGN KEY (vendor_id) REFERENCES users(id),
    FOREIGN KEY (related_request_id) REFERENCES maintenance_requests(id)
);

CREATE TABLE IF NOT EXISTS password_reset_requests (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    status       VARCHAR(20) DEFAULT 'PENDING',
    force_change BOOLEAN DEFAULT FALSE,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME DEFAULT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ── New columns for users table ──────────────────────────
ALTER TABLE users ADD COLUMN IF NOT EXISTS block_no VARCHAR(10) DEFAULT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS floor_no VARCHAR(10) DEFAULT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_no VARCHAR(15) DEFAULT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS force_password_change BOOLEAN DEFAULT FALSE;

-- Update existing residents with block/floor
UPDATE users SET block_no='A', floor_no='1', phone_no='+91-9876543210' WHERE email='arun@resident.com';
UPDATE users SET block_no='B', floor_no='2', phone_no='+91-9876543211' WHERE email='priya@resident.com';

-- ── Additional Vendors ────────────────────────────────────
INSERT INTO users (name, email, password, role, service_type, phone_no) VALUES
('Kavitha Housekeeper', 'kavitha@vendor.com',  'pass123', 'VENDOR', 'HOUSEKEEPING', '+91-9000000001'),
('Meena Housekeeper',   'meena@vendor.com',    'pass123', 'VENDOR', 'HOUSEKEEPING', '+91-9000000002'),
('Raju Security',       'raju@vendor.com',     'pass123', 'VENDOR', 'SECURITY',     '+91-9000000003'),
('Vikram Security',     'vikram@vendor.com',   'pass123', 'VENDOR', 'SECURITY',     '+91-9000000004'),
('Mohan Civil',         'mohan@vendor.com',    'pass123', 'VENDOR', 'CIVIL',        '+91-9000000005'),
('Geetha Cleaning',     'geetha@vendor.com',   'pass123', 'VENDOR', 'CLEANING',     '+91-9000000006'),
('Arjun Pest Control',  'arjun@vendor.com',    'pass123', 'VENDOR', 'PEST_CONTROL', '+91-9000000007');

-- ── Seed flat members for Arun's flat ────────────────────
INSERT INTO flat_members (user_id, member_name, phone_no) VALUES
(1, 'Arun Sharma Sr.', '+91-9876540000'),
(1, 'Arun Wife',       '+91-9876540001');
