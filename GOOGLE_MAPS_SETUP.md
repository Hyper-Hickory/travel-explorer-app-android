# Google Maps API Setup Instructions

## To enable the map functionality in your travel app, you need to:

### 1. Get a NEW Google Maps API Key
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the following APIs:
   - Maps SDK for Android
   - Places API
4. Create credentials (API Key)
5. Restrict the API key to Android apps and add your package name: `com.example.trave_app`

### 2. Add Your NEW API Key
Replace `YOUR_NEW_GOOGLE_MAPS_API_KEY_HERE` in the following files with your actual API key:
- `app/src/main/AndroidManifest.xml` (line 23)
- `app/src/main/java/com/example/trave_app/MapActivity.java` (line 81)

### 3. Build and Run
After adding your API key, build and run the app. The map will show your current location and display nearby places based on the selected category.

## Features Added:
- **Full Google Maps integration** with real-time location
- **Category-specific markers** with unique colors:
  - 🔴 Restaurants (Red)
  - 🟠 Cafes (Orange) 
  - 🔵 Hotels (Blue)
  - 🟢 Parks (Green)
  - 🟡 Gas Stations (Yellow)
  - 🟣 Parking (Violet)
  - 🔷 Hostels (Cyan)
  - 🟣 Malls (Magenta)
- **Location permissions** handling
- **Fallback sample markers** if Places API fails
- **Back navigation** from map to options
