-- Script SQL pour ajouter la table favoris
-- À exécuter dans la base de données bekri_db

USE bekri_db;

-- Table favoris pour le système de favoris intelligent
CREATE TABLE IF NOT EXISTS favoris (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    evenement_id INT NOT NULL,
    date_ajout DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_favori (user_id, evenement_id),
    FOREIGN KEY (evenement_id) REFERENCES evenement(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_evenement_id (evenement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insertion de quelques favoris de test (optionnel)
-- Remplacez les IDs par des IDs valides de votre base
-- INSERT INTO favoris (user_id, evenement_id) VALUES (1, 1);
-- INSERT INTO favoris (user_id, evenement_id) VALUES (1, 2);
-- INSERT INTO favoris (user_id, evenement_id) VALUES (2, 1);

SELECT 'Table favoris créée avec succès!' as message;
