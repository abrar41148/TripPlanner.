package com.example.tripplanner;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatbotFragment extends DialogFragment {

    private static final String TAG = "ChatbotFragment";
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    private RecyclerView rvChatMessages;
    private EditText etChatInput;
    private MaterialButton btnSendMessage;
    private ChatMessageAdapter messageAdapter;
    private List<ChatMessage> messages = new ArrayList<>();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private String destination;
    private long startDate;
    private long endDate;
    private ArrayList<String> activities;
    private int tripId;
    private int planningDays = -1;
    private ItineraryPlannerListener listener;
    private String geminiApiKey;
    private String lastGeneratedJson = null;

    private OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public interface ItineraryPlannerListener {
        void onItineraryReady(String itineraryJson, String name, String description);
    }

    public static ChatbotFragment newInstance(int tripId, String destination, long startDate,
                                             long endDate, ArrayList<String> activities) {
        ChatbotFragment fragment = new ChatbotFragment();
        Bundle args = new Bundle();
        args.putInt("tripId", tripId);
        args.putString("destination", destination);
        args.putLong("startDate", startDate);
        args.putLong("endDate", endDate);
        args.putStringArrayList("activities", activities);
        fragment.setArguments(args);
        return fragment;
    }

    public void setItineraryListener(ItineraryPlannerListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chatbot, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvChatMessages = view.findViewById(R.id.rvChatMessages);
        etChatInput = view.findViewById(R.id.etChatInput);
        btnSendMessage = view.findViewById(R.id.btnSendMessage);

        // Get Gemini API key
        SharedPreferences prefs = getContext().getSharedPreferences(LoginActivity.PREFS_NAME, getContext().MODE_PRIVATE);
        geminiApiKey = prefs.getString("gemini_api_key", "");

        if (geminiApiKey.isEmpty()) {
            addBotMessage("❌ Gemini API key not configured. Set it in Profile settings.");
            btnSendMessage.setEnabled(false);
            etChatInput.setEnabled(false);
            return;
        }

        // Get data from arguments
        if (getArguments() != null) {
            tripId = getArguments().getInt("tripId", -1);
            destination = getArguments().getString("destination", "");
            startDate = getArguments().getLong("startDate", 0);
            endDate = getArguments().getLong("endDate", 0);
            activities = getArguments().getStringArrayList("activities");
        }
        if (activities == null) activities = new ArrayList<>();

        // Setup RecyclerView
        messageAdapter = new ChatMessageAdapter(messages);
        rvChatMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChatMessages.setAdapter(messageAdapter);

        // Send button
        btnSendMessage.setOnClickListener(v -> sendMessage());

        // Initial bot greeting
        String greeting = "Hello! 👋 I'm your AI Itinerary Planner for " + destination + ".\n\n" +
                "Tell me:\n• How many days to plan?\n• Pace preference (relaxed/balanced/active)?";
        addBotMessage(greeting);
    }

    private void sendMessage() {
        String userInput = etChatInput.getText().toString().trim();
        if (userInput.isEmpty()) return;

        addUserMessage(userInput);
        etChatInput.setText("");
        btnSendMessage.setEnabled(false);

        // Check if user wants to save
        if (lastGeneratedJson != null && userInput.toLowerCase().contains("save")) {
            saveItinerary();
            return;
        }

        mainHandler.postDelayed(() -> processBotResponse(userInput), 300);
    }

    private void saveItinerary() {
        String name = planningDays + "-Day " + destination;
        String description = "AI-generated itinerary";

        // Save directly to DB
        try {
            DatabaseHelper db = new DatabaseHelper(getContext());
            long id = db.insertItinerary(tripId, name, description, lastGeneratedJson);
            Log.d(TAG, "Itinerary saved with id=" + id + " tripId=" + tripId);

            // Also notify listener if set
            if (listener != null) {
                listener.onItineraryReady(lastGeneratedJson, name, description);
            }

            addBotMessage("✅ Itinerary saved! Close this chat and check the Itinerary tab.");
            btnSendMessage.setEnabled(false);
            etChatInput.setEnabled(false);
        } catch (Exception e) {
            Log.e(TAG, "Error saving itinerary", e);
            addBotMessage("❌ Error saving: " + e.getMessage());
            btnSendMessage.setEnabled(true);
        }
    }

    private void addUserMessage(String message) {
        messages.add(new ChatMessage(ChatMessage.TYPE_USER, message));
        messageAdapter.notifyItemInserted(messages.size() - 1);
        rvChatMessages.scrollToPosition(messages.size() - 1);
    }

    private void addBotMessage(String message) {
        messages.add(new ChatMessage(ChatMessage.TYPE_BOT, message));
        messageAdapter.notifyItemInserted(messages.size() - 1);
        rvChatMessages.scrollToPosition(messages.size() - 1);
    }

    private void processBotResponse(String userInput) {
        String lowerInput = userInput.toLowerCase();

        if (planningDays == -1) {
            int days = extractNumberFromText(userInput);
            if (days > 0) {
                planningDays = days;
                addBotMessage("Creating " + days + "-day itinerary.\n\nPace: 1️⃣ Relaxed | 2️⃣ Balanced | 3️⃣ Active");
                btnSendMessage.setEnabled(true);
            } else {
                addBotMessage("How many days? (e.g., '3 days')");
                btnSendMessage.setEnabled(true);
            }
        } else {
            String pace = "balanced";
            if (lowerInput.contains("active") || lowerInput.contains("fast") || lowerInput.contains("3")) {
                pace = "active";
            } else if (lowerInput.contains("relax") || lowerInput.contains("slow") || lowerInput.contains("1")) {
                pace = "relaxed";
            }

            addBotMessage("⏳ Generating your " + pace + " itinerary with AI...");
            generateItineraryWithGemini(pace);
        }
    }

    private int extractNumberFromText(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                StringBuilder number = new StringBuilder();
                while (i < text.length() && Character.isDigit(text.charAt(i))) {
                    number.append(text.charAt(i));
                    i++;
                }
                return Integer.parseInt(number.toString());
            }
        }
        return -1;
    }

    private void generateItineraryWithGemini(String pace) {
        String prompt = "Generate a " + planningDays + "-day " + pace + " travel itinerary for " + destination + ". " +
                "Include activities like: " + String.join(", ", activities) + ". " +
                "Return ONLY a valid JSON array (no markdown, no code blocks, no explanation). " +
                "Format: [{\"day\":1,\"time\":\"9:00 AM\",\"name\":\"Place Name\",\"duration\":\"2 hours\",\"category\":\"Sightseeing\"}, ...]. " +
                "Category must be one of: Sightseeing, Museum, Food, Nature, Shopping, Beach, Sports, Nightlife, Tour, Relaxation. " +
                "Include " + (planningDays * 4) + " items total across all days. Make sure day numbers are correctly ordered.";

        try {
            // Build Gemini REST API request body
            JSONObject textPart = new JSONObject();
            textPart.put("text", prompt);

            JSONArray partsArray = new JSONArray();
            partsArray.put(textPart);

            JSONObject contentObj = new JSONObject();
            contentObj.put("parts", partsArray);

            JSONArray contentsArray = new JSONArray();
            contentsArray.put(contentObj);

            JSONObject requestBody = new JSONObject();
            requestBody.put("contents", contentsArray);

            String url = "https://generativelanguage.googleapis.com/v1/models/gemini-3.5-flash:generateContent?key=" + geminiApiKey;

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(requestBody.toString(), JSON_MEDIA))
                    .build();

            Log.d(TAG, "Sending request to Gemini API...");

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "API call failed", e);
                    mainHandler.post(() -> {
                        addBotMessage("❌ Network error: " + e.getMessage());
                        btnSendMessage.setEnabled(true);
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "API response code: " + response.code());
                    Log.d(TAG, "API response: " + body.substring(0, Math.min(body.length(), 500)));

                    if (!response.isSuccessful()) {
                        mainHandler.post(() -> {
                            addBotMessage("❌ API error " + response.code() + ": " + body.substring(0, Math.min(body.length(), 200)));
                            btnSendMessage.setEnabled(true);
                        });
                        return;
                    }

                    try {
                        JSONObject root = new JSONObject(body);
                        JSONArray candidates = root.getJSONArray("candidates");
                        JSONObject firstCandidate = candidates.getJSONObject(0);
                        JSONObject content = firstCandidate.getJSONObject("content");
                        JSONArray parts = content.getJSONArray("parts");
                        String text = parts.getJSONObject(0).getString("text");

                        // Clean markdown fences if present
                        text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

                        Log.d(TAG, "Parsed text: " + text.substring(0, Math.min(text.length(), 300)));

                        // Parse as JSON array
                        JSONArray itinerary = new JSONArray(text);
                        lastGeneratedJson = itinerary.toString();

                        // Build display text
                        StringBuilder display = new StringBuilder("<b>🗺️ " + planningDays + "-Day Itinerary</b><br>");
                        int currentDay = -1;
                        for (int i = 0; i < itinerary.length(); i++) {
                            JSONObject item = itinerary.getJSONObject(i);
                            int day = item.optInt("day", 1);
                            if (day != currentDay) {
                                currentDay = day;
                                display.append("<br><b>☀️ DAY ").append(day).append("</b><br>")
                                       .append("━━━━━━━━━━━━━━━━━━━━<br>");
                            }
                            display.append("<b>🕐 ").append(item.optString("time", "")).append("</b><br>")
                                   .append("📍 ").append(item.optString("name", ""))
                                   .append(" (").append(item.optString("duration", "")).append(")<br><br>");
                        }
                        display.append("Type <b>save</b> to save this itinerary!");

                        mainHandler.post(() -> {
                            addBotMessage(display.toString());
                            btnSendMessage.setEnabled(true);
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Parse error", e);
                        mainHandler.post(() -> {
                            addBotMessage("❌ Failed to parse AI response: " + e.getMessage() + "\n\nTry again?");
                            planningDays = -1;
                            btnSendMessage.setEnabled(true);
                        });
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Request build error", e);
            addBotMessage("❌ Error: " + e.getMessage());
            btnSendMessage.setEnabled(true);
        }
    }
}
