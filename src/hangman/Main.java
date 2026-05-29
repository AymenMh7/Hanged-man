package hangman;

import hangman.ui.GameWindow;
import javafx.application.Application;

/**
 * Application entry point — works whether launched from:
 *
 *   • The compile.bat / run.bat scripts (classpath set up for you), OR
 *   • Your IDE (VS Code, IntelliJ IDEA, Eclipse), provided JavaFX +
 *     the MySQL connector are on the project classpath and the JavaFX
 *     module-path VM args are set.
 *
 * Why this class doesn't extend {@link Application}:
 *   The JVM checks the launched class. If it extends Application and
 *   JavaFX modules aren't present, it dies with
 *   "JavaFX runtime components are missing" before any of your code
 *   gets to run. Using a separate launcher dodges that check, so we
 *   can catch the underlying error here and print a friendly hint.
 *
 * IDE setup quick-reference (Windows defaults):
 *   - VM args:  --module-path "C:/javafx-sdk/lib" --add-modules javafx.controls
 *   - Libraries:  lib/mysql-connector-*.jar  +  C:/javafx-sdk/lib/*.jar
 *
 * VS Code: the included .vscode/launch.json + .vscode/settings.json
 *          should do this automatically. Use "Run → Start Debugging"
 *          (F5) so the launch.json config is picked up.
 *
 * IntelliJ IDEA: see the "Run from your IDE" section of README.md.
 */
public class Main {

    public static void main(String[] args) {
        try {
            Application.launch(GameWindow.class, args);
        } catch (LinkageError err) {
            // LinkageError covers NoClassDefFoundError, UnsatisfiedLinkError,
            // and the other classloading failures JavaFX can hit.
            printJavaFxHint(err);
        } catch (RuntimeException ex) {
            // Catches the IllegalStateException JavaFX throws when the
            // toolkit can't initialize at all.
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
        System.err.println("  JavaFX could not start.");
        System.err.println("============================================================");
        System.err.println();
        System.err.println("Your IDE probably isn't passing the JavaFX module path.");
        System.err.println();
        System.err.println("Add these VM arguments to your run configuration:");
        System.err.println("    --module-path \"C:/javafx-sdk/lib\"");
        System.err.println("    --add-modules javafx.controls");
        System.err.println();
        System.err.println("VS Code:  use \"Run → Start Debugging\" (F5) so the");
        System.err.println("          included .vscode/launch.json is picked up.");
        System.err.println("IntelliJ: Run → Edit Configurations → VM options.");
        System.err.println();
        System.err.println("Underlying error:");
        err.printStackTrace();
    }
}
