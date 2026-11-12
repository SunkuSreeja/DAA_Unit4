/**
 * Default-package launcher so the app can be started with:
 *
 *   java --module-path lib\javafx\lib --add-modules javafx.controls,javafx.fxml -cp out Main
 *
 * This thin wrapper delegates immediately to the real application entry-point.
 */
public class Main {
    public static void main(String[] args) {
        com.daa.lab.MainApp.main(args);
    }
}

