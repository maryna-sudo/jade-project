@echo off
setlocal

set "ROOT_DIR=%~dp0..\.."
set "BIN_DIR=%ROOT_DIR%\bin"
set "SRC_DIR=%ROOT_DIR%\src"
set "LIB_JAR=%ROOT_DIR%\lib\jade.jar"
set "SOURCES_FILE=%TEMP%\jade_sources.txt"

if exist "%BIN_DIR%" rmdir /s /q "%BIN_DIR%"
mkdir "%BIN_DIR%"

dir /s /b "%SRC_DIR%\*.java" > "%SOURCES_FILE%"
javac -cp "%LIB_JAR%" -d "%BIN_DIR%" @"%SOURCES_FILE%"
if errorlevel 1 exit /b 1

echo Compiled classes into %BIN_DIR%
