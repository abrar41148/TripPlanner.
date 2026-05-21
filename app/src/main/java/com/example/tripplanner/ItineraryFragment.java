package com.example.tripplanner;


import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ItineraryFragment extends Fragment implements ItineraryAdapter.ItineraryActionListener, ChatbotFragment.ItineraryPlannerListener {

    private RecyclerView rvItineraries;
    private MaterialButton btnPlanItinerary;
    private LinearLayout llEmptyState;
    private ItineraryAdapter adapter;
    private List<ItineraryItem> itineraries = new ArrayList<>();
    private DatabaseHelper db;
    private int tripId;
    private String destination;
    private long startDate;
    private long endDate;
    private ArrayList<String> activities;

    public static ItineraryFragment newInstance(int tripId, String destination, long startDate, 
                                                long endDate, ArrayList<String> activities) {
        ItineraryFragment fragment = new ItineraryFragment();
        Bundle args = new Bundle();
        args.putInt("tripId", tripId);
        args.putString("destination", destination);
        args.putLong("startDate", startDate);
        args.putLong("endDate", endDate);
        args.putStringArrayList("activities", activities);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_itinerary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvItineraries = view.findViewById(R.id.rvItineraries);
        btnPlanItinerary = view.findViewById(R.id.btnPlanItinerary);
        llEmptyState = view.findViewById(R.id.llEmptyState);

        db = new DatabaseHelper(getContext());

        if (getArguments() != null) {
            tripId = getArguments().getInt("tripId", -1);
            destination = getArguments().getString("destination", "");
            startDate = getArguments().getLong("startDate", 0);
            endDate = getArguments().getLong("endDate", 0);
            activities = getArguments().getStringArrayList("activities");
        }

        // Setup RecyclerView
        adapter = new ItineraryAdapter(itineraries, this);
        rvItineraries.setAdapter(adapter);
        rvItineraries.setLayoutManager(new LinearLayoutManager(getContext()));

        // Load itineraries
        loadItineraries();

        // Plan button listener
        btnPlanItinerary.setOnClickListener(v -> startChatbotPlanning());

        Log.d("ItineraryFragment", "tripId=" + tripId + " destination=" + destination);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (db != null) {
            loadItineraries();
        }
    }

    private void loadItineraries() {
        Log.d("ItineraryFragment", "loadItineraries tripId=" + tripId);
        if (tripId == -1) {
            Log.w("ItineraryFragment", "tripId is -1, cannot load itineraries");
            updateEmptyState(true);
            return;
        }

        List<DatabaseHelper.ItineraryRecord> records = db.getItinerariesByTripId(tripId);
        itineraries.clear();

        for (DatabaseHelper.ItineraryRecord record : records) {
            ItineraryItem item = new ItineraryItem(
                    record.id,
                    record.name,
                    record.description,
                    parseAttractions(record.attractions),
                    record.attractions
            );
            itineraries.add(item);
        }

        adapter.notifyDataSetChanged();
        updateEmptyState(itineraries.isEmpty());
    }

    private List<AttractionItem> parseAttractions(String attractionsJson) {
        List<AttractionItem> attractions = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(attractionsJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                attractions.add(new AttractionItem(
                        obj.optString("time"),
                        obj.optString("name"),
                        obj.optString("duration")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return attractions;
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            rvItineraries.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvItineraries.setVisibility(View.VISIBLE);
            llEmptyState.setVisibility(View.GONE);
        }
    }

    private void startChatbotPlanning() {
        ChatbotFragment chatbotFragment = ChatbotFragment.newInstance(tripId, destination, startDate, endDate, activities);
        chatbotFragment.setItineraryListener(this);
        chatbotFragment.show(getChildFragmentManager(), "chatbot");
    }

    @Override
    public void onViewDetails(ItineraryItem item) {
        ItineraryDetailDialogFragment dialog = ItineraryDetailDialogFragment.newInstance(
                item.id,
                tripId,
                item.name,
                item.description,
                item.rawJson
        );
        dialog.setOnItineraryStateChangedListener(this::loadItineraries);
        dialog.show(getChildFragmentManager(), "itinerary_detail");
    }

    @Override
    public void onDelete(ItineraryItem item) {
        db.deleteItinerary(item.id);
        loadItineraries();
        Toast.makeText(getContext(), "Itinerary deleted", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItineraryReady(String itineraryJson, String name, String description) {
        loadItineraries();
        Toast.makeText(getContext(), "Itinerary saved! ✅", Toast.LENGTH_SHORT).show();
    }

    public static class ItineraryItem {
        public int id;
        public String name;
        public String description;
        public List<AttractionItem> attractions;
        public String rawJson;

        public ItineraryItem(int id, String name, String description, List<AttractionItem> attractions, String rawJson) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.attractions = attractions;
            this.rawJson = rawJson;
        }
    }

    public static class AttractionItem {
        public String time;
        public String name;
        public String duration;

        public AttractionItem(String time, String name, String duration) {
            this.time = time;
            this.name = name;
            this.duration = duration;
        }
    }
}
