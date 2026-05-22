@echo off
setlocal

echo ==========================================
echo  CLANK - EJECUCION FINAL DE TESTS ALLURE
echo ==========================================

REM 1. Preparar Java y Allure
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%USERPROFILE%\scoop\shims;%PATH%"

set "ALLURE_CMD=%USERPROFILE%\scoop\shims\allure.cmd"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: No se ha encontrado Java en:
    echo %JAVA_HOME%\bin\java.exe
    exit /b 1
)

if not exist "%ALLURE_CMD%" (
    echo ERROR: No se ha encontrado Allure en:
    echo %ALLURE_CMD%
    echo Instala Allure con: scoop install allure
    exit /b 1
)

echo.
echo Comprobando Java...
java -version

echo.
echo Comprobando Allure...
call "%ALLURE_CMD%" --version

echo.
echo 2. Limpiando carpetas de resultados locales...
if exist app\build\outputs\allure rmdir /s /q app\build\outputs\allure
if exist app\build\reports\allure-report rmdir /s /q app\build\reports\allure-report
if exist app\build\reports\allure-report.zip del /q app\build\reports\allure-report.zip

mkdir app\build\outputs\allure

echo.
echo 3. Compilando app y APK de tests...
call gradlew.bat clean :app:assembleDebug :app:assembleDebugAndroidTest

if errorlevel 1 (
    echo ERROR: Fallo compilando el proyecto.
    exit /b 1
)

echo.
echo 4. Instalando app en el emulador/dispositivo...
adb install -r app\build\outputs\apk\debug\app-debug.apk

if errorlevel 1 (
    echo ERROR: No se pudo instalar app-debug.apk.
    exit /b 1
)

echo.
echo 5. Instalando APK de tests...
adb install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk

if errorlevel 1 (
    echo ERROR: No se pudo instalar app-debug-androidTest.apk.
    exit /b 1
)

echo.
echo 6. Limpiando resultados Allure anteriores del dispositivo...
adb shell run-as com.clank.app rm -rf files/allure-results

echo.
echo 7. Ejecutando TODOS los tests instrumentados...
adb shell am instrument -w -r com.clank.app.test/com.clank.app.HiltTestRunner

if errorlevel 1 (
    echo ERROR: Han fallado los tests instrumentados.
    echo Revisa la salida anterior antes de generar el reporte.
    exit /b 1
)

echo.
echo 8. Comprobando resultados Allure en el dispositivo...
adb shell run-as com.clank.app ls -la files/allure-results

if errorlevel 1 (
    echo ERROR: No se encontraron resultados Allure en el dispositivo.
    exit /b 1
)

echo.
echo 9. Extrayendo allure-results al ordenador...
adb exec-out run-as com.clank.app sh -c "cd /data/data/com.clank.app/files && tar cf - allure-results" > app\build\outputs\allure\allure-results.tar

echo.
echo 10. Validando archivo TAR...
tar -tf app\build\outputs\allure\allure-results.tar

if errorlevel 1 (
    echo ERROR: El archivo TAR de Allure no es valido.
    exit /b 1
)

echo.
echo 11. Descomprimiendo resultados...
tar -xf app\build\outputs\allure\allure-results.tar -C app\build\outputs\allure

echo.
echo 12. Generando reporte HTML de Allure...
call "%ALLURE_CMD%" generate app\build\outputs\allure\allure-results --clean -o app\build\reports\allure-report

if errorlevel 1 (
    echo ERROR: No se pudo generar el reporte Allure.
    exit /b 1
)

echo.
echo 13. Comprimiendo reporte para entrega...
powershell -Command "Compress-Archive -Path 'app\build\reports\allure-report\*' -DestinationPath 'app\build\reports\allure-report.zip' -Force"

echo.
echo ==========================================
echo  PROCESO TERMINADO CORRECTAMENTE
echo ==========================================
echo.
echo Reporte HTML:
echo app\build\reports\allure-report
echo.
echo Reporte ZIP para entregar:
echo app\build\reports\allure-report.zip
echo.

echo Abriendo reporte Allure...
call "%ALLURE_CMD%" open app\build\reports\allure-report

endlocal