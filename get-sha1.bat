@echo off
echo ========================================
echo   Получение SHA-1 для Client Ledger
echo ========================================
echo.

REM Проверяем наличие keytool в разных местах
set KEYTOOL=

REM 1. Проверяем в Android SDK
if exist "%LOCALAPPDATA%\Android\Sdk\jbr\bin\keytool.exe" (
    set KEYTOOL=%LOCALAPPDATA%\Android\Sdk\jbr\bin\keytool.exe
    echo [OK] keytool найден в Android SDK
    goto :found
)

REM 2. Проверяем в Program Files Java
for /d %%i in ("C:\Program Files\Java\jdk*") do (
    if exist "%%i\bin\keytool.exe" (
        set KEYTOOL=%%i\bin\keytool.exe
        echo [OK] keytool найден в Java JDK
        goto :found
    )
)

REM 3. Проверяем в JAVA_HOME
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\keytool.exe" (
        set KEYTOOL=%JAVA_HOME%\bin\keytool.exe
        echo [OK] keytool найден в JAVA_HOME
        goto :found
    )
)

echo [ОШИБКА] keytool не найден!
echo.
echo Решения:
echo 1. Откройте Android Studio
echo 2. Или установите Java JDK
echo.
pause
exit /b 1

:found
echo.
echo Получаю SHA-1 отпечаток...
echo.

echo Выполняю keytool...
echo.

"%KEYTOOL%" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android 2>&1 | findstr /i "SHA1"

if errorlevel 1 (
    echo.
    echo [ОШИБКА] Не удалось получить SHA-1!
    echo.
    echo Возможные причины:
    echo 1. Debug keystore не существует
    echo 2. keytool вернул ошибку
    echo.
    echo Попробуйте другой способ из инструкции SHA1_ПРОСТАЯ_ИНСТРУКЦИЯ.md
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo   Данные для Google Cloud Console:
echo ========================================
echo Package name: com.clientledger.app
echo SHA-1: (см. строку SHA1: выше)
echo ========================================
echo.
echo Нажмите любую клавишу для выхода...
pause >nul
