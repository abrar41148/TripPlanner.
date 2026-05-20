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

        holder.btnViewDetails.setOnClickListener(v -> listener.onViewDetails(item));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return itineraries.size();
    }

    static class ItineraryViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDescription, tvAttractionCount;
        MaterialButton btnViewDetails, btnDelete;

        ItineraryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItineraryName);
            tvDescription = itemView.findViewById(R.id.tvItineraryDescription);
            tvAttractionCount = itemView.findViewById(R.id.tvAttractionCount);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnDelete = itemView.findViewById(R.id.btnDeleteItinerary);
        }
    }
}
