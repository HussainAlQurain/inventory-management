--password is admin
INSERT INTO role (id, name) VALUES (1, 'ROLE_USER');
INSERT INTO role (id, name) VALUES (2, 'ROLE_ADMIN');
INSERT INTO role (id, name) VALUES (3, 'ROLE_SUPER_ADMIN');
INSERT INTO users (id, username, password, email, active) VALUES (0, 'admin', '$2a$14$C2HvKTOQmGVMKZGQ0xa1NO8UUcRHoYgjESdZlEj51bZcSKye43Qdm', 'hussain.qurain@outlook.com', TRUE);
INSERT INTO user_roles (user_id, role_id) VALUES (0, 1);
INSERT INTO user_roles (user_id, role_id) VALUES (0, 2);
INSERT INTO user_roles (user_id, role_id) VALUES (0, 3);
