--password is admin
-- INSERT INTO role (name) VALUES ('ROLE_USER');
-- INSERT INTO role (name) VALUES ('ROLE_ADMIN');
-- INSERT INTO role (name) VALUES ('ROLE_SUPER_ADMIN');
-- INSERT INTO users (username, password, email, status, first_name, last_name, phone) VALUES ('admin', '$2a$14$C2HvKTOQmGVMKZGQ0xa1NO8UUcRHoYgjESdZlEj51bZcSKye43Qdm', 'hussain.qurain@outlook.com', 'active', 'Hussain', 'Al-Qurain', '+966536071929');
-- INSERT INTO company (name, tax_id, phone, mobile, email, state, city, address, zip, add_purchased_items_to_favorites, logo, allowed_invoice_deviation, export_delivery_notes_as_bills)
-- VALUES ('Company A', '3000000000000', '+966013555555', '+966555555555', 'hussain.qurain@outlook.com', 'khobar', 'khobar', 'khobar', '55555', TRUE, 'test.png', 3, TRUE);
-- INSERT INTO user_roles (user_id, role_id) VALUES (1, 1);
-- INSERT INTO user_roles (user_id, role_id) VALUES (1, 2);
-- INSERT INTO user_roles (user_id, role_id) VALUES (1, 3);
-- INSERT INTO company_user (id, company_id, users_id) VALUES (1, 1, 1)
--change to update instead of create-drop for hibernate

INSERT INTO role (name)
SELECT 'ROLE_USER'
    WHERE NOT EXISTS (SELECT 1 FROM role WHERE name = 'ROLE_USER');

INSERT INTO role (name)
SELECT 'ROLE_ADMIN'
    WHERE NOT EXISTS (SELECT 1 FROM role WHERE name = 'ROLE_ADMIN');

INSERT INTO role (name)
SELECT 'ROLE_SUPER_ADMIN'
    WHERE NOT EXISTS (SELECT 1 FROM role WHERE name = 'ROLE_SUPER_ADMIN');

INSERT INTO users (username, password, email, status, first_name, last_name, phone)
SELECT 'admin', '$2a$14$C2HvKTOQmGVMKZGQ0xa1NO8UUcRHoYgjESdZlEj51bZcSKye43Qdm', 'hussain.qurain@outlook.com', 'active', 'Hussain', 'Al-Qurain', '+966536071929'
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

INSERT INTO company (name, tax_id, phone, mobile, email, state, city, address, zip, add_purchased_items_to_favorites, logo, allowed_invoice_deviation, export_delivery_notes_as_bills)
SELECT 'Company A', '3000000000000', '+966013555555', '+966555555555', 'hussain.qurain@outlook.com', 'khobar', 'khobar', 'khobar', '55555', TRUE, 'test.png', 3, TRUE
    WHERE NOT EXISTS (SELECT 1 FROM company WHERE name = 'Company A');


INSERT INTO user_roles (user_id, role_id)
SELECT 1, 1
    WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = 1 AND role_id = 1);

INSERT INTO user_roles (user_id, role_id)
SELECT 1, 2
    WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = 1 AND role_id = 2);

INSERT INTO user_roles (user_id, role_id)
SELECT 1, 3
    WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = 1 AND role_id = 3);

INSERT INTO company_user (id, company_id, users_id)
SELECT 1, 1, 1
    WHERE NOT EXISTS (SELECT 1 FROM company_user WHERE company_id = 1 AND users_id = 1);
