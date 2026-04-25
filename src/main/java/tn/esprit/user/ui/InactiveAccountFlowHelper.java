package tn.esprit.user.ui;

import tn.esprit.shared.DialogHelper;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.entity.Utilisateur;

import java.io.IOException;
import java.util.function.Consumer;

public final class InactiveAccountFlowHelper {
    private static final String FALLBACK_MESSAGE =
            "Votre compte est inactif. Veuillez consulter votre email ou contacter support@bekri.tn.";
    private static final String ADMIN_SUPPORT_MESSAGE =
            "Votre compte a ete desactive par un administrateur. Veuillez contacter support@bekri.tn pour le reactiver.";

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

        String deactivatedBy = user.getDeactivatedBy();
        if ("admin".equalsIgnoreCase(deactivatedBy)) {
            DialogHelper.showInfo("Compte inactif", ADMIN_SUPPORT_MESSAGE);
            return;
        }

        if ("user".equalsIgnoreCase(deactivatedBy)) {
            boolean openCodeScreen = DialogHelper.showChoice(
                    "Compte desactive",
                    "Votre compte a ete desactive par vous. Saisissez le code de reactivation recu par email pour le reactiver.",
                    "Entrer le code",
                    "Fermer"
            );
            if (openCodeScreen) {
                openSelfReactivationCode(user.getEmail(), beforeNavigate, navigationErrorHandler);
            }
            return;
        }

        DialogHelper.showInfo("Compte inactif", FALLBACK_MESSAGE);
    }

    public static void openSupportScreen(Consumer<String> navigationErrorHandler) {
        openSupportScreen(() -> {}, navigationErrorHandler);
    }

    public static void openSupportScreen(Runnable beforeNavigate, Consumer<String> navigationErrorHandler) {
        try {
            beforeNavigate.run();
            SceneManager.switchTo("reactivation-request");
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
