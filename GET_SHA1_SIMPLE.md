# Как получить SHA-1 отпечаток - ПРОСТЫЕ СПОСОБЫ

## Способ 1: Через Terminal в Android Studio ⭐ (САМЫЙ ПРОСТОЙ)

1. Откройте **Android Studio**
2. Внизу нажмите вкладку **"Terminal"** (если её нет: View → Tool Windows → Terminal)
3. В терминале выполните команду:

**Windows:**
```bash
keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
```

**Linux/Mac:**
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

4. Найдите строку **`SHA1:`** в выводе
5. Скопируйте значение после `SHA1:` (можно с двоеточиями: `AA:BB:CC:DD:...`)

---

## Способ 2: Через Gradle Task (добавлено в проект)

1. Откройте **Android Studio**
2. Справа откройте панель **"Gradle"** (View → Tool Windows → Gradle)
3. В дереве найдите: **app → Tasks → other**
4. Найдите задачу **"printSha1"** (должна появиться после синхронизации)
5. Дважды кликните на **"printSha1"**
6. Внизу в панели **"Run"** или **"Build"** увидите SHA-1

**Если задачи нет:**
- Нажмите **File → Sync Project with Gradle Files** (иконка слоненка вверху)
- Подождите завершения синхронизации
- Попробуйте снова

---

## Способ 3: Через командную строку проекта

1. Откройте терминал (PowerShell или CMD) в папке проекта `D:\Programs\test`
2. Выполните:

**Windows PowerShell:**
```powershell
.\gradlew.bat app:printSha1
```

**Linux/Mac:**
```bash
./gradlew app:printSha1
```

3. В выводе найдите строку **`SHA1:`**

---

## Способ 4: Если APK уже установлен на устройстве (через ADB)

1. Подключите устройство к компьютеру
2. Включите **"Отладка по USB"** на устройстве
3. В терминале Android Studio выполните:

```bash
adb shell pm list packages | grep clientledger
adb shell dumpsys package com.clientledger.app | grep -A 1 "signatures"
```

Или проще:

```bash
adb shell dumpsys package com.clientledger.app | findstr SHA
```

(Этот способ может не работать, так как нужны root права)

---

## Способ 5: Если у вас есть уже собранный APK

1. Соберите APK: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. После сборки внизу появится уведомление **"APK(s) generated successfully"**
3. Нажмите **"locate"**
4. Найденный APK будет в: `app/build/outputs/apk/debug/app-debug.apk`
5. В терминале выполните:

```bash
keytool -list -printcert -jarfile app/build/outputs/apk/debug/app-debug.apk
```

6. Найдите строку **`SHA1:`**

---

## Что делать если keytool не найден?

### Вариант A: Найти keytool в установленном JDK

Обычно keytool находится в:
- `C:\Program Files\Java\jdk-XX\bin\keytool.exe`
- Или в Android Studio: `C:\Users\ВашеИмя\AppData\Local\Android\Sdk\jbr\bin\keytool.exe`

Добавьте полный путь к команде:

**Windows:**
```powershell
"C:\Program Files\Java\jdk-17\bin\keytool.exe" -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

### Вариант B: Использовать keytool из Android Studio

В Android Studio Terminal:

**Windows:**
```bash
"%LOCALAPPDATA%\Android\Sdk\jbr\bin\keytool.exe" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

---

## Пример вывода keytool

Когда выполните команду, вы увидите что-то вроде:

```
...
Certificate fingerprints:
         SHA1: AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD
         SHA256: ...
...
```

**Скопируйте строку:** `AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD`

---

## Данные для Google Cloud Console

После получения SHA-1, используйте эти данные:

```
Package name: com.clientledger.app
SHA-1 fingerprint: [ваш SHA-1 из команды выше]
```

---

## Если ничего не помогает

**Просто создайте OAuth Client ID в Google Cloud Console БЕЗ SHA-1:**

1. Создайте OAuth Client ID типа "Android"
2. Package name: `com.clientledger.app`
3. **SHA-1 можно оставить пустым или ввести любой** (для тестирования)
4. После создания, попробуйте войти в приложении
5. Если получите ошибку `DEVELOPER_ERROR`, Google покажет правильный SHA-1 в сообщении об ошибке
6. Обновите OAuth Client ID с правильным SHA-1

---

**Рекомендую начать со Способа 1 (Terminal в Android Studio) - это самый надежный!** 🚀
