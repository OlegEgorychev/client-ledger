# Client Ledger

## About

This application was created for my mother, who works as a stylist. She needed a tool to structure and organize her work with clients, manage appointments, track income and expenses, and maintain client information in one place.

## Description

Client Ledger is an Android application built with Jetpack Compose and Room database, designed to help stylists and service providers manage their client relationships, appointments, and finances.

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

### Expenses
- Track daily expenses
- View expenses by date

### Statistics
- View income and expense statistics
- Analyze profitability by period

### Splash Screen
- Displays test version notice
- Information about personal use only

## Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Database**: Room Persistence Library
- **Architecture**: MVVM (Model-View-ViewModel)
- **Navigation**: Navigation Compose
- **Coroutines**: Kotlin Coroutines for asynchronous operations
- **Date/Time**: Java Time API

## Project Structure

```
app/src/main/java/com/clientledger/app/
├── data/
│   ├── dao/              # Data Access Objects
│   ├── database/         # Room database setup
│   ├── entity/           # Database entities
│   └── repository/       # Repository layer
├── ui/
│   ├── navigation/       # Navigation setup
│   ├── screen/          # Composable screens
│   │   ├── calendar/   # Calendar and schedule screens
│   │   ├── clients/    # Client management screens
│   │   └── stats/      # Statistics screen
│   ├── theme/          # App theme
│   └── viewmodel/      # ViewModels
└── util/               # Utility functions
```

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
- **Clients**: Personal information, contact details
- **Appointments**: Client ID, title, start time, duration, income, payment status
- **Expenses**: Title, amount, date/time

## Requirements

- Android SDK 26 (Android 8.0) or higher
- Target SDK 34

## Build Instructions

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Build and run on device or emulator

## Notes

- This is a test version of the application
- Not intended for commercial use
- Developed exclusively for personal use
- All rights reserved

## License

This project is for personal use only.
