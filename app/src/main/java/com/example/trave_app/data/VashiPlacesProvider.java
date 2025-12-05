package com.example.trave_app.data;

import android.content.Context;
import android.text.TextUtils;

import com.example.trave_app.R;
import com.example.trave_app.database.entity.Place;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VashiPlacesProvider {

    public static List<Place> getAllPlaces(Context context) {
        return loadFromRaw(context);
    }

    public static List<Place> getPlacesByCategory(Context context, String category) {
        List<Place> all = loadFromRaw(context);
        if (TextUtils.isEmpty(category)) return all;
        String wanted = category.toLowerCase(Locale.US);
        List<Place> filtered = new ArrayList<>();
        for (Place p : all) {
            if (p.getCategory() != null && p.getCategory().toLowerCase(Locale.US).equals(wanted)) {
                filtered.add(p);
            }
        }
        return filtered;
    }

    private static List<Place> loadFromRaw(Context context) {
        List<Place> places = new ArrayList<>();
        try {
            InputStream is = context.getResources().openRawResource(R.raw.places_vashi);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONArray arr = new JSONArray(sb.toString());
            long now = System.currentTimeMillis();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String name = obj.optString("name");
                String category = obj.optString("category");
                double lat = obj.optDouble("latitude");
                double lon = obj.optDouble("longitude");
                String address = obj.optString("address");
                float rating = (float) obj.optDouble("rating", 4.0);
                String placeId = generateId(category, name);
                Place place = new Place(placeId, name, category, lat, lon, address, rating, false, now);
                places.add(place);
            }
        } catch (Exception ignored) {
        }
        return places;
    }

    private static String generateId(String category, String name) {
        String base = (category == null ? "place" : category) + "_" + (name == null ? "unknown" : name);
        return "vashi_" + base.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "_");
    }
}


