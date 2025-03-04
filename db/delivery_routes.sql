-- Create table for locations
CREATE TABLE locations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

-- Create table for paths with traffic factor
CREATE TABLE paths (
    from_location INT,
    to_location INT,
    distance DOUBLE,
    traffic_factor DOUBLE,
    PRIMARY KEY (from_location, to_location),
    FOREIGN KEY (from_location) REFERENCES locations(id),
    FOREIGN KEY (to_location) REFERENCES locations(id)
);
