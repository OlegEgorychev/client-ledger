# Client Ledger

## About

This application was created for my mother, who works as a stylist. She needed a tool to structure and organize her work with clients, manage appointments, track income and expenses, and maintain client information in one place.

This project is being developed with the assistance of **AI Cursor** and **ChatGPT** to accelerate development and implement best practices.

## Description

Client Ledger is an Android application built with Jetpack Compose and Room database, designed to help stylists and service providers manage their client relationships, appointments, and finances with automatic backup to Google Drive.

## Features

### Client Management
- Add, edit, and view client information
- Client search functionality (by first name or last name)
- Client statistics: number of visits and total amount paid
- Client details screen with personal information

### Calendar & Scheduling
- Monthly calendar view
- Day schedule view with timeline (similar to Google Calendar)
  - Vertical timeline from 00:00 to 23:00
  - Visual representation of appointments with start and end times
  - Appointment cards positioned accurately on the timeline
- Click on any day to view the schedule

### Appointments
- Create and edit appointments
- Set appointment start and end times
- Track income per appointment
- Mark appointments as paid/unpaid
- Quick client selection with autocomplete search
- Create new clients on the fly when adding appointments
- Multiple services per appointment with tags

### Expenses
- Track daily expenses with multiple items
- View expenses by date
- Expense categorization with tags

### Statistics
- View income and expense statistics
- Analyze profitability by period
- Client income analysis
- Monthly and daily statistics
- Income/expense charts

### Backup & Restore
- **Automatic backups** after every data change (clients, appointments, expenses)
- Automatic upload to **Google Drive** (when signed in)
- Manual export backup to file
- Restore from backup file
- Backup history with numbered and dated files
- Backup files stored in "ClientLedger Backups" folder on Google Drive

### Settings
- Dark/Light theme toggle
- Daily reminder settings (hour and minute)
- Backup management (export, restore, Google Drive integration)
- Google Drive sign-in/out

### Reminders
- Daily reminder notifications
- Configurable reminder time
- SMS reminder functionality (optional)

## Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Database**: Room Persistence Library (schema version 8)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Navigation**: Navigation Compose
- **Coroutines**: Kotlin Coroutines for asynchronous operations
- **Date/Time**: Java Time API
- **Backup**: JSON serialization with Gson
- **Cloud Storage**: Google Drive API integration
- **Preferences**: DataStore Preferences
- **Charts**: Vico Charts library

## Project Structure

```
app/src/main/java/com/clientledger/app/
├── data/
│   ├── backup/            # Backup system and Google Drive integration
│   ├── dao/               # Data Access Objects
│   ├── database/          # Room database setup
│   ├── entity/            # Database entities
│   ├── preferences/       # App preferences (DataStore)
│   └── repository/        # Repository layer
├── ui/
│   ├── navigation/        # Navigation setup
│   ├── screen/           # Composable screens
│   │   ├── calendar/    # Calendar and schedule screens
│   │   ├── clients/     # Client management screens
│   │   ├── stats/       # Statistics screen
│   │   └── settings/    # Settings screen
│   ├── theme/           # App theme
│   └── viewmodel/       # ViewModels
└── util/                # Utility functions
```

## Backup System

The application includes a comprehensive backup system:

### Automatic Backups
- Triggers automatically after creating/editing:
  - Clients
  - Appointments
  - Expenses
- Debounced to prevent excessive backups (1-3 seconds delay)
- Backup includes all database entities with schema version

### Google Drive Integration
- Automatic upload to Google Drive when signed in
- Creates "ClientLedger Backups" folder automatically
- Files named with backup number and timestamp: `backup_001_2024-01-15_14-30.json`
- Backup counter increments automatically

### Manual Backup/Restore
- Export latest backup (share file)
- Restore from backup file (file picker)
- Confirmation dialog before restore
- Schema version validation

### Backup File Format
JSON format containing:
- Metadata (createdAt, appVersion, schemaVersion)
- All entities (clients, appointments, expenses, tags, etc.)

## Google Drive Setup

To enable Google Drive backups:

1. Create a project in [Google Cloud Console](https://console.cloud.google.com/)
2. Enable Google Drive API
3. Configure OAuth Consent Screen
4. Create OAuth Client ID (Android type) with:
   - Package name: `com.clientledger.app`
   - SHA-1 certificate fingerprint (debug keystore)
5. Add test users in OAuth Consent Screen (if in testing mode)

See `GOOGLE_DRIVE_SETUP.md` for detailed instructions.

## Key Features Implementation

### Day Schedule View
- Vertical timeline with hourly markers (00:00 - 23:00)
- Appointment cards positioned by start time
- Card height corresponds to appointment duration
- Accurate time alignment with timeline markers

### Client Search
- Prefix-based search (case-insensitive)
- Searches by first name or last name
- Shows all clients alphabetically when field is empty
- Real-time filtering as you type

### Database Schema
- **Clients**: Personal information, contact details, gender, birth date
- **Appointments**: Client ID, date, start/end time, income, payment status, notes
- **AppointmentServices**: Multiple services per appointment with tags
- **Expenses**: Title, date, total amount
- **ExpenseItems**: Multiple items per expense with tags and amounts
- **ServiceTags**: Tags for categorizing services and expenses

## Requirements

- Android SDK 26 (Android 8.0) or higher
- Target SDK 34
- Google Play Services (for Google Drive integration)
- Internet connection (for Google Drive sync)

## Dependencies

- Jetpack Compose BOM 2024.02.01
- Room 2.6.1
- Navigation Compose 2.7.7
- DataStore Preferences 1.0.0
- Google Play Services Auth 21.0.0
- Google Drive API (REST)
- Gson 2.10.1
- Vico Charts 1.13.1

## Build Instructions

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files (will download Gradle 8.5)
4. Build and run on device or emulator
5. For Google Drive integration, configure OAuth Client ID in Google Cloud Console

## Version

- **Current Version**: 0.1.18
- **Version Code**: 19

## Notes

- This is a test version of the application
- Not intended for commercial use
- Developed exclusively for personal use
- Google Drive integration requires Google account and proper OAuth setup
- All rights reserved

## License

This project is for personal use only.
