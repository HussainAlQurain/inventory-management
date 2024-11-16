--password is admin
INSERT INTO role (id, name) VALUES (1, 'ROLE_USER');
INSERT INTO role (id, name) VALUES (2, 'ROLE_ADMIN');
INSERT INTO role (id, name) VALUES (3, 'ROLE_SUPER_ADMIN');
INSERT INTO users (id, username, password, email, status, first_name, last_name, phone) VALUES (0, 'admin', '$2a$14$C2HvKTOQmGVMKZGQ0xa1NO8UUcRHoYgjESdZlEj51bZcSKye43Qdm', 'hussain.qurain@outlook.com', 'active', 'Hussain', 'Al-Qurain', '+966536071929');
INSERT INTO company (id, name, tax_id, phone, mobile, email, state, city, address, zip, add_purchased_items_to_favorites, logo, allowed_invoice_deviation, export_delivery_notes_as_bills)
VALUES (0, 'Company A', '3000000000000', '+966013555555', '+966555555555', 'hussain.qurain@outlook.com', 'khobar', 'khobar', 'khobar', '55555', TRUE, 'test.png', 3, TRUE);
INSERT INTO user_roles (user_id, role_id) VALUES (0, 1);
INSERT INTO user_roles (user_id, role_id) VALUES (0, 2);
INSERT INTO user_roles (user_id, role_id) VALUES (0, 3);
