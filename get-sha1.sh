#!/bin/bash
# Скрипт для получения SHA-1 отпечатка подписи APK
# Для Linux/Mac

echo "=== Получение SHA-1 отпечатка для Client Ledger ==="
echo ""

# Определяем путь к debug keystore
DEBUG_KEYSTORE="$HOME/.android/debug.keystore"

echo "Ищем debug keystore..."

if [ -f "$DEBUG_KEYSTORE" ]; then
    echo "✓ Debug keystore найден: $DEBUG_KEYSTORE"
    echo ""
    echo "Получаем SHA-1 отпечаток..."
    echo ""
    
    # Получаем SHA-1
    SHA1_OUTPUT=$(keytool -list -v -keystore "$DEBUG_KEYSTORE" -alias androiddebugkey -storepass android -keypass android 2>&1)
    
    if [ $? -eq 0 ]; then
        # Ищем SHA-1 в выводе
        SHA1_LINE=$(echo "$SHA1_OUTPUT" | grep "SHA1:")
        
        if [ -n "$SHA1_LINE" ]; then
            echo "═══════════════════════════════════════════════════════"
            echo "  SHA-1 для Debug APK:"
            echo "═══════════════════════════════════════════════════════"
            echo ""
            echo "$SHA1_LINE"
            echo ""
            
            # Извлекаем только SHA-1 без префикса
            SHA1_VALUE=$(echo "$SHA1_LINE" | sed 's/.*SHA1: *//' | tr -d ' ')
            echo "SHA-1 значение (для вставки в Google Cloud Console):"
            echo "$SHA1_VALUE"
            echo ""
            
            echo "═══════════════════════════════════════════════════════"
            echo "  Данные для Google Cloud Console:"
            echo "═══════════════════════════════════════════════════════"
            echo ""
            echo -n "Package name: "
            echo "com.clientledger.app"
            echo ""
            echo -n "SHA-1 fingerprint: "
            echo "$SHA1_VALUE"
            echo ""
            
            # Сохраняем в файл для удобства
            OUTPUT_FILE="sha1-info.txt"
            cat > "$OUTPUT_FILE" << EOF
SHA-1 информация для Google Cloud Console
==========================================

Package name: com.clientledger.app
SHA-1 fingerprint: $SHA1_VALUE

Скопируйте эти данные для создания OAuth Client ID в Google Cloud Console.
EOF
            
            echo "✓ Данные сохранены в файл: $OUTPUT_FILE"
            echo ""
            
        else
            echo "✗ Не удалось найти SHA-1 в выводе keytool"
        fi
    else
        echo "✗ Ошибка при выполнении keytool"
        echo "$SHA1_OUTPUT"
    fi
else
    echo "✗ Debug keystore не найден по пути: $DEBUG_KEYSTORE"
    echo ""
    echo "Возможные решения:"
    echo "1. Запустите Android Studio хотя бы один раз"
    echo "2. Или создайте debug keystore вручную"
    echo ""
fi

echo ""
echo "Для Release APK используйте команду:"
echo "keytool -list -v -keystore путь/к/вашему/release.keystore -alias ваш_alias"
echo ""
