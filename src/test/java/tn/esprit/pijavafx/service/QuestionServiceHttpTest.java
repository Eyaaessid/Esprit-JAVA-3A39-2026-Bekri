package tn.esprit.pijavafx.service;

import org.junit.jupiter.api.*;
import tn.esprit.pijavafx.model.QuestionEvaluationDto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour QuestionServiceHttp
 * Suit le pattern du workshop : @BeforeAll, @Order, @AfterEach
 * Base de données MySQL réelle requise (Spring Boot backend sur http://127.0.0.1:8080)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QuestionServiceHttpTest {

    static IQuestionService service;
    static int idQuestionTest = -1;

    // ── Setup ─────────────────────────────────────────────────────────────────
    @BeforeAll
    static void setup() {
        service = new QuestionServiceHttp();
    }

    // ── Nettoyage après chaque test ───────────────────────────────────────────
    @AfterEach
    void cleanUp() {
        if (idQuestionTest != -1) {
            try {
                service.delete(idQuestionTest);
            } catch (Exception ignored) {
                // Déjà supprimé par le test lui-même
            }
            idQuestionTest = -1;
        }
    }

    // ── Test 1 : Ajouter une question ─────────────────────────────────────────
    @Test
    @Order(1)
    void testAjouterQuestion() throws Exception {
        QuestionEvaluationDto dto = new QuestionEvaluationDto();
        dto.setTexte("Question de test unitaire — humeur ?");
        dto.setCategory("humeur");
        dto.setTypeReponse("choice");
        dto.setOption1("Très bien");
        dto.setOption2("Correctement");
        dto.setOption3("Pas bien du tout");

        service.create(dto);

        List<QuestionEvaluationDto> questions = service.getAll();
        assertNotNull(questions, "La liste ne doit pas être null");
        assertFalse(questions.isEmpty(), "La liste ne doit pas être vide après ajout");

        boolean trouve = questions.stream()
                .anyMatch(q -> "Question de test unitaire — humeur ?".equals(q.getTexte()));
        assertTrue(trouve, "La question ajoutée doit être retrouvée dans la liste");

        // Récupérer l'ID pour les tests suivants et le nettoyage
        idQuestionTest = questions.stream()
                .filter(q -> "Question de test unitaire — humeur ?".equals(q.getTexte()))
                .mapToInt(QuestionEvaluationDto::getId)
                .max()
                .orElse(-1);
    }

    // ── Test 2 : Afficher toutes les questions ────────────────────────────────
    @Test
    @Order(2)
    void testAfficherQuestions() throws Exception {
        // Créer une question pour s'assurer que la liste n'est pas vide
        QuestionEvaluationDto dto = new QuestionEvaluationDto();
        dto.setTexte("Question test afficher — sommeil ?");
        dto.setCategory("sommeil");
        dto.setTypeReponse("choice");
        dto.setOption1("Très bien dormi");
        dto.setOption2("Moyennement");
        dto.setOption3("Très mal dormi");
        service.create(dto);

        List<QuestionEvaluationDto> questions = service.getAll();

        assertNotNull(questions, "getAll() ne doit pas retourner null");
        assertFalse(questions.isEmpty(), "La liste doit contenir au moins une question");

        // Vérifier que chaque question a un texte et une catégorie non null
        questions.forEach(q -> {
            assertNotNull(q.getTexte(),    "Chaque question doit avoir un texte");
            assertNotNull(q.getCategory(), "Chaque question doit avoir une catégorie");
        });

        // Récupérer l'ID pour le nettoyage
        idQuestionTest = questions.stream()
                .filter(q -> "Question test afficher — sommeil ?".equals(q.getTexte()))
                .mapToInt(QuestionEvaluationDto::getId)
                .max()
                .orElse(-1);
    }

    // ── Test 3 : Modifier une question ────────────────────────────────────────
    @Test
    @Order(3)
    void testModifierQuestion() throws Exception {
        // Créer une question à modifier
        QuestionEvaluationDto dto = new QuestionEvaluationDto();
        dto.setTexte("Question avant modification");
        dto.setCategory("poids");
        dto.setTypeReponse("choice");
        dto.setOption1("Option A");
        dto.setOption2("Option B");
        dto.setOption3("Option C");
        service.create(dto);

        // Récupérer l'ID créé
        List<QuestionEvaluationDto> avant = service.getAll();
        idQuestionTest = avant.stream()
                .filter(q -> "Question avant modification".equals(q.getTexte()))
                .mapToInt(QuestionEvaluationDto::getId)
                .max()
                .orElse(-1);

        assertNotEquals(-1, idQuestionTest, "La question créée doit exister");

        // Modifier
        QuestionEvaluationDto modif = new QuestionEvaluationDto();
        modif.setTexte("Question après modification");
        modif.setCategory("nutrition");
        modif.setTypeReponse("choice");
        modif.setOption1("Nouvelle option A");
        modif.setOption2("Nouvelle option B");
        modif.setOption3("Nouvelle option C");
        service.update(idQuestionTest, modif);

        // Vérifier
        List<QuestionEvaluationDto> apres = service.getAll();

        boolean texteModifie = apres.stream()
                .anyMatch(q -> "Question après modification".equals(q.getTexte()));
        assertTrue(texteModifie, "Le texte modifié doit apparaître dans la liste");

        boolean ancienTexteDisparu = apres.stream()
                .noneMatch(q -> "Question avant modification".equals(q.getTexte()));
        assertTrue(ancienTexteDisparu, "L'ancien texte ne doit plus exister");

        // Vérifier que la catégorie a aussi été mise à jour
        QuestionEvaluationDto modifiee = apres.stream()
                .filter(q -> "Question après modification".equals(q.getTexte()))
                .findFirst()
                .orElse(null);
        assertNotNull(modifiee, "La question modifiée doit être retrouvée");
        assertEquals("nutrition", modifiee.getCategory(), "La catégorie doit être mise à jour");
    }

    // ── Test 4 : Supprimer une question ──────────────────────────────────────
    @Test
    @Order(4)
    void testSupprimerQuestion() throws Exception {
        // Créer une question à supprimer
        QuestionEvaluationDto dto = new QuestionEvaluationDto();
        dto.setTexte("Question à supprimer — test unitaire");
        dto.setCategory("activite");
        dto.setTypeReponse("choice");
        dto.setOption1("Oui");
        dto.setOption2("Non");
        dto.setOption3("Parfois");
        service.create(dto);

        // Récupérer l'ID
        List<QuestionEvaluationDto> avant = service.getAll();
        idQuestionTest = avant.stream()
                .filter(q -> "Question à supprimer — test unitaire".equals(q.getTexte()))
                .mapToInt(QuestionEvaluationDto::getId)
                .max()
                .orElse(-1);

        assertNotEquals(-1, idQuestionTest, "La question à supprimer doit exister");

        // Supprimer
        service.delete(idQuestionTest);

        // Vérifier qu'elle n'existe plus
        List<QuestionEvaluationDto> apres = service.getAll();
        boolean existeEncore = apres.stream()
                .anyMatch(q -> q.getId() == idQuestionTest);
        assertFalse(existeEncore, "La question supprimée ne doit plus exister dans la liste");

        idQuestionTest = -1; // @AfterEach n'a rien à nettoyer
    }

    // ── Test 5 : Vérifier la cohérence des options ───────────────────────────
    @Test
    @Order(5)
    void testOptionsNonVides() throws Exception {
        QuestionEvaluationDto dto = new QuestionEvaluationDto();
        dto.setTexte("Question test options — hydratation ?");
        dto.setCategory("hydratation");
        dto.setTypeReponse("choice");
        dto.setOption1("Beaucoup");
        dto.setOption2("Modérément");
        dto.setOption3("Peu");
        service.create(dto);

        List<QuestionEvaluationDto> questions = service.getAll();
        QuestionEvaluationDto trouvee = questions.stream()
                .filter(q -> "Question test options — hydratation ?".equals(q.getTexte()))
                .findFirst()
                .orElse(null);

        assertNotNull(trouvee, "La question doit être retrouvée");
        assertNotNull(trouvee.getOption1(), "Option 1 ne doit pas être null");
        assertNotNull(trouvee.getOption2(), "Option 2 ne doit pas être null");
        assertNotNull(trouvee.getOption3(), "Option 3 ne doit pas être null");
        assertFalse(trouvee.getOption1().isBlank(), "Option 1 ne doit pas être vide");
        assertFalse(trouvee.getOption2().isBlank(), "Option 2 ne doit pas être vide");
        assertFalse(trouvee.getOption3().isBlank(), "Option 3 ne doit pas être vide");
        assertEquals("hydratation", trouvee.getCategory(), "La catégorie doit correspondre");

        // Nettoyage
        idQuestionTest = trouvee.getId();
    }

    // ── Test 6 : Validation — texte trop court doit échouer ──────────────────
    @Test
    @Order(6)
    void testAjouterQuestionTexteInvalide() {
        QuestionEvaluationDto dto = new QuestionEvaluationDto();
        dto.setTexte("Ab");   // moins de 5 caractères — invalide
        dto.setCategory("humeur");
        dto.setTypeReponse("choice");
        dto.setOption1("Oui");
        dto.setOption2("Non");
        dto.setOption3("Peut-être");

        assertThrows(Exception.class, () -> service.create(dto),
                "Créer une question avec un texte trop court doit lever une exception");
    }
}