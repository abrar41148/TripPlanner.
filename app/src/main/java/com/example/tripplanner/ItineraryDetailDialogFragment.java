package com.example.tripplanner;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ItineraryDetailDialogFragment extends DialogFragment {

    private static final String TAG = "ItineraryDetailDF";

    private int itineraryId;
    private int tripId;
    private String itineraryName;
    private String itineraryDesc;
    private String attractionsJson;

    private List<DetailItem> items = new ArrayList<>();
    private DatabaseHelper db;

    private TextView tvProgress;
    private LinearProgressIndicator progressBar;

    public interface OnItineraryStateChangedListener {
        void onItineraryUpdated();
    }

    private OnItineraryStateChangedListener updateListener;

    public static ItineraryDetailDialogFragment newInstance(int id, int tripId, String name, String desc, String attractionsJson) {
        ItineraryDetailDialogFragment fragment = new ItineraryDetailDialogFragment();
        Bundle args = new Bundle();
        args.putInt("id", id);
        args.putInt("tripId", tripId);
        args.putString("name", name);
        args.putString("desc", desc);
        args.putString("attractions", attractionsJson);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnItineraryStateChangedListener(OnItineraryStateChangedListener listener) {
        this.updateListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DatabaseHelper(getContext());
        if (getArguments() != null) {
            itineraryId = getArguments().getInt("id");
            tripId = getArguments().getInt("tripId");
            itineraryName = getArguments().getString("name");
            itineraryDesc = getArguments().getString("desc");
            attractionsJson = getArguments().getString("attractions");
        }
        parseAttractions();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_itinerary_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvTitle = view.findViewById(R.id.tvItineraryTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvItinerarySubtitle);
        MaterialButton btnClose = view.findViewById(R.id.btnClose);
        RecyclerView rvItems = view.findViewById(R.id.rvItineraryItems);
        tvProgress = view.findViewById(R.id.tvProgress);
        progressBar = view.findViewById(R.id.progressBar);

        tvTitle.setText(itineraryName);
        tvSubtitle.setText(itineraryDesc);

        btnClose.setOnClickListener(v -> dismiss());

        rvItems.setLayoutManager(new LinearLayoutManager(getContext()));
        rvItems.setAdapter(new DetailAdapter(items));

        updateProgress();
    }

    private void updateProgress() {
        if (tvProgress == null || progressBar == null) return;
        int total = items.size();
        int done = 0;
        for (DetailItem item : items) {
            if (item.completed) done++;
        }
        tvProgress.setText(done + " / " + total + " completed");
        progressBar.setMax(total);
        progressBar.setProgress(done);
    }

    private void parseAttractions() {
        items.clear();
        try {
            JSONArray arr = new JSONArray(attractionsJson);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                DetailItem item = new DetailItem();
                item.day = obj.optInt("day", 1);
                item.time = obj.optString("time", "");
                item.name = obj.optString("name", "");
                item.duration = obj.optString("duration", "");
                item.category = obj.optString("category", "Activity");
                item.completed = obj.optBoolean("completed", false);
                items.add(item);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing attractions", e);
        }
    }

    private void saveCheckedStates() {
        try {
            JSONArray arr = new JSONArray();
            for (DetailItem item : items) {
                JSONObject obj = new JSONObject();
                obj.put("day", item.day);
                obj.put("time", item.time);
                obj.put("name", item.name);
                obj.put("duration", item.duration);
                obj.put("category", item.category);
                obj.put("completed", item.completed);
                arr.put(obj);
            }
            db.getWritableDatabase().execSQL(
                    "UPDATE " + DatabaseHelper.TABLE_ITINERARIES +
                            " SET " + DatabaseHelper.COL_ITIN_ATTRACTIONS + " = ?" +
                            " WHERE " + DatabaseHelper.COL_ITIN_ID + " = ?",
                    new Object[]{arr.toString(), itineraryId}
            );

            if (updateListener != null) {
                updateListener.onItineraryUpdated();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to save checklist state", e);
        }
    }

    private static class DetailItem {
        int day;
        String time;
        String name;
        String duration;
        String category;
        boolean completed;
    }

    private class DetailAdapter extends RecyclerView.Adapter<DetailAdapter.DetailViewHolder> {

        private List<DetailItem> itemsList;

        DetailAdapter(List<DetailItem> itemsList) {
            this.itemsList = itemsList;
        }

        @NonNull
        @Override
        public DetailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_itinerary_detail_check, parent, false);
            return new DetailViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DetailViewHolder holder, int position) {
            DetailItem item = itemsList.get(position);

            holder.tvTime.setText("🕐 " + item.time);
            holder.tvName.setText(item.name);
            holder.tvDuration.setText("⏱ " + item.duration);
            holder.tvCategory.setText(getCategoryEmoji(item.category) + " " + item.category);

            // Checkbox state
            holder.cbCompleted.setOnCheckedChangeListener(null);
            holder.cbCompleted.setChecked(item.completed);

            // Strikethrough + fade for completed
            applyCompletedStyle(holder, item.completed);

            // Card click toggles checkbox
            holder.cardItem.setOnClickListener(v -> {
                boolean newState = !holder.cbCompleted.isChecked();
                holder.cbCompleted.setChecked(newState);
                item.completed = newState;
                applyCompletedStyle(holder, newState);
                saveCheckedStates();
                updateProgress();
            });

            // Day header
            if (position == 0 || itemsList.get(position - 1).day != item.day) {
                holder.tvDayHeader.setText("DAY " + item.day);
                holder.llDayHeader.setVisibility(View.VISIBLE);
            } else {
                holder.llDayHeader.setVisibility(View.GONE);
            }
        }

        private String getCategoryEmoji(String category) {
            if (category == null) return "📍";
            String lower = category.toLowerCase();
            if (lower.contains("museum") || lower.contains("art") || lower.contains("histor")) return "🏛";
            if (lower.contains("food") || lower.contains("restaurant") || lower.contains("dining") || lower.contains("cafe")) return "🍽";
            if (lower.contains("park") || lower.contains("garden") || lower.contains("nature") || lower.contains("hike")) return "🌿";
            if (lower.contains("shop") || lower.contains("market")) return "🛍";
            if (lower.contains("beach") || lower.contains("swim") || lower.contains("water")) return "🏖";
            if (lower.contains("sport") || lower.contains("bike") || lower.contains("run") || lower.contains("active")) return "🚴";
            if (lower.contains("night") || lower.contains("bar") || lower.contains("club")) return "🌙";
            if (lower.contains("sight") || lower.contains("landmark") || lower.contains("tower") || lower.contains("monument")) return "🏰";
            if (lower.contains("tour") || lower.contains("walk")) return "🚶";
            if (lower.contains("relax") || lower.contains("spa")) return "🧘";
            return "📍";
        }

        private void applyCompletedStyle(DetailViewHolder holder, boolean completed) {
            if (completed) {
                holder.tvName.setPaintFlags(holder.tvName.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                holder.tvName.setAlpha(0.4f);
                holder.tvTime.setAlpha(0.4f);
                holder.tvDuration.setAlpha(0.4f);
                holder.tvCategory.setAlpha(0.4f);
            } else {
                holder.tvName.setPaintFlags(holder.tvName.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                holder.tvName.setAlpha(1.0f);
                holder.tvTime.setAlpha(1.0f);
                holder.tvDuration.setAlpha(1.0f);
                holder.tvCategory.setAlpha(1.0f);
            }
        }

        @Override
        public int getItemCount() {
            return itemsList.size();
        }

        class DetailViewHolder extends RecyclerView.ViewHolder {
            LinearLayout llDayHeader;
            TextView tvDayHeader, tvTime, tvName, tvDuration, tvCategory;
            MaterialCheckBox cbCompleted;
            com.google.android.material.card.MaterialCardView cardItem;

            DetailViewHolder(@NonNull View itemView) {
                super(itemView);
                llDayHeader = itemView.findViewById(R.id.llDayHeader);
                tvDayHeader = itemView.findViewById(R.id.tvDayHeader);
                tvTime = itemView.findViewById(R.id.tvTime);
                tvName = itemView.findViewById(R.id.tvName);
                tvDuration = itemView.findViewById(R.id.tvDuration);
                tvCategory = itemView.findViewById(R.id.tvCategory);
                cbCompleted = itemView.findViewById(R.id.cbCompleted);
                cardItem = itemView.findViewById(R.id.cardItem);
            }
        }
    }
}
