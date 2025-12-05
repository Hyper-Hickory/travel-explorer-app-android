package com.example.trave_app.chatbot.service;

import android.content.Context;
import android.util.Log;
import com.example.trave_app.database.entity.Place;
import com.example.trave_app.database.entity.Favorite;
import com.example.trave_app.database.entity.SearchHistory;
import com.example.trave_app.ml.engine.TravelRecommendationEngine;
import com.example.trave_app.ml.service.IntelligentSearchService;
import com.example.trave_app.data.VashiPlacesProvider;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;

public class GeminiAIService {
    private static final String TAG = "GeminiAIService";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String PRIMARY_MODEL = "gemini-1.5-flash-latest"; // use latest stable alias
    private static final String FALLBACK_MODEL = "gemini-pro";  // broad fallback that is widely available
    
    // Gemini API key (provided by user)
    // Get your API key from: https://makersuite.google.com/app/apikey
    private static final String API_KEY = "YOUR_GEMINI_API_KEY_HERE";
    
    private final OkHttpClient client;
    private final Gson gson;
    private final Context context;
    private final TravelRecommendationEngine mlEngine;
    private final IntelligentSearchService searchService;

    public GeminiAIService(Context context) {
        this.context = context;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .writeTimeout(45, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        this.gson = new Gson();
        this.mlEngine = TravelRecommendationEngine.getInstance(context);
        this.searchService = IntelligentSearchService.getInstance(context);
    }

    // Try to list models and pick an available one
    private String discoverAvailableModel() {
        String url = BASE_URL + "/v1beta/models";
        Request req = new Request.Builder()
                .url(url + "?key=" + API_KEY)
                .get()
                .addHeader("x-goog-api-key", API_KEY)
                .build();
        try (Response resp = client.newCall(req).execute()) {
            if (resp.isSuccessful() && resp.body() != null) {
                String body = resp.body().string();
                JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
                if (obj.has("models")) {
                    // Prefer 1.5 flash/pro, then pro
                    String[] preferred = new String[]{
                            "gemini-1.5-flash", "gemini-1.5-flash-latest",
                            "gemini-1.5-pro", "gemini-1.5-pro-latest",
                            "gemini-pro"
                    };
                    String modelsStr = obj.get("models").toString();
                    for (String m : preferred) {
                        if (modelsStr.contains(m)) return m;
                    }
                }
            }
        } catch (Exception ignored) { }
        return null;
    }

    public interface AIResponseCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public void generateResponse(String userMessage, List<Place> places, 
                               List<Favorite> favorites, List<SearchHistory> searchHistory,
                               AIResponseCallback callback) {
        
        // Normalize message
        String lowerMsg = userMessage == null ? "" : userMessage.toLowerCase();

        // Deterministic local handler: Navi Mumbai category requests
        try {
            if (isNaviMumbaiPlaceRequest(lowerMsg)) {
                String localResponse = buildNaviMumbaiResponse(lowerMsg);
                if (localResponse != null && !localResponse.isEmpty()) {
                    callback.onSuccess(localResponse);
                    return;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Local Navi Mumbai handler error", e);
        }

        // Use raw user message for Gemini prompt (Gemini-only behavior)
        String mlEnhancedPrompt = userMessage;
        
        if (API_KEY.equals("your_gemini_api_key_here")) {
            callback.onError("Gemini API key is not set");
            return;
        }

        JsonObject requestBody = createRequestBody(mlEnhancedPrompt);
        
        RequestBody body = RequestBody.create(
            requestBody.toString(),
            MediaType.parse("application/json")
        );

        // Try sequence: include pro variants as well
        String[] paths = new String[] {
                "/v1beta/models/" + PRIMARY_MODEL + ":generateContent",
                "/v1beta/models/gemini-1.5-pro-latest:generateContent",
                "/v1beta/models/gemini-1.5-flash:generateContent",
                "/v1beta/models/gemini-1.5-pro:generateContent",
                "/v1beta/models/" + FALLBACK_MODEL + ":generateContent",
                "/v1/models/" + PRIMARY_MODEL + ":generateContent",
                "/v1/models/gemini-1.5-pro-latest:generateContent",
                "/v1/models/gemini-1.5-flash:generateContent",
                "/v1/models/gemini-1.5-pro:generateContent",
                "/v1/models/" + FALLBACK_MODEL + ":generateContent",
                // Retry same set but with query key in URL in case header is filtered
                "/v1beta/models/" + PRIMARY_MODEL + ":generateContent?key=" + API_KEY,
                "/v1beta/models/gemini-1.5-pro-latest:generateContent?key=" + API_KEY,
                "/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY,
                "/v1beta/models/gemini-1.5-pro:generateContent?key=" + API_KEY,
                "/v1beta/models/" + FALLBACK_MODEL + ":generateContent?key=" + API_KEY,
                "/v1/models/" + PRIMARY_MODEL + ":generateContent?key=" + API_KEY,
                "/v1/models/gemini-1.5-pro-latest:generateContent?key=" + API_KEY,
                "/v1/models/gemini-1.5-flash:generateContent?key=" + API_KEY,
                "/v1/models/gemini-1.5-pro:generateContent?key=" + API_KEY,
                "/v1/models/" + FALLBACK_MODEL + ":generateContent?key=" + API_KEY
        };

        performSequentialCalls(body, paths, 0, callback);
    }

    // Detect if the user asks for Navi Mumbai places or generic place recommendations
    private boolean isNaviMumbaiPlaceRequest(String lowerMsg) {
        if (lowerMsg == null) return false;

        boolean mentionsLocation = lowerMsg.contains("navi mumbai") || lowerMsg.contains("navimumbai")
                || lowerMsg.contains("vashi") || lowerMsg.contains("seawoods")
                || lowerMsg.contains("nerul") || lowerMsg.contains("juinagar")
                || lowerMsg.contains("turbhe") || lowerMsg.contains("kopar khairane")
                || lowerMsg.contains("ghansoli") || lowerMsg.contains("airoli");

        boolean mentionsCategory = lowerMsg.contains("hotel") || lowerMsg.contains("hostel")
                || lowerMsg.contains("restaurant") || lowerMsg.contains("cafe")
                || lowerMsg.contains("park") || lowerMsg.contains("mall")
                || lowerMsg.contains("eat") || lowerMsg.contains("food")
                || lowerMsg.contains("place to stay") || lowerMsg.contains("accommodation")
                || lowerMsg.contains("recommend") || lowerMsg.contains("suggest")
                || lowerMsg.contains("prefer") || lowerMsg.contains("list")
                || lowerMsg.contains("show") || lowerMsg.contains("give");

        // Requirement: Prefer Navi Mumbai in normal chatbot. If user asks generic categories, still serve Navi Mumbai.
        return mentionsCategory || mentionsLocation;
    }

    // Build a formatted response listing top Navi Mumbai places per requested category
    private String buildNaviMumbaiResponse(String lowerMsg) {
        // Categories supported by dataset
        String[] allCategories = new String[]{"hotels", "hostels", "restaurants", "cafes", "parks", "malls"};

        // Determine requested categories
        List<String> requested = new ArrayList<>();
        for (String c : allCategories) {
            if (lowerMsg.contains(c) || (c.equals("cafes") && lowerMsg.contains("cafe"))
                    || (c.equals("hotels") && lowerMsg.contains("hotel"))
                    || (c.equals("restaurants") && lowerMsg.contains("restaurant"))
                    || (c.equals("parks") && lowerMsg.contains("park"))
                    || (c.equals("malls") && lowerMsg.contains("mall"))
                    || (c.equals("hostels") && lowerMsg.contains("hostel"))) {
                requested.add(c);
            }
        }
        if (requested.isEmpty()) {
            // If no specific category, provide all by default as per requirement
            Collections.addAll(requested, allCategories);
        }

        // Emoji/title map
        Map<String, String> titleMap = new LinkedHashMap<>();
        titleMap.put("hotels", "🏨 Hotels");
        titleMap.put("hostels", "🛏️ Hostels");
        titleMap.put("restaurants", "🍽️ Restaurants");
        titleMap.put("cafes", "☕ Cafes");
        titleMap.put("parks", "🌳 Parks");
        titleMap.put("malls", "🛍️ Malls");

        StringBuilder sb = new StringBuilder();
        sb.append("📍 Navi Mumbai Recommendations\n\n");
        sb.append("Here are top places I can suggest in Navi Mumbai (Vashi/nearby):\n\n");

        int categoriesIncluded = 0;
        for (String cat : requested) {
            List<Place> list;
            try {
                list = VashiPlacesProvider.getPlacesByCategory(context, cat);
            } catch (Exception e) {
                list = new ArrayList<>();
            }
            if (list == null || list.isEmpty()) continue; // skip empty categories (e.g., hostels if none)

            // Sort by rating desc, then name
            list.sort(new Comparator<Place>() {
                @Override
                public int compare(Place a, Place b) {
                    int r = Float.compare(b.getRating(), a.getRating());
                    if (r != 0) return r;
                    String na = a.getName() == null ? "" : a.getName();
                    String nb = b.getName() == null ? "" : b.getName();
                    return na.compareToIgnoreCase(nb);
                }
            });

            sb.append("**").append(titleMap.get(cat)).append(" (").append(list.size()).append(")**\n");
            int limit = list.size();
            for (int i = 0; i < limit; i++) {
                Place p = list.get(i);
                sb.append(i + 1).append(". ")
                  .append(p.getName() == null ? "(Unnamed)" : p.getName())
                  .append(" — ⭐ ").append(p.getRating())
                  .append("\n");
                if (p.getAddress() != null && !p.getAddress().isEmpty()) {
                    sb.append("   ").append(p.getAddress()).append("\n");
                }
            }
            sb.append("\n");
            categoriesIncluded++;
        }

        if (categoriesIncluded == 0) {
            // Fallback to all places if category filtering produced nothing
            List<Place> all = VashiPlacesProvider.getAllPlaces(context);
            if (all == null || all.isEmpty()) return null;
            all.sort(new Comparator<Place>() {
                @Override
                public int compare(Place a, Place b) {
                    return Float.compare(b.getRating(), a.getRating());
                }
            });
            sb.append("Top places in Navi Mumbai:\n\n");
            int limit = Math.min(10, all.size());
            for (int i = 0; i < limit; i++) {
                Place p = all.get(i);
                sb.append(i + 1).append(". ")
                  .append(p.getName() == null ? "(Unnamed)" : p.getName())
                  .append(" ("
                  ).append(p.getCategory() == null ? "place" : p.getCategory())
                  .append(") — ⭐ ").append(p.getRating()).append("\n");
                if (p.getAddress() != null && !p.getAddress().isEmpty()) {
                    sb.append("   ").append(p.getAddress()).append("\n");
                }
            }
        }

        sb.append("➡️ Ask for a specific category or area (e.g., Vashi, Nerul, Seawoods) for more focused suggestions.");
        return sb.toString();
    }

    private void performSequentialCalls(RequestBody body, String[] paths, int index, AIResponseCallback callback) {
        if (index >= paths.length) {
            // As last resort, try to discover an available model and call it once
            String discovered = discoverAvailableModel();
            if (discovered != null) {
                String path = "/v1beta/models/" + discovered + ":generateContent";
                performSequentialCalls(body, new String[]{path, path + "?key=" + API_KEY}, 0, callback);
                return;
            }
            callback.onError("Service temporarily unavailable");
            return;
        }

        String url = BASE_URL + paths[index];
        Request req = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("x-goog-api-key", API_KEY)
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API call failed", e);
                // Try next endpoint on network failure
                performSequentialCalls(body, paths, index + 1, callback);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    String aiResponse = parseGeminiResponse(responseBody);
                    if (aiResponse != null && !aiResponse.isEmpty()) {
                        callback.onSuccess(aiResponse);
                    } else {
                        callback.onError("Empty response from Gemini");
                    }
                } else {
                    String errBody = null;
                    try { if (response.body() != null) errBody = response.body().string(); } catch (Exception ignore) {}
                    String conciseError = extractApiError(errBody);
                    Log.e(TAG, "API response not successful: " + response.code() + " body=" + errBody);
                    // Try next endpoint on 4xx/5xx
                    if (index + 1 < paths.length) {
                        performSequentialCalls(body, paths, index + 1, callback);
                    } else {
                        callback.onError(conciseError != null ? conciseError : "Service temporarily unavailable");
                    }
                }
                response.close();
            }
        });
    }

    private String extractApiError(String errBody) {
        if (errBody == null || errBody.isEmpty()) return null;
        try {
            JsonObject obj = JsonParser.parseString(errBody).getAsJsonObject();
            if (obj.has("error")) {
                JsonObject e = obj.getAsJsonObject("error");
                if (e.has("message")) return e.get("message").getAsString();
            }
        } catch (Exception ignored) { }
        return null;
    }

    private String createMLEnhancedPrompt(String userMessage, List<Place> places, 
                                        List<Favorite> favorites, List<SearchHistory> searchHistory) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a smart travel assistant with access to the user's travel data and ML insights. ");
        
        // Add ML-generated insights to the prompt
        try {
            mlEngine.learnFromUserBehavior(places, favorites, searchHistory);
            Map<String, Object> insights = mlEngine.analyzeTravelPatterns();
            
            prompt.append("User's travel patterns: ");
            prompt.append("Top preference: ").append(insights.get("topPreference")).append(", ");
            prompt.append("Most visited: ").append(insights.get("mostVisited")).append(", ");
            prompt.append("Travel diversity: ").append(insights.get("diversityScore")).append("%. ");
            
            // Add personalized recommendations context
            List<Place> recommendations = mlEngine.getPersonalizedRecommendations(places, 3);
            if (!recommendations.isEmpty()) {
                prompt.append("Top ML recommendations for user: ");
                for (int i = 0; i < Math.min(3, recommendations.size()); i++) {
                    Place rec = recommendations.get(i);
                    prompt.append(rec.getName()).append(" (").append(rec.getCategory()).append(")");
                    if (i < recommendations.size() - 1) prompt.append(", ");
                }
                prompt.append(". ");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating ML insights for prompt", e);
        }
        
        // Add user data context
        if (!places.isEmpty()) {
            prompt.append("User has visited ").append(places.size()).append(" places. ");
        }
        if (!favorites.isEmpty()) {
            prompt.append("User has ").append(favorites.size()).append(" favorite places. ");
        }
        
        prompt.append("User message: ").append(userMessage);
        prompt.append(" Please provide a helpful, personalized response based on their travel patterns and preferences.");
        
        return prompt.toString();
    }

    private String enhanceResponseWithMLInsights(String aiResponse, String userMessage, 
                                               List<Place> places, List<Favorite> favorites) {
        // Check if user is asking for recommendations
        String lowerMessage = userMessage.toLowerCase();
        if (lowerMessage.contains("recommend") || lowerMessage.contains("suggest") || 
            lowerMessage.contains("where should") || lowerMessage.contains("what to visit")) {
            
            try {
                List<Place> mlRecommendations = mlEngine.getPersonalizedRecommendations(places, 3);
                if (!mlRecommendations.isEmpty()) {
                    StringBuilder enhanced = new StringBuilder(aiResponse);
                    enhanced.append("\n\n🤖 **ML-Powered Recommendations:**\n");
                    
                    for (int i = 0; i < mlRecommendations.size(); i++) {
                        Place rec = mlRecommendations.get(i);
                        enhanced.append((i + 1)).append(". **").append(rec.getName()).append("** (")
                               .append(rec.getCategory()).append(") - Rating: ").append(rec.getRating()).append("⭐\n");
                    }
                    enhanced.append("\n*These recommendations are personalized based on your travel patterns!*");
                    return enhanced.toString();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error enhancing response with ML recommendations", e);
            }
        }
        
        return aiResponse;
    }

    private String generateMLEnhancedFallbackResponse(String userMessage, List<Place> places, 
                                                    List<Favorite> favorites, List<SearchHistory> searchHistory) {
        String lowerMessage = userMessage.toLowerCase();
        
        try {
            // Learn from user behavior for ML insights
            mlEngine.learnFromUserBehavior(places, favorites, searchHistory);
            Map<String, Object> insights = mlEngine.analyzeTravelPatterns();
            
            // Generate contextual responses based on ML insights
            if (lowerMessage.contains("recommend") || lowerMessage.contains("suggest")) {
                return generateMLRecommendationResponse(insights, places);
            } else if (lowerMessage.contains("pattern") || lowerMessage.contains("insight") || lowerMessage.contains("analysis")) {
                return generateMLInsightsResponse(insights);
            } else if (lowerMessage.contains("search") || lowerMessage.contains("find")) {
                return generateMLSearchResponse(userMessage, places, searchHistory);
            } else if (lowerMessage.contains("favorite") || lowerMessage.contains("like")) {
                return generateMLFavoriteResponse(favorites, insights);
            } else if (lowerMessage.contains("hello") || lowerMessage.contains("hi") || lowerMessage.contains("hey")) {
                return generateMLGreetingResponse(insights);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating ML-enhanced fallback", e);
        }
        
        // Default fallback responses
        String[] defaultResponses = {
            "I'm here to help you discover amazing places! Based on your travel history, I can provide personalized recommendations.",
            "Let me assist you with travel suggestions tailored to your preferences and past experiences!",
            "I can help you find great places to visit based on your travel patterns and favorites.",
            "Ask me about recommendations, travel insights, or help finding specific types of places!"
        };
        
        return defaultResponses[new Random().nextInt(defaultResponses.length)];
    }

    private String generateMLRecommendationResponse(Map<String, Object> insights, List<Place> places) {
        StringBuilder response = new StringBuilder();
        response.append("🎯 **Personalized Recommendations Based on Your Travel Patterns:**\n\n");
        
        try {
            List<Place> recommendations = mlEngine.getPersonalizedRecommendations(places, 5);
            
            if (!recommendations.isEmpty()) {
                response.append("Here are my top ML-powered suggestions for you:\n\n");
                for (int i = 0; i < recommendations.size(); i++) {
                    Place rec = recommendations.get(i);
                    response.append((i + 1)).append(". **").append(rec.getName()).append("**\n");
                    response.append("   📍 Category: ").append(rec.getCategory()).append("\n");
                    response.append("   ⭐ Rating: ").append(rec.getRating()).append("\n");
                    if (rec.getAddress() != null && !rec.getAddress().isEmpty()) {
                        response.append("   📍 Location: ").append(rec.getAddress()).append("\n");
                    }
                    response.append("\n");
                }
                
                response.append("💡 *These recommendations are based on your preference for ")
                       .append(insights.get("topPreference"))
                       .append(" and your travel diversity score of ")
                       .append(insights.get("diversityScore")).append("%.*");
            } else {
                response.append("I need more data about your travel preferences to provide better recommendations. ");
                response.append("Try visiting some places and adding favorites to help me learn your preferences!");
            }
            
        } catch (Exception e) {
            response.append("I'm still learning your preferences. Visit more places and add favorites to get better recommendations!");
        }
        
        return response.toString();
    }

    private String generateMLInsightsResponse(Map<String, Object> insights) {
        StringBuilder response = new StringBuilder();
        response.append("🧠 **Your Travel Pattern Analysis:**\n\n");
        
        response.append("📊 **Key Insights:**\n");
        response.append("• Top Preference: ").append(insights.get("topPreference")).append("\n");
        response.append("• Most Visited Category: ").append(insights.get("mostVisited")).append("\n");
        response.append("• Travel Diversity Score: ").append(insights.get("diversityScore")).append("%\n");
        response.append("• Total Visits Tracked: ").append(insights.get("totalVisits")).append("\n\n");
        
        double diversityScore = (Double) insights.get("diversityScore");
        if (diversityScore > 70) {
            response.append("🌟 You're an adventurous traveler with diverse interests! You explore many different types of places.\n\n");
        } else if (diversityScore > 40) {
            response.append("🎯 You have focused travel preferences but still enjoy variety in your destinations.\n\n");
        } else {
            response.append("🔍 You have specific travel preferences. Consider exploring new categories to diversify your experiences!\n\n");
        }
        
        response.append("💡 *These insights help me provide better personalized recommendations for you.*");
        
        return response.toString();
    }

    private String generateMLSearchResponse(String userMessage, List<Place> places, List<SearchHistory> searchHistory) {
        StringBuilder response = new StringBuilder();
        response.append("🔍 **Smart Search Results:**\n\n");
        
        try {
            // Extract search query from user message
            String query = userMessage.toLowerCase()
                    .replace("search", "").replace("find", "")
                    .replace("for", "").replace("me", "")
                    .trim();
            
            if (!query.isEmpty()) {
                List<Place> searchResults = searchService.performIntelligentSearch(query, places, 5);
                
                if (!searchResults.isEmpty()) {
                    response.append("Found ").append(searchResults.size()).append(" results for \"").append(query).append("\":\n\n");
                    
                    for (int i = 0; i < searchResults.size(); i++) {
                        Place place = searchResults.get(i);
                        response.append((i + 1)).append(". **").append(place.getName()).append("**\n");
                        response.append("   📍 ").append(place.getCategory()).append(" • ⭐ ").append(place.getRating()).append("\n");
                        if (place.getAddress() != null) {
                            response.append("   📍 ").append(place.getAddress()).append("\n");
                        }
                        response.append("\n");
                    }
                    
                    response.append("🤖 *Results ranked using ML based on your preferences and search patterns.*");
                } else {
                    response.append("No results found for \"").append(query).append("\". ");
                    response.append("Try different keywords or explore new places to expand your search options!");
                }
            } else {
                response.append("What would you like to search for? I can help you find places based on your preferences and past visits!");
            }
            
        } catch (Exception e) {
            response.append("I can help you search for places! Just tell me what type of place you're looking for.");
        }
        
        return response.toString();
    }

    private String generateMLFavoriteResponse(List<Favorite> favorites, Map<String, Object> insights) {
        StringBuilder response = new StringBuilder();
        response.append("❤️ **Your Favorite Places Analysis:**\n\n");
        
        if (!favorites.isEmpty()) {
            response.append("You have ").append(favorites.size()).append(" favorite places! Here are some insights:\n\n");
            
            // Show recent favorites
            response.append("📍 **Recent Favorites:**\n");
            int count = Math.min(3, favorites.size());
            for (int i = 0; i < count; i++) {
                Favorite fav = favorites.get(i);
                response.append("• ").append(fav.getName());
                if (fav.getNotes() != null && !fav.getNotes().isEmpty()) {
                    response.append(" - ").append(fav.getNotes());
                }
                response.append("\n");
            }
            
            response.append("\n🎯 Based on your favorites, your top preference is ")
                   .append(insights.get("topPreference"))
                   .append(". I can recommend similar places you might love!");
            
        } else {
            response.append("You haven't added any favorites yet! ");
            response.append("Start adding places to your favorites so I can learn your preferences and provide better recommendations.");
        }
        
        return response.toString();
    }

    private String generateMLGreetingResponse(Map<String, Object> insights) {
        StringBuilder response = new StringBuilder();
        
        String[] greetings = {
            "Hello! I'm your AI travel assistant powered by machine learning. ",
            "Hi there! Ready to explore with personalized recommendations? ",
            "Hey! I'm here to help you discover amazing places based on your preferences. "
        };
        
        response.append(greetings[new Random().nextInt(greetings.length)]);
        
        if (insights.get("totalVisits") != null && (Integer) insights.get("totalVisits") > 0) {
            response.append("I've been learning from your ").append(insights.get("totalVisits"))
                   .append(" visits and can see you prefer ").append(insights.get("topPreference"))
                   .append(". How can I help you today?");
        } else {
            response.append("I'm ready to learn your travel preferences and provide personalized recommendations. What can I help you with?");
        }
        
        return response.toString();
    }

    private JsonObject createRequestBody(String prompt) {
        // { "contents": [ { "role": "user", "parts": [ { "text": "..." } ] } ] }
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);

        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        content.add("parts", gson.toJsonTree(new JsonObject[]{part}));

        JsonObject requestBody = new JsonObject();
        requestBody.add("contents", gson.toJsonTree(new JsonObject[]{content}));
        return requestBody;
    }

    private String parseGeminiResponse(String responseBody) {
        try {
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            
            if (jsonResponse.has("candidates") && jsonResponse.getAsJsonArray("candidates").size() > 0) {
                JsonObject candidate = jsonResponse.getAsJsonArray("candidates").get(0).getAsJsonObject();
                
                if (candidate.has("content")) {
                    JsonObject content = candidate.getAsJsonObject("content");
                    
                    if (content.has("parts") && content.getAsJsonArray("parts").size() > 0) {
                        JsonObject part = content.getAsJsonArray("parts").get(0).getAsJsonObject();
                        
                        if (part.has("text")) {
                            return part.get("text").getAsString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing Gemini response", e);
        }
        
        return null;
    }
}
