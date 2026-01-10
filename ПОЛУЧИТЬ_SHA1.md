# Как получить SHA-1 отпечаток - ПРОСТЫЕ СПОСОБЫ

## Способ 1: Через Gradle Task в Android Studio ⭐ (ТЕПЕРЬ ДОСТУПЕН)

Я добавил задачу `printSha1` в ваш проект. Сделайте:

1. В Android Studio нажмите **File → Sync Project with Gradle Files** (иконка слоненка 🔄 вверху)
2. Подождите завершения синхронизации
3. Справа откройте панель **"Gradle"**
4. Найдите: **app → Tasks → other → printSha1**
5. **Дважды кликните** на `printSha1`
6. Внизу в панели **"Build"** или **"Run"** увидите SHA-1

---

## Способ 2: Через Terminal в Android Studio ⭐⭐ (САМЫЙ ПРОСТОЙ)

1. В Android Studio внизу откройте вкладку **"Terminal"** 
   (если нет: View → Tool Windows → Terminal)

2. В терминале выполните:

**Windows:**
```bash
keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
```

**Если keytool не найден, используйте полный путь:**
```bash
"%LOCALAPPDATA%\Android\Sdk\jbr\bin\keytool.exe" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

3. Найдите строку **`SHA1:`** в выводе
4. Скопируйте значение (например: `AA:BB:CC:DD:EE:FF:...`)

---

## Способ 3: Через командную строку проекта

1. Откройте PowerShell в папке проекта `D:\Programs\test`
2. Выполните:
```powershell
.\gradlew.bat app:printSha1
```

---

## Способ 4: Если ничего не помогает - создайте APK и получите SHA-1 из него

1. В Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Дождитесь сборки
3. Внизу появится уведомление **"APK(s) generated successfully"** → нажмите **"locate"**
4. APK будет в: `app/build/outputs/apk/debug/app-debug.apk`
5. В Terminal выполните:

**Windows:**
```bash
"%LOCALAPPDATA%\Android\Sdk\jbr\bin\keytool.exe" -list -printcert -jarfile app\build\outputs\apk\debug\app-debug.apk
```

**Или если keytool в PATH:**
```bash
keytool -list -printcert -jarfile app/build/outputs/apk/debug/app-debug.apk
```

6. Найдите строку **`SHA1:`**

---

## Что делать если keytool не найден?

### Вариант A: Использовать keytool из Android Studio

В Android Studio Terminal используйте полный путь:

**Windows:**
```bash
"%LOCALAPPDATA%\Android\Sdk\jbr\bin\keytool.exe" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

### Вариант B: Найти в установленном JDK

Обычно находится в:
- `C:\Program Files\Java\jdk-XX\bin\keytool.exe`

---

## Пример вывода

После выполнения команды вы увидите:

```
Alias name: androiddebugkey
Creation date: ...
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Android Debug, O=Android, C=US
Issuer: CN=Android Debug, O=Android, C=US
Serial number: ...
Valid from: ... until: ...
Certificate fingerprints:
         SHA1: AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD
         SHA256: ...
Signature algorithm name: ...
```

**Скопируйте:** `AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD`

---

## Данные для Google Cloud Console

```
Package name: com.clientledger.app
SHA-1 fingerprint: [ваш SHA-1 из любого способа выше]
```

---

## Рекомендация

**Начните со Способа 2 (Terminal в Android Studio)** - это самый быстрый и надежный способ! 🚀
