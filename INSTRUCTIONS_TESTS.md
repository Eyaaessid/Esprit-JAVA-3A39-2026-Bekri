# Instructions pour les Tests Unitaires JUnit

## Configuration Complétée ✓

Le projet a été configuré avec:
- ✓ Dépendance JUnit Jupiter 5.10.1 dans pom.xml
- ✓ Structure Maven avec séparation src/main et src/test
- ✓ Classes de modèles (Evenement, ParticipationEvenement, ParticipationDisplay)
- ✓ Classes de services (EvenementService, ParticipationService)
- ✓ Tests unitaires complets pour les deux services

## Structure des Tests

### 1. Imports JUnit
```java
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
```

### 2. Annotations Utilisées

- `@BeforeAll`: Méthode statique exécutée une seule fois avant tous les tests
  - Initialise les services
  - Crée les objets de test

- `@Test`: Marque une méthode comme test unitaire

- `@Order(n)`: Définit l'ordre d'exécution des tests (1, 2, 3...)

- `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)`: Active l'ordre des tests

- `@AfterAll`: Méthode statique exécutée après tous les tests (nettoyage)

### 3. Assertions Principales

```java
// Vérifier l'égalité
assertEquals(valeurAttendue, valeurActuelle);

// Vérifier qu'un objet n'est pas null
assertNotNull(objet);

// Vérifier qu'un objet est null
assertNull(objet);

// Vérifier une condition vraie
assertTrue(condition);

// Vérifier une condition fausse
assertFalse(condition);

// Vérifier qu'aucune exception n'est levée
assertDoesNotThrow(() -> { /* code */ });

// Vérifier qu'une exception est levée
assertThrows(SQLException.class, () -> { /* code */ });
```

## Exécution des Tests

### Avant de lancer les tests:

1. **Créer la base de données**:
```bash
mysql -u root -p < database.sql
```

2. **Vérifier la connexion** dans `MyConnection.java`:
```java
private static final String URL = "jdbc:mysql://localhost:3306/evenement_db";
private static final String USER = "root";
private static final String PASSWORD = "";
```

### Lancer les tests:

```bash
# Tous les tests
mvn test

# Un seul fichier de test
mvn test -Dtest=EvenementServiceTest

# Une seule méthode de test
mvn test -Dtest=EvenementServiceTest#testAjouterEvenement
```

## Tests Implémentés

### EvenementServiceTest
1. `testAjouterEvenement` - Ajoute un événement et vérifie son insertion
2. `testAfficherEvenements` - Liste tous les événements
3. `testGetEvenementById` - Récupère un événement par ID
4. `testModifierEvenement` - Modifie un événement existant
5. `testSupprimerEvenement` - Supprime un événement
6. `testAjouterEvenementAvecDonneesInvalides` - Test de validation

### ParticipationServiceTest
1. `testAjouterParticipation` - Ajoute une participation
2. `testAfficherParticipations` - Liste avec jointure événement
3. `testGetParticipationById` - Récupère une participation par ID
4. `testModifierParticipation` - Modifie une participation
5. `testSupprimerParticipation` - Supprime une participation
6. `testAjouterParticipationAvecEmailInvalide` - Test de validation

## Bonnes Pratiques Appliquées

✓ Tests ordonnés pour éviter les conflits
✓ Utilisation de @BeforeAll pour l'initialisation
✓ Tests d'intégration avec base de données réelle
✓ Tests de validation des données
✓ Nettoyage après les tests (@AfterAll)
✓ Assertions claires et précises
✓ Nommage explicite des méthodes de test

## Résultats Attendus

Lors de l'exécution de `mvn test`, vous devriez voir:

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running tn.esprit.services.EvenementServiceTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running tn.esprit.services.ParticipationServiceTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
```

## Dépannage

### Erreur de connexion MySQL
- Vérifier que MySQL est démarré
- Vérifier les credentials dans MyConnection.java
- Vérifier que la base evenement_db existe

### Tests échouent
- Vérifier que les tables sont créées (database.sql)
- Vérifier les contraintes de clés étrangères
- Nettoyer les données de test entre les exécutions

## Prochaines Étapes

Pour améliorer les tests:
1. Ajouter des tests avec base de données en mémoire (H2)
2. Utiliser des mocks pour isoler les tests
3. Ajouter des tests de performance
4. Implémenter des tests paramétrés (@ParameterizedTest)
