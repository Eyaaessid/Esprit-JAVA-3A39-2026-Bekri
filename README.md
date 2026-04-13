# Application de Gestion d'Événements - JavaFX

Application JavaFX pour la gestion d'événements et de participations avec tests unitaires JUnit.

## Structure du Projet

```
src/
├── main/
│   ├── java/tn/esprit/
│   │   ├── models/          # Modèles de données
│   │   ├── services/        # Logique métier
│   │   ├── utils/           # Utilitaires (connexion DB)
│   │   └── Main.java        # Point d'entrée
│   └── resources/
│       └── style.css        # Styles CSS
└── test/
    └── java/tn/esprit/services/  # Tests unitaires
```

## Prérequis

- Java 17+
- Maven 3.6+
- MySQL 8.0+
- JavaFX 17+

## Configuration de la Base de Données

1. Créer la base de données:
```bash
mysql -u root -p < database.sql
```

2. Modifier les paramètres de connexion dans `MyConnection.java` si nécessaire:
```java
private static final String URL = "jdbc:mysql://localhost:3306/evenement_db";
private static final String USER = "root";
private static final String PASSWORD = "";
```

## Compilation et Exécution

### Compiler le projet
```bash
mvn clean compile
```

### Exécuter les tests unitaires
```bash
mvn test
```

### Lancer l'application
```bash
mvn javafx:run
```

## Tests Unitaires

Les tests utilisent JUnit Jupiter 5.10.1 et suivent les bonnes pratiques:

- `@BeforeAll`: Initialisation des services avant tous les tests
- `@Test`: Annotation pour chaque méthode de test
- `@Order`: Définit l'ordre d'exécution des tests
- Assertions: `assertEquals`, `assertNotNull`, `assertTrue`, `assertFalse`, etc.

### EvenementServiceTest
- testAjouterEvenement
- testAfficherEvenements
- testGetEvenementById
- testModifierEvenement
- testSupprimerEvenement
- testAjouterEvenementAvecDonneesInvalides

### ParticipationServiceTest
- testAjouterParticipation
- testAfficherParticipations
- testGetParticipationById
- testModifierParticipation
- testSupprimerParticipation
- testAjouterParticipationAvecEmailInvalide

## Dépendances Maven

- JavaFX Controls & FXML
- MySQL Connector Java 8.0.33
- JUnit Jupiter 5.10.1

## Auteur

Projet développé pour l'apprentissage de JavaFX et des tests unitaires JUnit.
