@echo off
REM ============================================================
REM  compile.bat — Compiles the full project to out\
REM  Run this once before: java --module-path lib\javafx\lib ...
REM ============================================================

SET JAVAFX_LIB=%~dp0lib\javafx\lib
SET JAVAC=C:\Program Files\Java\jdk-26.0.1\bin\javac.exe
SET OUT=%~dp0out

echo.
echo  Compiling Robotics Path Planning Lab...
echo.

REM Create output directory
if not exist "%OUT%" mkdir "%OUT%"

REM Collect all .java source files
dir /s /b "%~dp0src\*.java" > "%TEMP%\sources.txt"
echo %~dp0Main.java >> "%TEMP%\sources.txt"

REM Compile with JavaFX on the module-path
"%JAVAC%" --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml -d "%OUT%" @"%TEMP%\sources.txt"

IF %ERRORLEVEL% EQU 0 (
    echo.
    echo   Compilation SUCCESS ^- class files written to: out\
    echo.
    echo   Run the app with:
    echo   java --module-path lib\javafx\lib --add-modules javafx.controls,javafx.fxml -cp out Main
    echo.
) ELSE (
    echo.
    echo   Compilation FAILED ^- check errors above.
    pause
)
