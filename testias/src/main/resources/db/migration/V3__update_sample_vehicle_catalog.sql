INSERT INTO vehicles (id, brand, model, year, price, availability, version) VALUES
('VM-001', 'Andina', 'Urbano', 2024, 80000000.00, 'AVAILABLE', 0),
('VM-002', 'Andina', 'Urbano', 2024, 80000000.00, 'AVAILABLE', 0),
('VM-003', 'Pacífico', 'Ruta', 2025, 110000000.00, 'AVAILABLE', 0),
('VM-004', 'Pacífico', 'Familiar', 2023, 95000000.00, 'AVAILABLE', 0),
('VM-005', 'Sierra', 'Camino', 2025, 130000000.00, 'AVAILABLE', 0)
ON CONFLICT (id) DO UPDATE SET
    brand = EXCLUDED.brand,
    model = EXCLUDED.model,
    year = EXCLUDED.year,
    price = EXCLUDED.price;
