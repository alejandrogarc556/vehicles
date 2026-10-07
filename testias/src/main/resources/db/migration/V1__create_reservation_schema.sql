CREATE TABLE vehicles (
    id VARCHAR(50) PRIMARY KEY,
    brand VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    year INTEGER NOT NULL,
    price NUMERIC(14, 2) NOT NULL,
    availability VARCHAR(20) NOT NULL CHECK (availability IN ('AVAILABLE', 'RESERVED')),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE reservations (
    reference_id VARCHAR(50) PRIMARY KEY,
    vehicle_id VARCHAR(50) NOT NULL,
    buyer_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('CONFIRMED', 'REJECTED')),
    reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_reservation_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id)
);

CREATE INDEX idx_vehicles_brand_id ON vehicles (brand, id);
CREATE INDEX idx_reservations_vehicle_id ON reservations (vehicle_id);

INSERT INTO vehicles (id, brand, model, year, price, availability, version) VALUES
('VM-001', 'Andina', 'Urbano', 2024, 80000000.00, 'AVAILABLE', 0),
('VM-002', 'Andina', 'Urbano', 2024, 80000000.00, 'AVAILABLE', 0),
('VM-003', 'Pacífico', 'Ruta', 2025, 110000000.00, 'AVAILABLE', 0),
('VM-004', 'Pacífico', 'Familiar', 2023, 95000000.00, 'AVAILABLE', 0),
('VM-005', 'Sierra', 'Camino', 2025, 130000000.00, 'AVAILABLE', 0)
ON CONFLICT (id) DO NOTHING;
