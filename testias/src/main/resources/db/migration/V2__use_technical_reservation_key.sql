ALTER TABLE reservations ADD COLUMN id BIGINT;

CREATE SEQUENCE reservations_id_seq;
ALTER TABLE reservations ALTER COLUMN id SET DEFAULT nextval('reservations_id_seq');
UPDATE reservations SET id = nextval('reservations_id_seq') WHERE id IS NULL;
ALTER TABLE reservations ALTER COLUMN id SET NOT NULL;

ALTER TABLE reservations DROP CONSTRAINT reservations_pkey;
ALTER TABLE reservations ADD CONSTRAINT reservations_pkey PRIMARY KEY (id);
ALTER TABLE reservations ADD CONSTRAINT reservations_reference_id_key UNIQUE (reference_id);
ALTER SEQUENCE reservations_id_seq OWNED BY reservations.id;
