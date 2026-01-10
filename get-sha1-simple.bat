@echo off
echo ========================================
echo   Получение SHA-1 для Client Ledger
echo ========================================
echo.

REM Простая версия - ищем keytool только в Android SDK
set KEYTOOL=%LOCALAPPDATA%\Android\Sdk\jbr\bin\keytool.exe

if not exist "%KEYTOOL%" (
    echo [ОШИБКА] keytool не найден!
    echo Путь: %KEYTOOL%
    echo.
    echo Решение: Запустите Android Studio хотя бы один раз
    echo.
    echo Нажмите любую клавишу для выхода...
    pause >nul
    exit /b 1
)

echo [OK] keytool найден
echo.

REM Проверяем keystore
if not exist "%USERPROFILE%\.android\debug.keystore" (
    echo [ОШИБКА] Debug keystore не найден!
    echo Путь: %USERPROFILE%\.android\debug.keystore
    echo.
    echo Решение: Запустите Android Studio и создайте проект
    echo.
    echo Нажмите любую клавишу для выхода...
    pause >nul
    exit /b 1
)

echo [OK] Debug keystore найден
echo.
echo Получаю SHA-1...
echo.

"%KEYTOOL%" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android

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
