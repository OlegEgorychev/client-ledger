# Скрипт для получения SHA-1 отпечатка подписи APK
# Для Windows PowerShell

Write-Host "=== Получение SHA-1 отпечатка для Client Ledger ===" -ForegroundColor Green
Write-Host ""

# Определяем путь к debug keystore
$debugKeystorePath = "$env:USERPROFILE\.android\debug.keystore"

Write-Host "Ищем debug keystore..." -ForegroundColor Yellow

if (Test-Path $debugKeystorePath) {
    Write-Host "✓ Debug keystore найден: $debugKeystorePath" -ForegroundColor Green
    Write-Host ""
    Write-Host "Получаем SHA-1 отпечаток..." -ForegroundColor Yellow
    Write-Host ""
    
    # Получаем SHA-1
    $sha1Output = keytool -list -v -keystore $debugKeystorePath -alias androiddebugkey -storepass android -keypass android 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        # Ищем SHA-1 в выводе
        $sha1Line = $sha1Output | Select-String "SHA1:"
        
        if ($sha1Line) {
            Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
            Write-Host "  SHA-1 для Debug APK:" -ForegroundColor White
            Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
            Write-Host ""
            Write-Host $sha1Line.ToString().Trim() -ForegroundColor Yellow
            Write-Host ""
            
            # Извлекаем только SHA-1 без префикса
            $sha1Value = ($sha1Line.ToString() -replace ".*SHA1:\s*", "").Trim()
            Write-Host "SHA-1 значение (для вставки в Google Cloud Console):" -ForegroundColor White
            Write-Host $sha1Value -ForegroundColor Green -BackgroundColor Black
            Write-Host ""
            
            Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
            Write-Host "  Данные для Google Cloud Console:" -ForegroundColor White
            Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
            Write-Host ""
            Write-Host "Package name: " -NoNewline -ForegroundColor White
            Write-Host "com.clientledger.app" -ForegroundColor Yellow
            Write-Host ""
            Write-Host "SHA-1 fingerprint: " -NoNewline -ForegroundColor White
            Write-Host $sha1Value -ForegroundColor Yellow
            Write-Host ""
            
            # Сохраняем в файл для удобства
            $outputFile = "sha1-info.txt"
            @"
SHA-1 информация для Google Cloud Console
==========================================

Package name: com.clientledger.app
SHA-1 fingerprint: $sha1Value

Скопируйте эти данные для создания OAuth Client ID в Google Cloud Console.
"@ | Out-File -FilePath $outputFile -Encoding UTF8
            
            Write-Host "✓ Данные сохранены в файл: $outputFile" -ForegroundColor Green
            Write-Host ""
            
        } else {
            Write-Host "✗ Не удалось найти SHA-1 в выводе keytool" -ForegroundColor Red
        }
    } else {
        Write-Host "✗ Ошибка при выполнении keytool" -ForegroundColor Red
        Write-Host $sha1Output -ForegroundColor Red
    }
} else {
    Write-Host "✗ Debug keystore не найден по пути: $debugKeystorePath" -ForegroundColor Red
    Write-Host ""
    Write-Host "Возможные решения:" -ForegroundColor Yellow
    Write-Host "1. Запустите Android Studio хотя бы один раз" -ForegroundColor White
    Write-Host "2. Или создайте debug keystore вручную" -ForegroundColor White
    Write-Host ""
}

Write-Host ""
Write-Host "Для Release APK используйте команду:" -ForegroundColor Cyan
Write-Host "keytool -list -v -keystore путь/к/вашему/release.keystore -alias ваш_alias" -ForegroundColor Yellow
Write-Host ""
