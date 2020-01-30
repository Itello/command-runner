package se.itello.commandrunner;

/**
 * This is a Workaround. Since javafx got unbundled from java,
 * the main class can no longer extend from the Application class.
 */
public class Java11Main {

    public static void main(String[] args) {
        CommandRunner.main(args);
    }
}
