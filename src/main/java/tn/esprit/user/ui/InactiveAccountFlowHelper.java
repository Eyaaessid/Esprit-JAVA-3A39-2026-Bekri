package tn.esprit.user.ui;

import tn.esprit.shared.DialogHelper;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.entity.Utilisateur;

import java.io.IOException;
import java.util.function.Consumer;

public final class InactiveAccountFlowHelper {
    private static final String FALLBACK_MESSAGE =
            "Votre compte est inactif. Vous pouvez soumettre une demande de reactivation depuis l'application.";
    private static final String ADMIN_REQUEST_MESSAGE =
            "Votre compte a ete desactive par un administrateur. Vous pouvez soumettre une demande de reactivation.";
    private static final String SELF_CODE_MESSAGE =
            "Votre compte a ete desactive par vous. Saisissez le code de reactivation recu par email pour le reactiver.";

    private InactiveAccountFlowHelper() {
    }

    public static void handleInactiveUser(Utilisateur user, Consumer<String> navigationErrorHandler) {
        handleInactiveUser(user, () -> {}, navigationErrorHandler);
    }

    public static void handleInactiveUser(Utilisateur user,
                                          Runnable beforeNavigate,
                                          Consumer<String> navigationErrorHandler) {
        if (user == null) {
            DialogHelper.showInfo("Compte inactif", FALLBACK_MESSAGE);
            return;
        }

        if ("user".equalsIgnoreCase(user.getDeactivatedBy())) {
            boolean openCodeScreen = DialogHelper.showChoice(
                    "Compte desactive",
                    SELF_CODE_MESSAGE,
                    "Entrer le code",
                    "Fermer"
            );
            if (openCodeScreen) {
                openSelfReactivationCode(user.getEmail(), beforeNavigate, navigationErrorHandler);
            }
            return;
        }

        boolean openRequestScreen = DialogHelper.showChoice(
                "Compte inactif",
                "admin".equalsIgnoreCase(user.getDeactivatedBy()) ? ADMIN_REQUEST_MESSAGE : FALLBACK_MESSAGE,
                "Soumettre une demande",
                "Fermer"
        );
        if (openRequestScreen) {
            openSupportScreen(user.getEmail(), beforeNavigate, navigationErrorHandler);
        }
    }

    public static void openSupportScreen(Consumer<String> navigationErrorHandler) {
        openSupportScreen(null, () -> {}, navigationErrorHandler);
    }

    public static void openSupportScreen(Runnable beforeNavigate, Consumer<String> navigationErrorHandler) {
        openSupportScreen(null, beforeNavigate, navigationErrorHandler);
    }

    public static void openSupportScreen(String email, Consumer<String> navigationErrorHandler) {
        openSupportScreen(email, () -> {}, navigationErrorHandler);
    }

    public static void openSupportScreen(String email,
                                         Runnable beforeNavigate,
                                         Consumer<String> navigationErrorHandler) {
        try {
            beforeNavigate.run();
            ReactivationRequestController controller =
                    SceneManager.switchToAndGetController("reactivation-request");
            controller.setEmail(email);
        } catch (IOException e) {
            navigationErrorHandler.accept("Erreur de navigation.");
        }
    }

    public static void openSelfReactivationCode(String email, Consumer<String> navigationErrorHandler) {
        openSelfReactivationCode(email, () -> {}, navigationErrorHandler);
    }

    public static void openSelfReactivationCode(String email,
                                                Runnable beforeNavigate,
                                                Consumer<String> navigationErrorHandler) {
        try {
            beforeNavigate.run();
            SelfReactivationCodeController controller = SceneManager.switchToAndGetController("self-reactivation-code");
            controller.setEmail(email);
        } catch (IOException e) {
            navigationErrorHandler.accept("Erreur de navigation.");
        }
    }
}
