@echo off
echo ==========================================
echo   OceanBase Data Import System Starter
echo ==========================================
echo.

cd /d "%~dp0"

set JAR_FILE=target\data-import-system-1.0.0.jar

if not exist "%JAR_FILE%" (
    echo ERROR: JAR file not found!
    echo Please run build.bat first to build the project.
    pause
    exit /b 1
)

echo Starting OceanBase Data Import System...
echo.
echo The application will be available at:
echo   http://localhost:8080
echo.
echo Press Ctrl+C to stop the application.
echo.

java -jar "%JAR_FILE%"

if errorlevel 1 (
    echo.
    echo Application stopped with error!
    pause
)
