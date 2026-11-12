@echo off
cd /d "%~dp0"
java --enable-native-access=javafx.graphics --module-path "lib" --add-modules javafx.controls,javafx.fxml -cp "lab-1.0.jar" com.daa.lab.MainApp
pause
