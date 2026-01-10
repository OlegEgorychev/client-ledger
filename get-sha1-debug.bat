@echo off
echo ========================================
echo   Получение SHA-1 для Client Ledger
echo   (Debug версия - показывает все ошибки)
echo ========================================
echo.

REM Проверяем наличие keytool в разных местах
set KEYTOOL=

echo [1/4] Ищем keytool в Android SDK...
if exist "%LOCALAPPDATA%\Android\Sdk\jbr\bin\keytool.exe" (
    set KEYTOOL=%LOCALAPPDATA%\Android\Sdk\jbr\bin\keytool.exe
    echo [OK] keytool найден: %KEYTOOL%
    goto :found
)
echo [НЕТ] Не найден в Android SDK

echo.
echo [2/4] Ищем keytool в Program Files Java...
for /d %%i in ("C:\Program Files\Java\jdk*") do (
    if exist "%%i\bin\keytool.exe" (
        set KEYTOOL=%%i\bin\keytool.exe
        echo [OK] keytool найден: %KEYTOOL%
        goto :found
    )
)
echo [НЕТ] Не найден в Program Files Java

echo.
echo [3/4] Ищем keytool в JAVA_HOME...
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\keytool.exe" (
        set KEYTOOL=%JAVA_HOME%\bin\keytool.exe
        echo [OK] keytool найден: %KEYTOOL%
        goto :found
    )
    echo [НЕТ] JAVA_HOME установлен, но keytool не найден: %JAVA_HOME%\bin\keytool.exe
) else (
    echo [НЕТ] JAVA_HOME не установлен
)

echo.
echo [4/4] Ищем keytool в PATH...
where keytool >nul 2>&1
if %errorlevel% == 0 (
    where keytool
    set KEYTOOL=keytool
    echo [OK] keytool найден в PATH
    goto :found
)
echo [НЕТ] Не найден в PATH

echo.
echo ========================================
echo   [ОШИБКА] keytool не найден нигде!
echo ========================================
echo.
echo Решения:
echo 1. Откройте Android Studio хотя бы один раз (создаст SDK)
echo 2. Или установите Java JDK
echo 3. Или используйте альтернативный способ из SHA1_ПРОСТАЯ_ИНСТРУКЦИЯ.md
echo.
pause
exit /b 1

:found
echo.
echo ========================================
echo   Проверяем debug keystore...
echo ========================================
if exist "%USERPROFILE%\.android\debug.keystore" (
    echo [OK] Debug keystore найден: %USERPROFILE%\.android\debug.keystore
) else (
    echo [ОШИБКА] Debug keystore не найден!
    echo Путь: %USERPROFILE%\.android\debug.keystore
    echo.
    echo Решение: Запустите Android Studio и создайте проект
    echo (Android Studio автоматически создаст debug keystore)
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo   Получаю SHA-1 отпечаток...
echo ========================================
echo.

"%KEYTOOL%" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android

if errorlevel 1 (
    echo.
    echo [ОШИБКА] Команда keytool завершилась с ошибкой!
    echo Код ошибки: %errorlevel%
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo   ДАННЫЕ ДЛЯ GOOGLE CLOUD CONSOLE:
echo ========================================
echo Package name: com.clientledger.app
echo SHA-1: (см. строку SHA1: выше в выводе)
echo.
echo Скопируйте значение SHA1 из строки выше.
echo ========================================
echo.
echo Нажмите любую клавишу для выхода...
pause >nul
