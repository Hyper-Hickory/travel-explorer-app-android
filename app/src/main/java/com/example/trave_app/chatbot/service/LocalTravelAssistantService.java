package com.example.trave_app.chatbot.service;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.example.trave_app.database.entity.Place;
import com.example.trave_app.data.VashiPlacesProvider;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Lightweight local Q&A based Travel Assistant.
 * Loads up to 10,000 Q&A pairs from assets/travel_faq.json and does keyword-based retrieval.
 */
public class LocalTravelAssistantService {
    private static final String TAG = "LocalTAService";
    private static final String ASSET_FILE = "travel_faq.json";

    public static class QAItem {
        public String q;
        public String a;
        public List<String> tags;
    }

    private final Context context;
    private final Gson gson;
    private List<QAItem> kb;

    public LocalTravelAssistantService(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        loadKnowledgeBase();
    }

    private void loadKnowledgeBase() {
        AssetManager am = context.getAssets();
        try (InputStream is = am.open(ASSET_FILE);
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            Type listType = new TypeToken<List<QAItem>>(){}.getType();
            List<QAItem> parsed = gson.fromJson(sb.toString(), listType);
            if (parsed == null) parsed = new ArrayList<>();
            // Cap to 10k to ensure performance
            if (parsed.size() > 10000) {
                kb = new ArrayList<>(parsed.subList(0, 10000));
            } else {
                kb = parsed;
            }
        } catch (IOException e) {
            Log.w(TAG, "No travel_faq.json found or failed to read. Using empty KB.", e);
            kb = new ArrayList<>();
        }
    }

    public String answer(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "Please type your question about restaurants, parks, hostels, cafes, hotels, or malls.";
        }
        String q = userMessage.toLowerCase(Locale.ROOT);

        // Deterministic local handler for Navi Mumbai categories
        try {
            if (isNaviMumbaiPlaceRequest(q)) {
                String resp = buildNaviMumbaiResponse(q);
                if (resp != null && !resp.isEmpty()) return resp;
            }
        } catch (Exception e) {
            Log.e(TAG, "Local Navi Mumbai handler error", e);
        }

        if (kb.isEmpty()) {
            return "Travel Assistant knowledge base is empty. Please add entries to travel_faq.json.";
        }
        // Score each QAItem by keyword overlap
        List<Scored> scored = new ArrayList<>();
        for (QAItem item : kb) {
            int score = score(q, item);
            if (score > 0) scored.add(new Scored(item, score));
        }
        if (scored.isEmpty()) {
            return "I couldn't find an exact match. Try asking more specifically (e.g., 'best cafes near city center' or 'budget hostels with wifi').";
        }
        Collections.sort(scored, Comparator.comparingInt((Scored s) -> s.score).reversed());
        return scored.get(0).item.a;
    }

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
        return mentionsCategory || mentionsLocation;
    }

    private String buildNaviMumbaiResponse(String lowerMsg) {
        String[] allCategories = new String[]{"hotels", "hostels", "restaurants", "cafes", "parks", "malls"};

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
            Collections.addAll(requested, allCategories);
        }

        Map<String, String> titleMap = new LinkedHashMap<>();
        titleMap.put("hotels", "🏨 Hotels");
        titleMap.put("hostels", "🛏️ Hostels");
        titleMap.put("restaurants", "🍽️ Restaurants");
        titleMap.put("cafes", "☕ Cafes");
        titleMap.put("parks", "🌳 Parks");
        titleMap.put("malls", "🛍️ Malls");

        StringBuilder sb = new StringBuilder();
        sb.append("📍 Navi Mumbai Recommendations\n\n");
        sb.append("Here are places I can suggest in Navi Mumbai (Vashi/nearby):\n\n");

        int categoriesIncluded = 0;
        for (String cat : requested) {
            List<Place> list;
            try {
                list = VashiPlacesProvider.getPlacesByCategory(context, cat);
            } catch (Exception e) {
                list = new ArrayList<>();
            }
            if (list == null || list.isEmpty()) continue;

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

            sb.append("**").append(titleMap.get(cat)).append(" (" ).append(Math.min(5, list.size())).append(")**\n");
            int limit = Math.min(5, list.size());
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
                  .append(" (")
                  .append(p.getCategory() == null ? "place" : p.getCategory())
                  .append(") — ⭐ ").append(p.getRating()).append("\n");
                if (p.getAddress() != null && !p.getAddress().isEmpty()) {
                    sb.append("   ").append(p.getAddress()).append("\n");
                }
            }
        }

        sb.append("➡️ Ask for a specific category or area (e.g., Vashi, Nerul, Seawoods) for more focused suggestions.");
        return sb.toString();
    }

    private static class Scored {
        QAItem item; int score; Scored(QAItem i, int s){item=i;score=s;}
    }

    private int score(String q, QAItem item) {
        int s = 0;
        // Keyword overlap with question
        if (item.q != null) {
            String iq = item.q.toLowerCase(Locale.ROOT);
            s += overlap(q, iq) * 3;
        }
        // Tags contribute
        if (item.tags != null) {
            for (String t : item.tags) {
                if (t == null) continue;
                String lt = t.toLowerCase(Locale.ROOT);
                if (q.contains(lt)) s += 2;
            }
        }
        // Category hints
        if (containsAny(q, new String[]{"restaurant","restaurants"}) && hasTag(item, "restaurants")) s += 2;
        if (containsAny(q, new String[]{"cafe","cafes","coffee"}) && hasTag(item, "cafes")) s += 2;
        if (containsAny(q, new String[]{"hotel","hotels"}) && hasTag(item, "hotels")) s += 2;
        if (containsAny(q, new String[]{"hostel","hostels"}) && hasTag(item, "hostels")) s += 2;
        if (containsAny(q, new String[]{"park","parks"}) && hasTag(item, "parks")) s += 2;
        if (containsAny(q, new String[]{"mall","malls","shopping"}) && hasTag(item, "malls")) s += 2;
        return s;
    }

    private boolean hasTag(QAItem item, String tag) {
        if (item.tags == null) return false;
        for (String t : item.tags) {
            if (t != null && t.equalsIgnoreCase(tag)) return true;
        }
        return false;
    }

    private boolean containsAny(String text, String[] kws) {
        for (String k : kws) if (text.contains(k)) return true;
        return false;
    }

    private int overlap(String a, String b) {
        // crude token overlap
        String[] at = a.split("\\W+");
        String[] bt = b.split("\\W+");
        int matches = 0;
        for (String x : at) {
            if (x.length() < 3) continue;
            for (String y : bt) {
                if (x.equals(y)) { matches++; break; }
            }
        }
        return matches;
    }
}
