-- Base: bekri_db (MySQL 8+)
-- Exécuter ce script une fois pour créer / mettre à jour la table utilisateur.

CREATE TABLE IF NOT EXISTS utilisateur (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nom VARCHAR(100) NOT NULL,
  prenom VARCHAR(100) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  mot_de_passe VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  statut VARCHAR(20) NOT NULL DEFAULT 'ACTIF',
  photo_profil VARCHAR(500),
  telephone VARCHAR(30) NULL,
  date_naissance DATE NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL
);

-- Si la table existait déjà sans colonnes optionnelles :
-- ALTER TABLE utilisateur ADD COLUMN telephone VARCHAR(30) NULL;
-- ALTER TABLE utilisateur ADD COLUMN date_naissance DATE NULL;
