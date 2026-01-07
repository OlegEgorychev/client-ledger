# UX Design: Import Client from Phone Contacts

## Overview
This document describes the UX flow, UI structure, and permission handling logic for importing a client from phone contacts in the Android client management app.

---

## 1. UX Flow Description

### Entry Point
- **Location**: `ClientEditScreen` (when `clientId == null`, i.e., creating a new client)
- **Trigger**: Secondary action button "Импорт из контактов" (Import from contacts)

### Flow Steps

#### Step 1: User Taps "Import from Contacts"
- User is on the "New Client" screen (`ClientEditScreen` with `clientId == null`)
- User taps the "Импорт из контактов" button
- **State**: Button shows loading indicator (optional, for permission check)

#### Step 2: Permission Check
- **If permission NOT granted:**
  - Show Android system permission dialog: "Allow [App Name] to access your contacts?"
  - User can choose "Allow" or "Deny"
  
- **If permission DENIED:**
  - Show Snackbar: "Для импорта контакта необходимо разрешение на доступ к контактам"
  - (English: "Contact access permission is required to import contacts")
  - User can manually grant permission in Settings
  - Flow stops here

- **If permission GRANTED:**
  - Proceed to Step 3

#### Step 3: Open System Contact Picker
- Launch Android system Contact Picker using `Intent.ACTION_PICK` with `ContactsContract.CommonDataKinds.Phone.CONTENT_URI`
- User sees native Android contact picker (not custom UI)
- User can search, scroll, and select a contact

#### Step 4: Contact Selection
- User selects a contact from the picker
- App receives contact URI via `ActivityResultLauncher`

#### Step 5: Extract Contact Data
- Extract from selected contact:
  - **Full name**: `ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME`
    - Split into `firstName` and `lastName` (if possible, otherwise put full name in `lastName`)
  - **Phone number** (REQUIRED): `ContactsContract.CommonDataKinds.Phone.NUMBER`
    - Format to E.164 if needed
  - **Email** (optional): `ContactsContract.CommonDataKinds.Email.ADDRESS`

#### Step 6: Handle Edge Cases

**Case A: Contact has NO phone number**
- Show AlertDialog:
  - Title: "Невозможно импортировать"
  - Message: "У выбранного контакта нет номера телефона. Выберите другой контакт."
  - Button: "OK"
- Do NOT navigate to edit screen
- User can try selecting another contact

**Case B: Contact has MULTIPLE phone numbers**
- Show AlertDialog with list of phone numbers:
  - Title: "Выберите номер телефона"
  - Message: "У контакта несколько номеров:"
  - List of phone numbers (with labels: Mobile, Work, Home, etc.)
  - Buttons: "Отмена" and for each number: "[Label]: [Number]"
- User selects one number
- Proceed with selected number

**Case C: Contact has phone number**
- Proceed to Step 7

#### Step 7: Pre-fill ClientEditScreen
- Navigate to `ClientEditScreen` with `clientId = null`
- Pre-fill fields:
  - `firstName`: Extracted first name (or first word of display name)
  - `lastName`: Extracted last name (or remaining words, or full name if can't split)
  - `phone`: Selected phone number (formatted to E.164)
  - `telegram`: Empty (not available from contacts)
  - `notes`: Empty
  - `gender`: Default "male"
  - `birthDate`: Empty (not available from contacts)
- **Important**: All fields remain **editable**
- **Important**: Client is **NOT auto-saved**
- User must tap "Сохранить" to create the client

#### Step 8: User Edits and Saves
- User can modify any pre-filled fields
- User taps "Сохранить" button
- Standard validation and save flow proceeds
- If phone number already exists: show error "Клиент с таким телефоном уже существует"

---

## 2. UI Structure

### 2.1 ClientEditScreen Modifications

#### Current Structure
```
Scaffold
└── TopAppBar
    ├── Title: "Новый клиент" / "Редактировать клиента"
    └── NavigationIcon: "Назад"
└── Column (Content)
    ├── OutlinedTextField: Имя *
    ├── OutlinedTextField: Фамилия *
    ├── Text: Пол
    ├── Row: [FilterChip: Муж] [FilterChip: Жен]
    ├── OutlinedTextField: Дата рождения
    ├── PhoneInput: Телефон
    ├── OutlinedTextField: Telegram
    ├── OutlinedTextField: Заметки
    ├── Text: Error message (if any)
    └── Button: Сохранить
```

#### New Structure (with Import Button)
```
Scaffold
└── TopAppBar
    ├── Title: "Новый клиент" / "Редактировать клиента"
    └── NavigationIcon: "Назад"
└── Column (Content)
    ├── [NEW] OutlinedButton: "Импорт из контактов" (only if clientId == null)
    │   └── Icon: Icons.Default.Contacts
    │   └── Text: "Импорт из контактов"
    ├── HorizontalDivider (if import button shown)
    ├── OutlinedTextField: Имя *
    ├── OutlinedTextField: Фамилия *
    ├── Text: Пол
    ├── Row: [FilterChip: Муж] [FilterChip: Жен]
    ├── OutlinedTextField: Дата рождения
    ├── PhoneInput: Телефон
    ├── OutlinedTextField: Telegram
    ├── OutlinedTextField: Заметки
    ├── Text: Error message (if any)
    └── Button: Сохранить
```

#### Button States

**"Импорт из контактов" Button:**
- **Visible**: Only when `clientId == null` (creating new client)
- **Hidden**: When `clientId != null` (editing existing client)
- **Enabled**: Always (permission check happens on click)
- **Loading**: Optional spinner while checking permission (if async)

**Visual Design:**
- Style: `OutlinedButton` (secondary action)
- Icon: `Icons.Default.Contacts` (or `Icons.Default.PersonAdd`)
- Text: "Импорт из контактов"
- Position: Top of form, before first input field
- Spacing: 16dp margin below button, 8dp divider

---

### 2.2 Dialog Components

#### Dialog A: No Phone Number
```
AlertDialog
├── Title: "Невозможно импортировать"
├── Text: "У выбранного контакта нет номера телефона. Выберите другой контакт."
└── Button: "OK" (dismisses dialog)
```

#### Dialog B: Multiple Phone Numbers
```
AlertDialog
├── Title: "Выберите номер телефона"
├── Text: "У контакта несколько номеров:"
├── Column (Scrollable)
│   ├── RadioButton: "[Mobile]: +79161234567" (selected)
│   ├── RadioButton: "[Work]: +79169876543"
│   └── RadioButton: "[Home]: +79165555555"
└── Row
    ├── TextButton: "Отмена"
    └── Button: "Выбрать" (uses selected number)
```

**Alternative (Simpler)**: Use `SingleChoiceItems` in AlertDialog

---

### 2.3 Snackbar Messages

1. **Permission Denied:**
   - Text: "Для импорта контакта необходимо разрешение на доступ к контактам"
   - Duration: `SnackbarDuration.Long`
   - Action: None (user must go to Settings)

2. **Import Error (generic):**
   - Text: "Не удалось импортировать контакт. Попробуйте еще раз."
   - Duration: `SnackbarDuration.Short`

---

## 3. Permission Handling Logic

### 3.1 Permission Declaration

**AndroidManifest.xml:**
```xml
<uses-permission android:name="android.permission.READ_CONTACTS" />
```

**Note**: For Android 13+ (API 33+), `READ_CONTACTS` is still required for reading phone numbers.

---

### 3.2 Permission Request Flow

#### High-Level Logic:

```
1. User taps "Импорт из контактов"
   ↓
2. Check: ContextCompat.checkSelfPermission(context, READ_CONTACTS)
   ↓
3. If PERMISSION_GRANTED:
   → Launch Contact Picker
   ↓
4. If PERMISSION_DENIED:
   → Check: shouldShowRequestPermissionRationale()
   ↓
   If true (user denied before):
   → Show Snackbar explaining why permission is needed
   → Request permission: ActivityCompat.requestPermissions()
   ↓
   If false (first time):
   → Request permission: ActivityCompat.requestPermissions()
   ↓
5. Handle permission result:
   ↓
   If GRANTED:
   → Launch Contact Picker
   ↓
   If DENIED:
   → Show Snackbar: "Разрешение необходимо для импорта контактов"
```

#### Implementation Details:

**In ClientEditScreen Composable:**

```kotlin
// Permission launcher
val contactPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (isGranted) {
        launchContactPicker()
    } else {
        showSnackbar("Для импорта контакта необходимо разрешение на доступ к контактам")
    }
}

// Contact picker launcher
val contactPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickContact()
) { uri ->
    uri?.let { handleContactSelection(it) }
}

// Function to check and request permission
fun handleImportClick() {
    when {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED -> {
            // Permission already granted, launch picker
            contactPickerLauncher.launch(null)
        }
        ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.READ_CONTACTS
        ) -> {
            // Show explanation, then request
            showSnackbar("Разрешение необходимо для выбора контакта")
            contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
        else -> {
            // First time, request directly
            contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }
}
```

**Note**: For Android 13+ (API 33+), use `ActivityResultContracts.PickContact()` which doesn't require `READ_CONTACTS` permission. For older versions, use `Intent.ACTION_PICK` with `ContactsContract.Contacts.CONTENT_URI` and `READ_CONTACTS` permission.

---

### 3.3 Contact Data Extraction

#### Extract Contact Info:

```kotlin
fun extractContactData(uri: Uri): ContactData? {
    val contentResolver = context.contentResolver
    
    // Query contact
    val cursor = contentResolver.query(
        uri,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL
        ),
        null, null, null
    )
    
    cursor?.use {
        if (it.moveToFirst()) {
            val displayName = it.getString(0) ?: ""
            val phoneNumbers = mutableListOf<PhoneNumber>()
            
            // Get all phone numbers
            do {
                val number = it.getString(1)
                val type = it.getInt(2)
                val label = it.getString(3)
                
                if (number != null) {
                    phoneNumbers.add(PhoneNumber(number, type, label))
                }
            } while (it.moveToNext())
            
            // Get email (separate query)
            val email = getContactEmail(uri)
            
            // Split name
            val (firstName, lastName) = splitName(displayName)
            
            return ContactData(
                firstName = firstName,
                lastName = lastName,
                phoneNumbers = phoneNumbers,
                email = email
            )
        }
    }
    
    return null
}

data class ContactData(
    val firstName: String,
    val lastName: String,
    val phoneNumbers: List<PhoneNumber>,
    val email: String?
)

data class PhoneNumber(
    val number: String,
    val type: Int,
    val label: String?
)
```

---

## 4. Edge Cases Handling

### 4.1 Contact Without Phone Number
- **Detection**: `phoneNumbers.isEmpty()`
- **Action**: Show AlertDialog "Невозможно импортировать"
- **User Action**: Dismiss dialog, can try another contact

### 4.2 Multiple Phone Numbers
- **Detection**: `phoneNumbers.size > 1`
- **Action**: Show AlertDialog with radio buttons or list
- **User Action**: Select one number, proceed with import

### 4.3 Contact Name Parsing
- **Strategy**: 
  - If name contains space: split on first space
  - First part → `firstName`
  - Remaining → `lastName`
  - If no space: full name → `lastName`, `firstName = ""`

### 4.4 Phone Number Formatting
- **Action**: Format to E.164 if possible
- **Fallback**: Use as-is if formatting fails
- **Validation**: Use existing `validatePhoneNumber()` function

### 4.5 Duplicate Phone Number
- **Detection**: After pre-fill, when user taps "Сохранить"
- **Action**: Show existing error: "Клиент с таким телефоном уже существует"
- **User Action**: Edit phone number or cancel

### 4.6 Permission Permanently Denied
- **Detection**: User denied permission multiple times, "Don't ask again" checked
- **Action**: Show Snackbar with link to Settings (if possible)
- **Alternative**: Show dialog explaining how to grant permission manually

---

## 5. UX Copy (Russian)

### Button Labels
- "Импорт из контактов" - Import from contacts button

### Dialog Titles
- "Невозможно импортировать" - Cannot import
- "Выберите номер телефона" - Choose phone number

### Dialog Messages
- "У выбранного контакта нет номера телефона. Выберите другой контакт." - Selected contact has no phone number. Choose another contact.
- "У контакта несколько номеров:" - Contact has multiple numbers:

### Snackbar Messages
- "Для импорта контакта необходимо разрешение на доступ к контактам" - Contact access permission is required to import contacts
- "Не удалось импортировать контакт. Попробуйте еще раз." - Failed to import contact. Please try again.

### Button Actions
- "OK" - OK
- "Отмена" - Cancel
- "Выбрать" - Select

---

## 6. Implementation Notes

### 6.1 ActivityResultContracts

**For Android 13+ (API 33+):**
- Use `ActivityResultContracts.PickContact()` - no permission needed
- Returns `Uri?` of selected contact

**For Android 12 and below:**
- Use `Intent.ACTION_PICK` with `ContactsContract.Contacts.CONTENT_URI`
- Requires `READ_CONTACTS` permission
- Returns `Uri?` of selected contact

### 6.2 Navigation

**Pre-fill Strategy:**
- Option A: Pass pre-filled data as arguments to `ClientEditScreen`
- Option B: Use ViewModel to set initial state
- Option C: Use `LaunchedEffect` with a flag to set initial values

**Recommended**: Option B or C - use ViewModel or state management to pre-fill fields when screen loads.

### 6.3 State Management

**ClientEditScreen State:**
- Add `var isImporting by remember { mutableStateOf(false) }` for loading state
- Add `var importError by remember { mutableStateOf<String?>(null) }` for error messages
- Pre-fill logic should run in `LaunchedEffect` when import data is available

---

## 7. Testing Scenarios

1. **Happy Path:**
   - Permission granted → Select contact with phone → Pre-fill → Save

2. **Permission Denied:**
   - Tap import → Deny permission → See Snackbar

3. **No Phone Number:**
   - Select contact without phone → See error dialog

4. **Multiple Phones:**
   - Select contact with 3 phones → See selection dialog → Choose one → Pre-fill

5. **Edit Before Save:**
   - Import contact → Edit name → Save → Success

6. **Duplicate Phone:**
   - Import contact → Save → Error if phone exists

7. **Cancel Import:**
   - Tap import → Open picker → Cancel → Return to form (no changes)

---

## Summary

This design provides:
- ✅ Clear entry point (secondary button on Add Client screen)
- ✅ Standard Android permission flow
- ✅ System Contact Picker (not custom UI)
- ✅ Pre-fill with editable fields
- ✅ No auto-save (user confirms)
- ✅ Edge case handling (no phone, multiple phones)
- ✅ Simple, clear UX copy
- ✅ Modern Android practices (ActivityResultContracts)

The implementation follows Jetpack Compose best practices and Android UX guidelines.


