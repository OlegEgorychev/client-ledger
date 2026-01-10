# Настройка Google Drive API для APK приложения

Это пошаговая инструкция по настройке Google Drive API для приложения, распространяемого через APK (без публикации в Google Play Store).

## Шаг 1: Получение SHA-1 отпечатка подписи APK

### Для Debug APK:
```bash
# Windows (PowerShell)
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android | Select-String "SHA1"

# Linux/Mac
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
```

### Для Release APK:
Если у вас есть release keystore, используйте:
```bash
keytool -list -v -keystore путь/к/вашему/keystore.jks -alias ваш_alias
```

**Важно**: Вы получите строку вида `SHA1: AA:BB:CC:DD:...` — скопируйте её полностью (без пробела после двоеточия).

## Шаг 2: Создание проекта в Google Cloud Console

1. Перейдите на [Google Cloud Console](https://console.cloud.google.com/)
2. Войдите в свой Google аккаунт
3. Нажмите "Создать проект" (Create Project)
4. Введите название проекта (например, "Client Ledger Backup")
5. Нажмите "Создать"

## Шаг 3: Включение Google Drive API

1. В боковом меню выберите **"APIs & Services" → "Library"**
2. В поиске введите **"Google Drive API"**
3. Нажмите на "Google Drive API"
4. Нажмите кнопку **"Enable"** (Включить)

## Шаг 4: Создание OAuth 2.0 Client ID

1. В боковом меню выберите **"APIs & Services" → "Credentials"**
2. Нажмите **"+ CREATE CREDENTIALS"** → **"OAuth client ID"**
3. Если появится предупреждение "OAuth consent screen", нажмите **"Configure consent screen"**:
   - Выберите **"External"** (если не используется G Suite)
   - Заполните обязательные поля:
     - **App name**: Client Ledger
     - **User support email**: ваш email
     - **Developer contact email**: ваш email
   - Нажмите **"Save and Continue"**
   - На шаге "Scopes" нажмите **"Save and Continue"**
   - На шаге "Test users" (опционально) добавьте тестовые email-адреса
   - Нажмите **"Back to Dashboard"**
4. Вернитесь к созданию OAuth Client ID:
   - **Application type**: выберите **"Android"**
   - **Name**: Client Ledger Android
   - **Package name**: `com.clientledger.app` (убедитесь, что совпадает с вашим `applicationId` в `build.gradle.kts`)
   - **SHA-1 certificate fingerprint**: вставьте SHA-1 отпечаток, полученный на шаге 1 (без пробелов и двоеточий, например: `AABBCCDDEEFF...`)
   - Нажмите **"Create"**
5. Скопируйте **Client ID** (строка вида `XXXXX.apps.googleusercontent.com`)

## Шаг 5: Проверка конфигурации (опционально)

**Важно**: Google Play Services автоматически определяет OAuth Client ID на основе:
- Package name приложения (`com.clientledger.app`)
- SHA-1 отпечатка подписи APK

Если в Google Cloud Console настроен OAuth Client ID с правильными package name и SHA-1, **дополнительная настройка в коде не требуется**.

Строка `google_oauth_client_id` в `strings.xml` оставлена для будущего использования (например, для валидации), но не используется в текущей реализации.

## Шаг 6: Сборка и установка APK

После настройки Client ID:

1. Соберите новый APK:
   ```bash
   ./gradlew assembleDebug
   # или для release
   ./gradlew assembleRelease
   ```
2. Установите APK на устройство
3. Откройте приложение → Настройки → Резервное копирование
4. Нажмите **"Войти в Google Drive"**
5. Выберите свой Google аккаунт и разрешите доступ

## Важные замечания

### Для Debug и Release APK нужны разные Client ID

Если вы используете разные подписи для debug и release APK:
- Создайте **два разных OAuth Client ID** в Google Cloud Console
- Один с SHA-1 от debug keystore (для тестирования)
- Один с SHA-1 от release keystore (для продакшена)
- Используйте разные строковые ресурсы или buildConfig для разных build types

### Альтернативный вариант (buildConfig)

Если нужно использовать разные Client ID для debug и release, можно использовать `BuildConfig`:

1. В `app/build.gradle.kts` добавьте:
   ```kotlin
   android {
       buildTypes {
           debug {
               buildConfigField("String", "GOOGLE_CLIENT_ID", "\"DEBUG_CLIENT_ID.apps.googleusercontent.com\"")
           }
           release {
               buildConfigField("String", "GOOGLE_CLIENT_ID", "\"RELEASE_CLIENT_ID.apps.googleusercontent.com\"")
           }
       }
   }
   ```
2. В коде используйте `BuildConfig.GOOGLE_CLIENT_ID` вместо строкового ресурса

### Безопасность

⚠️ **Важно**: Client ID не является секретным ключом, его можно хранить в коде приложения. Однако для дополнительной безопасности рекомендуется использовать ProGuard/R8 для обфускации в release сборках.

## Проверка работы

После настройки:
1. Запустите приложение
2. Добавьте/измените клиента или запись
3. Подождите 2 секунды (debounce)
4. Проверьте Google Drive → папку "ClientLedger Backups"
5. Убедитесь, что файл `backup_001_YYYY-MM-DD_HH-mm.json` появился в Drive

## Устранение проблем

### Ошибка "DEVELOPER_ERROR" или "10"
- Проверьте, что SHA-1 отпечаток совпадает с тем, что был указан в OAuth Client ID
- Проверьте, что package name (`com.clientledger.app`) совпадает с `applicationId` в `build.gradle.kts`

### Ошибка "SIGN_IN_FAILED"
- Проверьте, что Google Drive API включена в Google Cloud Console
- Проверьте, что OAuth consent screen настроен правильно
- Убедитесь, что вы используете правильный Client ID

### Ошибка "NETWORK_ERROR"
- Проверьте интернет-соединение
- Убедитесь, что устройство может обращаться к Google серверам

### Кнопка "Войти в Google Drive" не появляется или показывает "Не настроен"
- Убедитесь, что Google Play Services установлены и обновлены на устройстве
- Проверьте, что OAuth Client ID правильно настроен в Google Cloud Console с правильными package name и SHA-1
- Проверьте интернет-соединение

### Google Play Services автоматически находит Client ID
Google Play Services использует package name и SHA-1 для автоматического определения правильного OAuth Client ID. Убедитесь, что:
- Package name в `build.gradle.kts` (`applicationId`) совпадает с package name в OAuth Client ID
- SHA-1 отпечаток подписи APK совпадает с SHA-1 в OAuth Client ID

## Полезные ссылки

- [Google Cloud Console](https://console.cloud.google.com/)
- [Google Drive API Documentation](https://developers.google.com/drive/api/v3/about-sdk)
- [OAuth 2.0 для Android](https://developers.google.com/identity/protocols/oauth2/native-app)
