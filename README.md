# WalletLens - Personal Finance Tracker

WalletLens is a comprehensive Android application for personal finance management, built with modern Android development practices and MVVM architecture.

## Features

### ✅ Core Features (All Requirements Met)

1. **Notifications** - Payment reminders and budget warnings using WorkManager
2. **Database** - SQLite database using Room for local storage of transactions, budgets, and reminders
3. **App Widget** - Home screen widget showing today's spending and quick access
4. **Animations** - Smooth animations for transaction lists and UI interactions
5. **Multiple Screens** - Dashboard and Widget functionality

### 🎯 Target Audience

- Students managing limited monthly budgets
- Working professionals tracking recurring expenses
- Families looking to monitor household spending
- Freelancers managing irregular income

### 🏗️ Architecture

- **MVVM (Model-View-ViewModel)** - Clean architecture pattern
- **Room Database** - Local SQLite storage with LiveData
- **Repository Pattern** - Data abstraction layer
- **WorkManager** - Background tasks for notifications
- **Material Design** - Modern UI components

### 📱 Screens

1. **Main Dashboard** - Overview of income, expenses, balance, and charts
2. **Widget** - Home screen widget for quick access

### 🔧 Technical Implementation

#### Database Schema

- **Transactions** - Store income and expense data
- **Budgets** - Budget categories and limits
- **Reminders** - Payment reminders and notifications

#### Key Components

- `MainActivity` - Main dashboard with charts and transaction list
- `WalletLensWidget` - Home screen widget
- `NotificationWorker` - Background notification processing
- `TransactionAdapter` - RecyclerView adapter with animations

#### Dependencies

- **Room** - Database ORM
- **WorkManager** - Background task scheduling
- **MPAndroidChart** - Charts and visualizations
- **Material Design** - UI components
- **ViewBinding** - View binding for type safety

### 🚀 Getting Started

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Build and run on device/emulator

### 📋 Permissions Required

- Notifications - For payment reminders

### 🎨 UI Features

- **Animated Charts** - Pie charts for spending categories
- **Material Cards** - Modern card-based design
- **Floating Action Buttons** - Quick access to add transactions
- **Color-coded Transactions** - Green for income, red for expenses
- **Responsive Design** - Works on different screen sizes

### 🔔 Notification System

- Payment reminders for upcoming bills
- Budget overspending warnings
- Scheduled notifications using WorkManager
- Custom notification channel for Android 8+

### 📊 Data Visualization

- Pie charts for spending by category
- Real-time balance calculations
- Monthly income/expense summaries
- Transaction history with animations

### 🛠️ Development Notes

- Built with Kotlin
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 35 (Android 15)
- Uses modern Android development practices
- Follows Material Design guidelines

### 📈 Future Enhancements

- Cloud backup and sync
- Multiple currency support
- Export functionality
- Advanced analytics and insights
- Budget goal tracking
- Recurring transaction automation

---

**WalletLens** - Your personal finance companion for smarter spending and better financial awareness.
# WalletLens-Android
