# Firebase Setup Instructions for Travel App

## Overview
Your travel app now includes Firebase Firestore integration for cloud data synchronization. This allows your app data (places, favorites, search history) to be synced across devices and backed up to the cloud.

## Setup Steps

### 1. Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Create a project" or "Add project"
3. Enter project name: `trave-app` (or your preferred name)
4. Enable Google Analytics (optional)
5. Click "Create project"

### 2. Add Android App to Firebase Project
1. In Firebase Console, click "Add app" and select Android
2. Enter package name: `com.example.trave_app`
3. Enter app nickname: `Travel App`
4. Click "Register app"

### 3. Download Configuration File
1. Download the `google-services.json` file
2. **IMPORTANT**: Replace the placeholder file at `app/google-services.json` with your actual Firebase configuration file
3. The current file is a placeholder and won't work with real Firebase services

### 4. Enable Firestore Database
1. In Firebase Console, go to "Firestore Database"
2. Click "Create database"
3. Choose "Start in test mode" (for development)
4. Select a location close to your users
5. Click "Done"

### 5. Configure Firestore Security Rules (Optional)
For production, update Firestore rules in Firebase Console:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow read/write access to all documents
    match /{document=**} {
      allow read, write: if true;
    }
  }
}
```

## Features Implemented

### Automatic Cloud Sync
- **Places**: Automatically synced when added/updated
- **Favorites**: Synced when added/updated/deleted
- **Search History**: Synced when new searches are performed

### Manual Sync
- Call `travelRepository.performFullSync()` to manually sync all data
- Check sync status with `travelRepository.isSyncing()`

### Data Collections in Firestore
- `places`: All discovered places with location data
- `favorites`: User's favorite places with notes
- `search_history`: Recent search queries and results

## Build Configuration
All necessary dependencies and configurations have been added:
- Firebase BOM (Bill of Materials) for version management
- Firestore and Auth libraries
- Google Services plugin for processing google-services.json

## Usage in Code
The synchronization happens automatically in the background. Your existing code will continue to work exactly as before, but now data will also be synced to Firebase.

### Example: Adding a Place
```java
// This will save locally AND sync to cloud automatically
Place newPlace = new Place(placeId, name, category, lat, lng, address, rating, false, System.currentTimeMillis());
travelRepository.insert(newPlace);
```

### Example: Manual Full Sync
```java
// Perform complete bidirectional sync
travelRepository.performFullSync();
```

## Important Notes
1. **Replace the placeholder google-services.json** with your actual Firebase configuration
2. The app will work offline - cloud sync happens in background
3. All existing functionality is preserved
4. No changes needed to your existing UI code
5. Firebase operations are non-blocking and won't affect app performance

## Troubleshooting
- If build fails, ensure google-services.json is in the correct location: `app/google-services.json`
- Check that your Firebase project has Firestore enabled
- Verify package name matches between app and Firebase project: `com.example.trave_app`
