# Bekri – Plateforme de Bien-Être Mental et Physique
### Version Desktop · Java / JavaFX

![Java](https://img.shields.io/badge/Java-17%2B-blue?style=flat-square&logo=openjdk)
![JavaFX](https://img.shields.io/badge/JavaFX-17%2B-green?style=flat-square)
![MySQL](https://img.shields.io/badge/MySQL-8.0-orange?style=flat-square&logo=mysql)
![Maven](https://img.shields.io/badge/Maven-3.8%2B-yellow?style=flat-square&logo=apachemaven)
![Esprit](https://img.shields.io/badge/Esprit-PIDEV%203A%202025--2026-purple?style=flat-square)

---

## Overview

Ce projet a été développé dans le cadre du **PIDEV – Programme d'Ingénierie 3ème Année** à Esprit School of Engineering (Année Universitaire 2025–2026).

Bekri est une plateforme unifiée dédiée à la prévention et au suivi du bien-être mental et physique. Elle offre aux utilisateurs un espace centralisé pour évaluer leur état de santé, suivre leur progression et accéder à des ressources adaptées à leurs besoins. Cette version est une **application desktop** développée en Java avec JavaFX.

---

## Features

- 🧠 **Évaluation initiale** – Diagnostic personnalisé de l'état mental et physique de l'utilisateur
- 📊 **Analyse hebdomadaire** – Rapports et synthèses hebdomadaires sur l'évolution du bien-être
- 📅 **Suivi quotidien** – Journal de bord pour un suivi régulier au jour le jour
- 🧪 **Gestion des tests mentaux** – Administration et résultats de tests psychologiques standardisés
- 🎯 **Gestion des événements** – Planification et suivi d'activités liées au bien-être
- 📝 **Gestion des posts** – Espace communautaire pour partager des expériences et conseils

---

## Tech Stack

| Couche | Technologie |
|--------|------------|
| Interface utilisateur | JavaFX · FXML · SceneBuilder |
| Backend / Logique métier | Java 17+ · OOP · JDBC |
| Base de données | MySQL 8.0 |
| Build tool | Maven 3.8+ |
| IDE recommandé | IntelliJ IDEA / Eclipse |
| Architecture | MVC (Modèle-Vue-Contrôleur) |

---

## Architecture

L'application suit une architecture **MVC (Modèle-Vue-Contrôleur)** :

- **Modèle** – Classes Java représentant les entités métier et la couche d'accès aux données (JDBC)
- **Vue** – Fichiers FXML définissant l'interface graphique JavaFX
- **Contrôleur** – Classes Java gérant la logique d'interaction entre la vue et le modèle

---

## Getting Started

### Prérequis

- JDK 17 ou supérieur
- JavaFX SDK 17+
- Maven 3.8+
- Serveur MySQL (XAMPP / WAMP recommandé)
- IntelliJ IDEA ou Eclipse

### Installation

```bash
# Cloner le dépôt
git clone https://github.com/Eyaaessid/Esprit-JAVA-3A39-2026-Bekri.git

# Accéder au dossier
cd Bekri-Java

# Compiler le projet avec Maven
mvn clean install
```

### Configuration de la base de données

```properties
# Dans src/main/resources/config.properties
db.url=jdbc:mysql://localhost:3306/bekri_db
db.user=root
db.password=
```

```bash
# Importer le fichier SQL fourni
mysql -u root -p bekri_db < database/bekri_db.sql
```

### Lancer l'application

```bash
# Via Maven
mvn javafx:run
```

Ou directement depuis IntelliJ IDEA en lançant la classe `Main.java`.

> **Note :** Assurez-vous que le module JavaFX est correctement configuré dans votre IDE.  
> Pour IntelliJ, ajoutez les VM options :  
> `--module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml`

---

## Contributors

| Nom | Rôle |
|-----|------|
| Eya Essid | Développement Full-Stack |
| Hiba Ibn Hadj Mohammed | Développement Full-Stack |
| Aziz Jedidi | Développement Full-Stack |
| Aziz Barhoumi | Développement Full-Stack |
| Adem Ben Amara | Développement Full-Stack |

---

## Academic Context

Développé à **Esprit School of Engineering** – Tunisia  
PIDEV – 3A | 2025–2026

---

## Acknowledgments

Nous remercions Esprit School of Engineering pour l'encadrement pédagogique, ainsi que nos tuteurs et encadrants pour leur soutien tout au long du développement de ce projet.
