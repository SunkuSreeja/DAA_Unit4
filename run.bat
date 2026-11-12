@echo off
REM ============================================================
REM  run.bat — Runs the app directly with java (no Maven)
REM  Make sure you ran compile.bat first!
REM ============================================================

SET JAVAFX_LIB=%~dp0lib\javafx\lib
SET JAVA=C:\Program Files\Java\jdk-26.0.1\bin\java.exe

"%JAVA%" ^
  --module-path "%JAVAFX_LIB%" ^
  --add-modules javafx.controls,javafx.fxml ^
  --enable-native-access=javafx.graphics ^
  -cp "%~dp0out" ^
  Main

IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: App failed. Did you run compile.bat first?
    pause
)
