-- Seed data for the VMS demo.

-- Users. password_md5 = unsalted MD5 (DEMO-VULN weak crypto). Plaintext in comments.
INSERT INTO users (username, password_md5, email, role) VALUES
  ('alice.admin',     '0192023a7bbd73250516f069df18b500', 'alice.admin@workforce-demo.io',     'ADMIN'),      -- admin123
  ('bob.approver',    '0b4c8ce4e23b8ae56df175f78a246bc0', 'bob.approver@workforce-demo.io',    'APPROVER'),   -- approve123
  ('carol.recruiter', '1aa7649c400c7c2490ba46970d0bf092', 'carol.recruiter@workforce-demo.io', 'RECRUITER'),  -- recruit123
  ('dave.vendor',     '6c6e1464695ec20feb0b2a633f9cf27b', 'dave.vendor@workforce-demo.io',     'VENDOR');     -- vendor123

-- Vendors
INSERT INTO vendors (name, contact_email, logo_url, status) VALUES
  ('Apex Staffing Group',   'ops@apexstaffing.example',   'https://logo.example/apex.png',   'ACTIVE'),
  ('BlueRiver Talent',      'contact@blueriver.example',  'https://logo.example/blueriver.png', 'ACTIVE'),
  ('Contingent Partners',   'hello@contingent.example',   'https://logo.example/cp.png',     'ACTIVE'),
  ('DeltaForce Contractors','info@deltaforce.example',    'https://logo.example/delta.png',  'SUSPENDED');

-- Workers
INSERT INTO workers (name, title, vendor_id, status, bill_rate) VALUES
  ('Ada Lovelace',    'Senior Data Engineer',   1, 'ACTIVE',   145.00),
  ('Alan Turing',     'Platform Architect',     1, 'ACTIVE',   165.00),
  ('Grace Hopper',    'Engineering Manager',    2, 'ACTIVE',   150.00),
  ('Katherine Johnson','QA Analyst',            2, 'ACTIVE',    95.00),
  ('Linus Pauling',   'Security Engineer',      3, 'ACTIVE',   155.00),
  ('Marie Curie',     'DevOps Engineer',        3, 'ON_LEAVE', 130.00),
  ('Nikola Tesla',    'Electrical Contractor',  4, 'TERMINATED', 120.00);

-- Timesheets
INSERT INTO timesheets (worker_id, week_ending, hours, status) VALUES
  (1, '2026-08-29', 40.0, 'APPROVED'),
  (1, '2026-09-05', 38.5, 'SUBMITTED'),
  (2, '2026-08-29', 40.0, 'APPROVED'),
  (3, '2026-08-29', 42.0, 'SUBMITTED'),
  (4, '2026-08-29', 40.0, 'REJECTED'),
  (5, '2026-09-05', 40.0, 'SUBMITTED');

-- Approvals
INSERT INTO approvals (timesheet_id, approver, decision, comments) VALUES
  (1, 'bob.approver', 'APPROVED', 'Looks good'),
  (3, 'bob.approver', 'APPROVED', 'Approved for billing'),
  (5, 'bob.approver', 'REJECTED', 'Hours exceed contract cap');
