-- Création de la base de données
CREATE DATABASE IF NOT EXISTS evenement_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE evenement_db;

-- Table evenement
CREATE TABLE IF NOT EXISTS evenement (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    description TEXT,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    lieu VARCHAR(255) NOT NULL,
    capacite INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table participation_evenement
CREATE TABLE IF NOT EXISTS participation_evenement (
    id INT AUTO_INCREMENT PRIMARY KEY,
    evenement_id INT NOT NULL,
    nom_participant VARCHAR(255) NOT NULL,
    email_participant VARCHAR(255) NOT NULL,
    date_inscription DATETIME NOT NULL,
    statut VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (evenement_id) REFERENCES evenement(id) ON DELETE CASCADE
);

-- Insertion de données de test
INSERT INTO evenement (nom, description, date_debut, date_fin, lieu, capacite) VALUES
('Conférence JavaFX 2024', 'Une conférence sur JavaFX et les interfaces graphiques', '2024-05-15', '2024-05-17', 'Tunis', 150),
('Workshop Spring Boot', 'Atelier pratique sur Spring Boot', '2024-06-10', '2024-06-11', 'Sfax', 80),
('Hackathon Innovation', 'Compétition de développement 48h', '2024-07-20', '2024-07-22', 'Sousse', 200);

INSERT INTO participation_evenement (evenement_id, nom_participant, email_participant, date_inscription, statut) VALUES
(1, 'Mohamed Trabelsi', 'mohamed.trabelsi@example.com', '2024-04-01 10:30:00', 'Confirmé'),
(1, 'Fatma Gharbi', 'fatma.gharbi@example.com', '2024-04-02 14:15:00', 'Confirmé'),
(2, 'Ali Mansour', 'ali.mansour@example.com', '2024-05-10 09:00:00', 'En attente'),
(3, 'Salma Bouaziz', 'salma.bouaziz@example.com', '2024-06-15 16:45:00', 'Confirmé');
