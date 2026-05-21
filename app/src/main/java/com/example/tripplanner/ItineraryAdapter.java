package com.example.tripplanner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.ItineraryViewHolder> {

    private List<ItineraryFragment.ItineraryItem> itineraries;
    private ItineraryActionListener listener;

    public interface ItineraryActionListener {
        void onViewDetails(ItineraryFragment.ItineraryItem item);
        void onDelete(ItineraryFragment.ItineraryItem item);
    }

    public ItineraryAdapter(List<ItineraryFragment.ItineraryItem> itineraries, ItineraryActionListener listener) {
        this.itineraries = itineraries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ItineraryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_itinerary, parent, false);
        return new ItineraryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ItineraryViewHolder holder, int position) {
        ItineraryFragment.ItineraryItem item = itineraries.get(position);
        holder.tvName.setText(item.name);
        holder.tvDescription.setText(item.description);
        holder.tvAttractionCount.setText("📍 " + item.attractions.size() + " activities");

        
        // Strip the pace suffix (e.g. " (relaxed)") from display description so it looks super clean!
        String cleanDescription = item.description;
        String paceText = "balanced";
        if (item.description != null) {
            String descLower = item.description.toLowerCase();
            if (descLower.contains("relaxed")) {
                paceText = "relaxed";
            } else if (descLower.contains("active")) {
                paceText = "active";
            }
            cleanDescription = item.description.replaceAll("\\s*\\((relaxed|balanced|active)\\)", "");
        }
        holder.tvDescription.setText(cleanDescription);
        holder.tvAttractionCount.setText("📍 " + item.attractions.size() + " activities");

        if (holder.tvItineraryPace != null) {
            if (paceText.equals("relaxed")) {
                holder.tvItineraryPace.setText("☕ Relaxed");
                holder.tvItineraryPace.setTextColor(holder.itemView.getContext().getColor(R.color.accent_teal));
            } else if (paceText.equals("active")) {
                holder.tvItineraryPace.setText("⚡ Active");
                holder.tvItineraryPace.setTextColor(holder.itemView.getContext().getColor(R.color.accent_blue));
            } else {
                holder.tvItineraryPace.setText("🏃 Balanced");
                holder.tvItineraryPace.setTextColor(holder.itemView.getContext().getColor(R.color.accent_blue));
            }
        }

        holder.btnViewDetails.setOnClickListener(v -> listener.onViewDetails(item));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return itineraries.size();
    }

    static class ItineraryViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDescription, tvAttractionCount;
        TextView tvName, tvDescription, tvAttractionCount, tvItineraryPace;
        MaterialButton btnViewDetails, btnDelete;

        ItineraryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItineraryName);
            tvDescription = itemView.findViewById(R.id.tvItineraryDescription);
            tvAttractionCount = itemView.findViewById(R.id.tvAttractionCount);
            tvItineraryPace = itemView.findViewById(R.id.tvItineraryPace);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnDelete = itemView.findViewById(R.id.btnDeleteItinerary);
        }
    }
}
