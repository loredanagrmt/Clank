@echo off
setlocal

echo ==========================================
echo  CLANK - EJECUCION FINAL DE TESTS ALLURE
echo ==========================================

REM 1. Preparar Java, Android SDK, ADB y Allure
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
set "ADB_CMD=%ANDROID_HOME%\platform-tools\adb.exe"
set "ALLURE_CMD=%USERPROFILE%\scoop\shims\allure.cmd"

set "PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%USERPROFILE%\scoop\shims;%PATH%"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: No se ha encontrado Java en:
    echo %JAVA_HOME%\bin\java.exe
    exit /b 1
)

if not exist "%ADB_CMD%" (
    echo ERROR: No se ha encontrado ADB en:
    echo %ADB_CMD%
    echo Revisa la ruta del SDK de Android.
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
echo Comprobando ADB...
"%ADB_CMD%" devices

echo.
echo Esperando dispositivo/emulador...
"%ADB_CMD%" wait-for-device

"%ADB_CMD%" get-state | findstr /i "device" >nul
if errorlevel 1 (
    echo ERROR: No hay ningun emulador/dispositivo en estado device.
    echo Abre el emulador CLANK y vuelve a ejecutar el script.
    exit /b 1
)

echo.
echo Comprobando Allure...
call "%ALLURE_CMD%" --version

echo.
echo 2. Limpiando carpetas de resultados locales...
if exist app\build\outputs\allure rmdir /s /q app\build\outputs\allure
if exist app\build\reports\allure-report rmdir /s /q app\build\reports\allure-report
if exist app\build\reports\allure-report.zip del /q app\build\reports\allure-report.zip

echo.
echo 3. Compilando app y APK de tests...
call gradlew.bat clean :app:assembleDebug :app:assembleDebugAndroidTest

if errorlevel 1 (
    echo ERROR: Fallo compilando el proyecto.
    exit /b 1
)

echo.
echo 3.1 Creando carpeta local para resultados Allure...
if not exist app\build\outputs mkdir app\build\outputs
if not exist app\build\outputs\allure mkdir app\build\outputs\allure

echo.
echo 4. Instalando app en el emulador/dispositivo...
"%ADB_CMD%" install -r app\build\outputs\apk\debug\app-debug.apk

if errorlevel 1 (
    echo ERROR: No se pudo instalar app-debug.apk.
    exit /b 1
)

echo.
echo 5. Instalando APK de tests...
"%ADB_CMD%" install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk

if errorlevel 1 (
    echo ERROR: No se pudo instalar app-debug-androidTest.apk.
    exit /b 1
)

echo.
echo 5.1 Limpiando datos previos de la app...
"%ADB_CMD%" shell pm clear com.clank.app

if errorlevel 1 (
    echo ERROR: No se pudieron limpiar los datos de com.clank.app.
    exit /b 1
)

echo.
echo 6. Limpiando resultados Allure anteriores del dispositivo...
"%ADB_CMD%" shell run-as com.clank.app rm -rf files/allure-results

echo.
echo 7. Ejecutando TODOS los tests instrumentados...
echo Esta parte puede tardar varios minutos. No cierres esta ventana.

"%ADB_CMD%" shell am instrument -w -r com.clank.app.test/com.clank.app.HiltTestRunner > app\build\outputs\allure\instrumentation-output.txt 2>&1

echo.
echo ===== SALIDA DE INSTRUMENTACION =====
type app\build\outputs\allure\instrumentation-output.txt
echo ===== FIN SALIDA DE INSTRUMENTACION =====

findstr /C:"FAILURES!!!" app\build\outputs\allure\instrumentation-output.txt >nul
if not errorlevel 1 (
    echo.
    echo ERROR: Han fallado tests instrumentados.
    echo Revisa:
    echo app\build\outputs\allure\instrumentation-output.txt
    exit /b 1
)

findstr /C:"INSTRUMENTATION_CODE: -1" app\build\outputs\allure\instrumentation-output.txt >nul
if not errorlevel 1 (
    echo.
    echo ERROR: La instrumentacion termino con codigo -1.
    echo Revisa:
    echo app\build\outputs\allure\instrumentation-output.txt
    exit /b 1
)

findstr /C:"INSTRUMENTATION_FAILED" app\build\outputs\allure\instrumentation-output.txt >nul
if not errorlevel 1 (
    echo.
    echo ERROR: La instrumentacion fallo antes de completar los tests.
    echo Revisa:
    echo app\build\outputs\allure\instrumentation-output.txt
    exit /b 1
)

findstr /C:"OK (" app\build\outputs\allure\instrumentation-output.txt >nul
if errorlevel 1 (
    echo.
    echo ERROR: No se encontro confirmacion OK de JUnit.
    echo Puede que la ejecucion no haya terminado correctamente.
    echo Revisa:
    echo app\build\outputs\allure\instrumentation-output.txt
    exit /b 1
)

echo.
echo Tests instrumentados completados correctamente.

echo.
echo 8. Comprobando resultados Allure en el dispositivo...
"%ADB_CMD%" shell run-as com.clank.app ls -la files/allure-results

if errorlevel 1 (
    echo ERROR: No se encontraron resultados Allure en el dispositivo.
    exit /b 1
)

echo.
echo 9. Extrayendo allure-results al ordenador...
"%ADB_CMD%" exec-out run-as com.clank.app sh -c "cd /data/data/com.clank.app/files && tar cf - allure-results" > app\build\outputs\allure\allure-results.tar

if errorlevel 1 (
    echo ERROR: No se pudieron extraer los resultados Allure del dispositivo.
    exit /b 1
)

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

if errorlevel 1 (
    echo ERROR: No se pudieron descomprimir los resultados Allure.
    exit /b 1
)

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

if errorlevel 1 (
    echo ERROR: No se pudo comprimir el reporte Allure.
    exit /b 1
)

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