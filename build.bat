@echo off
echo ==========================================
echo   OceanBase Data Import System Builder
echo ==========================================
echo.

echo [1/3] Checking Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install JDK 17 or later
    pause
    exit /b 1
)

echo [2/3] Checking Maven...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Maven is not installed or not in PATH
    echo Please install Maven 3.6 or later
    pause
    exit /b 1
)

echo [3/3] Building project...
echo.

cd /d "%~dp0"

call mvn clean package -DskipTests

if errorlevel 1 (
    echo.
    echo BUILD FAILED!
    pause
    exit /b 1
)

echo.
echo ==========================================
echo   BUILD SUCCESSFUL!
echo ==========================================
echo.
echo The JAR file is located at:
echo   target\data-import-system-1.0.0.jar
echo.
echo To run the application:
echo   java -jar target\data-import-system-1.0.0.jar
echo.
echo IMPORTANT: Before running, make sure:
echo   1. OceanBase database is accessible
echo   2. Run schema-oracle.sql to create tables
echo.
pause
