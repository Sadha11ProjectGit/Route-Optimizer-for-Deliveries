-- Inserting sample locations
INSERT INTO locations (name) VALUES 
('Warehouse'),
('Location A'),
('Location B'),
('Location C'),
('Location D');

-- Inserting sample paths (with traffic factor for dynamic changes)
INSERT INTO paths (from_location, to_location, distance, traffic_factor) VALUES
(1, 2, 10.0, 1.2),  -- Warehouse to Location A
(2, 3, 5.0, 1.0),   -- Location A to Location B
(2, 4, 3.0, 1.5),   -- Location A to Location C
(3, 4, 7.0, 1.1),   -- Location B to Location C
(4, 5, 4.0, 1.3),   -- Location C to Location D
(1, 5, 15.0, 1.4);  -- Warehouse to Location D
