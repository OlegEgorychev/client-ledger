# Получить SHA-1 - PowerShell команда

## Правильная команда для PowerShell (Android Studio Terminal)

В Android Studio Terminal (который использует PowerShell) используйте эту команду:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\jbr\bin\keytool.exe" -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

**ВАЖНО:** Скопируйте команду целиком (включая `&` в начале).

---

## Альтернатива: Если keytool не найден в стандартном месте

Найдите keytool вручную и используйте полный путь:

```powershell
# Сначала найдите keytool:
Get-ChildItem -Path "$env:LOCALAPPDATA\Android\Sdk" -Filter "keytool.exe" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
```

Эта команда выведет путь к keytool. Затем используйте этот путь:

```powershell
& "ПОЛНЫЙ_ПУТЬ_К_KEYTOOL" -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

---

## Самый простой способ: Через CMD

В Android Studio Terminal переключитесь на CMD:

1. В Terminal введите: `cmd`
2. Нажмите Enter
3. Теперь вы в CMD, выполните:

```cmd
"%LOCALAPPDATA%\Android\Sdk\jbr\bin\keytool.exe" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

---

## Еще проще: Используйте батник

1. Откройте Проводник Windows
2. Перейдите в папку `D:\Programs\test`
3. Найдите файл `get-sha1.bat`
4. **Дважды кликните** на него
5. Окно командной строки покажет SHA-1

Это самый надежный способ! 🚀
