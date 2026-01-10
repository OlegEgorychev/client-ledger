# Как получить SHA-1 СЕЙЧАС (чтобы нажать Create)

## Способ 1: Получить SHA-1 из уже собранного APK ⭐ (САМЫЙ БЫСТРЫЙ)

Если у вас уже есть собранный APK:

1. Найдите APK в папке проекта:
   - `D:\Programs\test\app\build\outputs\apk\debug\app-debug.apk`
   
2. В Android Studio Terminal (или обычной командной строке) выполните:

**Если есть путь к keytool:**
```cmd
"%LOCALAPPDATA%\Android\Sdk\jbr\bin\keytool.exe" -list -printcert -jarfile "D:\Programs\test\app\build\outputs\apk\debug\app-debug.apk"
```

**Или если keytool в PATH:**
```cmd
keytool -list -printcert -jarfile "D:\Programs\test\app\build\outputs\apk\debug\app-debug.apk"
```

3. Найдите строку `SHA1:` в выводе
4. Скопируйте значение (например: `AA:BB:CC:DD:EE:FF:...`)

---

## Способ 2: Собрать APK и получить SHA-1

1. В Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Дождитесь сборки (несколько минут)
3. После сборки нажмите **"locate"** в уведомлении
4. APK будет в: `app\build\outputs\apk\debug\app-debug.apk`
5. Используйте Способ 1 выше с этим APK

---

## Способ 3: Использовать временное значение (обходной путь)

Если нужен SHA-1 прямо сейчас, можно ввести **временное значение**:

```
00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00
```

**Затем:**
1. Нажмите **"CREATE"** - OAuth Client ID создастся
2. Установите APK на устройство
3. Попробуйте войти в приложении
4. Получите ошибку с правильным SHA-1
5. Обновите OAuth Client ID с правильным SHA-1

---

## Способ 4: Через Android Studio Terminal напрямую (если APK есть)

1. В Android Studio откройте **Terminal** (вкладка внизу)
2. Введите `cmd` и нажмите Enter (переключитесь на CMD)
3. Проверьте, есть ли APK:

```cmd
dir "D:\Programs\test\app\build\outputs\apk\debug\app-debug.apk"
```

4. Если APK есть, выполните:

```cmd
cd D:\Programs\test
"%LOCALAPPDATA%\Android\Sdk\jbr\bin\keytool.exe" -list -printcert -jarfile "app\build\outputs\apk\debug\app-debug.apk"
```

5. Найдите строку `SHA1:` и скопируйте значение

---

## Способ 5: Если нет APK - создать через debug keystore

Попробуйте в Android Studio Terminal (после `cmd`):

```cmd
"%LOCALAPPDATA%\Android\Sdk\jbr\bin\keytool.exe" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android | findstr SHA1
```

Эта команда выведет только строку с SHA1.

---

## Рекомендация

**Если у вас нет собранного APK:**
1. Используйте **Способ 3** (временное значение `00:00:00:00...`)
2. Создайте OAuth Client ID
3. Соберите APK и попробуйте войти
4. Получите правильный SHA-1 через ошибку
5. Обновите OAuth Client ID

**Если APK уже есть:**
1. Используйте **Способ 1** или **Способ 4**
2. Получите SHA-1 из APK
3. Вставьте в форму
4. Нажмите Create

---

## Что делать после получения SHA-1

1. Скопируйте SHA-1 (например: `AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD`)
2. Вставьте в поле **"SHA-1 certificate fingerprint"** в Google Cloud Console
3. Можно вставлять:
   - С двоеточиями: `AA:BB:CC:DD:...`
   - Или без (удалите двоеточия): `AABBCCDD...`
   - Оба варианта работают!
4. Нажмите **"CREATE"** ✅
