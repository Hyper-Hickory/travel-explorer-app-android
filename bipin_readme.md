# Bipin README

This document summarizes the essential architecture, data flow, and algorithms used to integrate Maps, AI Chatbot, ML components, and Notifications/Alerts in the TRAVE_APP project.

## Project Overview
- Package: `com.example.trave_app`
- Key Modules:
  - Maps/Places UI and search (Options -> MapActivity)
  - Chatbots: Cloud-assisted (Gemini) and fully local Travel Assistant
  - ML insights and personalization
  - Notifications and real-time alerts
  - Local database (Room) and curated Navi Mumbai place dataset


# Maps Integration
- Entry points:
  - Main screen quick buttons (Restaurants/Hotels/Cafes/Malls) [TO DO hooks]
  - Options screen cards (e.g., Restaurants, Cafes, Hotels, Hostels, Malls, Parks) → `OptionsActivity` → `navigateToMap(String searchType)`
  - Intent extra: `search_type` passed to `MapActivity`
- Data sources:
  - Curated local dataset for Navi Mumbai via `res/raw/places_vashi.json` (used for chatbot/local listings)
  - Map search: planned integration with Google Maps/Places API (TODO), designed to use `search_type` for nearby search

Algorithm (planned typical Nearby Search):
1. Acquire user location (FusedLocationProvider).
2. Build Places API query for category (e.g., `restaurant`, `cafe`, `lodging`, `shopping_mall`, `park`).
3. Radius filter (e.g., 2km) and rank by prominence/distance.
4. Render markers and list; enable tap-to-details.


# AI Chatbot
There are two chatbot services. The app currently uses the Local Assistant by default from the Options screen.

## 1) Local Travel Assistant (primary)
- UI: `LocalAssistantActivity` (guided onboarding + free Q&A)
- Core logic: `chatbot/service/LocalTravelAssistantService`
- Data: `data/VashiPlacesProvider` reading `res/raw/places_vashi.json`

Behavior:
- Guided onboarding flow (sequenced questions):
  1) "Hello Sir! Great to see here you."
  2) "Are you planning a vacation, business trip, or a family visit?"
  3) "Do you have any preferred destination or should I suggest some options?"
  4) "Do you want to travel domestically or internationally?"
  5) "What is your approximate travel budget?"
  6) "Do you prefer budget-friendly options or luxury travel?"
  7) "Should I filter stays under a certain price range? (e.g., per night cost)"
  - After step 7, the bot switches to normal answering mode.

- Deterministic Navi Mumbai category handler:
  - Triggers on keywords: hotels, hostels, restaurants, cafes, parks, malls; verbs like prefer/list/show/give/suggest/recommend; localities like Vashi/Nerul/Seawoods/Airoli/etc.
  - Loads places with `VashiPlacesProvider.getPlacesByCategory(context, category)`.
  - Sorts by rating desc, secondary by name.
  - Returns up to 5 items per requested category with name, rating, and full address.

- Local FAQ fallback:
  - Loads up to 10,000 FAQs from `assets/travel_faq.json` if provided.
  - Keyword overlap scoring to pick the best answer when request isn’t a category request.

## 2) Gemini AI Service (optional cloud path)
- Class: `chatbot/service/GeminiAIService`
- HTTP client: OkHttp
- Models: `gemini-1.5-*` with sequential fallback routing; requires a valid API Key.
- Local deterministic handler identical in spirit to the Local Assistant: before calling the API, it tries to locally fulfill Navi Mumbai category requests using the same dataset.
- If API used, builds request JSON and parses the Gemini response.

Prompt/response strategy:
- For pure local Navi Mumbai category requests, no cloud is used.
- For generic questions, it can call Gemini and optionally merge ML insights (see ML below).


# ML Components
- Engine: `ml/engine/TravelRecommendationEngine`
- Service: `ml/service/IntelligentSearchService`
- Repository layer: `repository/TravelRepository` and Room DAOs provide places, favorites, and search history.

Algorithms in ML integration (as used by Gemini service and can be extended to Local Assistant):
- Learn from user behavior: `mlEngine.learnFromUserBehavior(places, favorites, searchHistory)`.
- Analyze patterns: `analyzeTravelPatterns()` returns `topPreference`, `mostVisited`, `diversityScore`, `totalVisits`.
- Personalized recommendations:
  - Score places based on user’s past interactions and categories.
  - Select top-N (e.g., 3–5) for inclusion in responses.


# Notifications and Alerts
- Manager: `notifications/service/RealTimeNotificationManager`
- On app start (`MainActivity`), `createNotificationChannels(this)` is called to ensure channels exist.
- Potential use-cases:
  - Real-time alerts for nearby places, price drops (future), trip reminders.

Typical flow:
1. Create channels (importance, sound, vibration as needed).
2. Build NotificationCompat with title, body, tap intent.
3. Issue with NotificationManager.


# Local Database (Room)
- Entities: `database/entity/Place`, `Favorite`, `SearchHistory`
- DAOs: `PlaceDao`, `FavoriteDao`, `SearchHistoryDao`
- DB: `TravelDatabase`
- Repository: `TravelRepository` exposes LiveData and async operations.

`Place` fields:
- `placeId`, `name`, `category`, `latitude`, `longitude`, `address`, `rating`, `is_favorite`, `created_at`.

`PlaceDao` key queries:
- `getAllPlaces()`, `getPlacesByCategory(category)`, `getFavoritePlaces()`, `searchPlacesByName(q)`
- update favorite flag, insert/update/delete bulk ops.


# Navi Mumbai Dataset
- File: `app/src/main/res/raw/places_vashi.json`
- Categories guaranteed to have ≥5 items: hotels, hostels, restaurants, cafes, parks, malls.
- Loader: `data/VashiPlacesProvider` reads JSON into `Place` objects with generated `placeId`.

JSON structure (example):
```json
{
  "name": "Inorbit Mall Vashi",
  "category": "malls",
  "latitude": 19.0654,
  "longitude": 72.9967,
  "address": "Sector 30A, Vashi, Navi Mumbai, Maharashtra 400703",
  "rating": 4.5
}
```


# UI Entry Points
- Main screen (`activity_main.xml`):
  - Search Nearby button → `OptionsActivity`.
  - Quick actions (Restaurants/Hotels/Cafes/Malls).
  - Travel Assistant button removed here by design (kept in Options screen).
- Options screen (`activity_options.xml`):
  - `cardChatbot` (Travel Assistant) opens `LocalAssistantActivity`.
  - `cardMLInsights` opens ML insights screen.


# Build & Run
1. Android Studio Arctic+ / Gradle per project.
2. Build variants: Debug/Release.
3. If using Gemini API (optional), set a valid API key in `GeminiAIService`.
4. Run on device/emulator with location enabled for Maps features.


# Extending & Customizing
- To add more Navi Mumbai places, append to `places_vashi.json`.
- To adjust suggestion limits, change caps in `LocalTravelAssistantService` (currently 5 per category).
- To route Travel Assistant from main screen, add a button and start `LocalAssistantActivity`.
- To integrate real Places API searches in MapActivity, implement nearby search using Google Places SDK.


# Key Classes & Files (quick index)
- Chat UI: `LocalAssistantActivity`, `ChatbotActivity`, `chatbot/adapter/ChatAdapter`, `chatbot/model/ChatMessage`
- Local Assistant: `chatbot/service/LocalTravelAssistantService`
- Gemini Cloud: `chatbot/service/GeminiAIService`
- Dataset Provider: `data/VashiPlacesProvider` (reads `res/raw/places_vashi.json`)
- ML: `ml/engine/TravelRecommendationEngine`, `ml/service/IntelligentSearchService`
- DB: `database/TravelDatabase`, `database/dao/*`, `database/entity/*`
- Repository & VM: `repository/TravelRepository`, `viewmodel/TravelViewModel`
- Notifications: `notifications/service/RealTimeNotificationManager`


# Security Notes
- Do not commit real API keys.
- The local assistant path works offline; cloud path requires network and key.


# License
Internal project documentation for TRAVE_APP. All rights reserved.
