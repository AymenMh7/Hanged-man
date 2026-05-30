package hangman;

import hangman.ui.GameWindow;
import javafx.application.Application;

/**
 * Point d'entrée de l'application — fonctionne aussi bien quand on
 * lance le jeu via :
 *
 *   • Les scripts compile.bat / run.bat (classpath déjà préparé), OU
 *   • Votre IDE (VS Code, IntelliJ IDEA, Eclipse), à condition que
 *     JavaFX et le connecteur MySQL soient sur le classpath du projet
 *     et que les arguments VM du module-path JavaFX soient configurés.
 *
 * Pourquoi cette classe n'étend PAS {@link Application} :
 *   La JVM vérifie la classe lancée. Si elle étend Application et que
 *   les modules JavaFX sont absents, elle plante avec
 *   "JavaFX runtime components are missing" avant que votre code ait
 *   la moindre chance de s'exécuter. Utiliser un lanceur séparé évite
 *   cette vérification, ce qui permet d'attraper l'erreur sous-jacente
 *   ici et d'afficher une indication conviviale.
 *
 * Configuration rapide pour l'IDE (valeurs Windows par défaut) :
 *   - Arguments VM : --module-path "C:/javafx-sdk/lib" --add-modules javafx.controls
 *   - Bibliothèques : lib/mysql-connector-*.jar  +  C:/javafx-sdk/lib/*.jar
 *
 * VS Code : les fichiers .vscode/launch.json + .vscode/settings.json
 *           fournis devraient faire ça automatiquement. Utilisez
 *           "Run → Start Debugging" (F5) pour que la config de
 *           launch.json soit prise en compte.
 *
 * IntelliJ IDEA : voir la section "Run from your IDE" du README.md.
 */
public class Main {

    public static void main(String[] args) {
        try {
            Application.launch(GameWindow.class, args);
        } catch (LinkageError err) {
            // LinkageError couvre NoClassDefFoundError, UnsatisfiedLinkError,
            // et les autres erreurs de chargement de classes que JavaFX
            // peut rencontrer.
            printJavaFxHint(err);
        } catch (RuntimeException ex) {
            // Attrape l'IllegalStateException que JavaFX lance quand la
            // boîte à outils ne peut pas s'initialiser du tout.
            if (rootCauseIsJavaFxMissing(ex)) {
                printJavaFxHint(ex);
            } else {
                throw ex;
            }
        }
    }

    private static boolean rootCauseIsJavaFxMissing(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            String msg = cur.getMessage();
            if (msg != null && (msg.contains("JavaFX runtime")
                             || msg.contains("javafx")
                             || msg.contains("Application launch must be"))) {
                return true;
            }
        }
        return false;
    }

    private static void printJavaFxHint(Throwable err) {
        System.err.println();
        System.err.println("============================================================");
        System.err.println("  JavaFX n'a pas pu démarrer.");
        System.err.println("============================================================");
        System.err.println();
        System.err.println("Votre IDE ne transmet probablement pas le module-path JavaFX.");
        System.err.println();
        System.err.println("Ajoutez ces arguments VM à votre configuration d'exécution :");
        System.err.println("    --module-path \"C:/javafx-sdk/lib\"");
        System.err.println("    --add-modules javafx.controls");
        System.err.println();
        System.err.println("VS Code  : utilisez \"Run → Start Debugging\" (F5) pour que");
        System.err.println("           le fichier .vscode/launch.json soit pris en compte.");
        System.err.println("IntelliJ : Run → Edit Configurations → VM options.");
        System.err.println();
        System.err.println("Erreur sous-jacente :");
        err.printStackTrace();
    }
}
