INSERT INTO user_roles(id, user_role)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'USER'),
    ('22222222-2222-2222-2222-222222222222', 'MODERATOR'),
    ('33333333-3333-3333-3333-333333333333', 'ADMIN')
ON CONFLICT DO NOTHING;

INSERT INTO users (id, email, password, first_name, last_name, phone_number, created_at, updated_at)
VALUES (
           'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
           'admin@blockwin.local',
           '$2a$10$hvBIMGIkZmu5VdsQ6LygtuqP6hqmPpallrUaGJN7.NYVsU8aN8.Im',
           'Admin',
           'BlockWin',
           NULL,
           now(),
           now()
       )
ON CONFLICT DO NOTHING;

INSERT INTO users_user_roles (user_entity_id, user_roles_id)
VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '33333333-3333-3333-3333-333333333333'), -- ADMIN
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111')  -- USER
ON CONFLICT DO NOTHING;
