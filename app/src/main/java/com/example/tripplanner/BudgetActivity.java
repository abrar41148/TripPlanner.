package com.example.tripplanner;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class BudgetActivity extends AppCompatActivity {

    TextInputEditText etTotalBudget, etAccommodation, etFood, etTransport, etActivities;
    TextView tvSpent, tvRemaining;
    TextView tvAccommodationTotal, tvFoodTotal, tvTransportTotal, tvActivitiesTotal;
    ChipGroup chipGroupAccommodation, chipGroupFood, chipGroupTransport, chipGroupActivities;
    MaterialButton btnAddAccommodation, btnAddFood, btnAddTransport, btnAddActivities;
    MaterialButton btnReset, btnBack;
    SharedPreferences sharedPreferences;
    String tripPrefix = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        sharedPreferences = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);

        boolean hasActive = sharedPreferences.getBoolean("has_active_plan", false);
        if (!hasActive) {
            Toast.makeText(this, "Please plan a trip first to manage budget", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String lastDest = sharedPreferences.getString("last_destination", "");
        long lastStart = sharedPreferences.getLong("last_start_date", 0);
        tripPrefix = lastDest + "_" + lastStart + "_";

        // Summary
        tvSpent = findViewById(R.id.tvSpent);
        tvRemaining = findViewById(R.id.tvRemaining);

        // Total budget
        etTotalBudget = findViewById(R.id.etTotalBudget);

        // Category inputs
        etAccommodation = findViewById(R.id.etAccommodation);
        etFood = findViewById(R.id.etFood);
        etTransport = findViewById(R.id.etTransport);
        etActivities = findViewById(R.id.etActivities);

        // Category totals
        tvAccommodationTotal = findViewById(R.id.tvAccommodationTotal);
        tvFoodTotal = findViewById(R.id.tvFoodTotal);
        tvTransportTotal = findViewById(R.id.tvTransportTotal);
        tvActivitiesTotal = findViewById(R.id.tvActivitiesTotal);

        // Chip groups
        chipGroupAccommodation = findViewById(R.id.chipGroupAccommodation);
        chipGroupFood = findViewById(R.id.chipGroupFood);
        chipGroupTransport = findViewById(R.id.chipGroupTransport);
        chipGroupActivities = findViewById(R.id.chipGroupActivities);

        // Add buttons
        btnAddAccommodation = findViewById(R.id.btnAddAccommodation);
        btnAddFood = findViewById(R.id.btnAddFood);
        btnAddTransport = findViewById(R.id.btnAddTransport);
        btnAddActivities = findViewById(R.id.btnAddActivities);

        btnReset = findViewById(R.id.btnReset);
        btnBack = findViewById(R.id.btnBack);

        // Load saved total budget
        etTotalBudget.setText(sharedPreferences.getString(tripPrefix + "budget_total", ""));

        // Auto-save total budget on focus loss
        etTotalBudget.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                saveTotalBudget();
                refreshSummary();
            }
        });

        // Add button listeners
        btnAddAccommodation.setOnClickListener(v -> addEntry("budget_accommodation_entries", etAccommodation, chipGroupAccommodation, tvAccommodationTotal));
        btnAddFood.setOnClickListener(v -> addEntry("budget_food_entries", etFood, chipGroupFood, tvFoodTotal));
        btnAddTransport.setOnClickListener(v -> addEntry("budget_transport_entries", etTransport, chipGroupTransport, tvTransportTotal));
        btnAddActivities.setOnClickListener(v -> addEntry("budget_activities_entries", etActivities, chipGroupActivities, tvActivitiesTotal));

        btnReset.setOnClickListener(v -> confirmReset());
        btnBack.setOnClickListener(v -> finish());

        // Load and display all
        loadCategory("budget_accommodation_entries", chipGroupAccommodation, tvAccommodationTotal);
        loadCategory("budget_food_entries", chipGroupFood, tvFoodTotal);
        loadCategory("budget_transport_entries", chipGroupTransport, tvTransportTotal);
        loadCategory("budget_activities_entries", chipGroupActivities, tvActivitiesTotal);
        refreshSummary();
    }

    // ──── Entry management ────

    void addEntry(String entriesKey, TextInputEditText input, ChipGroup chipGroup, TextView totalView) {
        double amount = parseDoubleFromInput(input);
        if (amount <= 0) {
            Toast.makeText(this, "Enter an amount to add", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullKey = tripPrefix + entriesKey;
        List<String> entries = getEntries(fullKey);
        entries.add(formatAmount(amount));
        saveEntries(fullKey, entries);

        // Sync old-format total key for dashboard compatibility
        syncLegacyTotal(entriesKey);

        input.setText("");
        rebuildChips(fullKey, chipGroup, totalView);
        refreshSummary();

        Toast.makeText(this, String.format("+₹%s added", formatAmount(amount)), Toast.LENGTH_SHORT).show();
    }

    void removeEntry(String entriesKey, int index, ChipGroup chipGroup, TextView totalView) {
        String fullKey = tripPrefix + entriesKey;
        List<String> entries = getEntries(fullKey);
        if (index >= 0 && index < entries.size()) {
            String removed = entries.remove(index);
            saveEntries(fullKey, entries);
            syncLegacyTotal(entriesKey);
            rebuildChips(fullKey, chipGroup, totalView);
            refreshSummary();
            Toast.makeText(this, String.format("-₹%s removed", removed), Toast.LENGTH_SHORT).show();
        }
    }

    // ──── Chips ────

    void loadCategory(String entriesKey, ChipGroup chipGroup, TextView totalView) {
        // Migrate old single-value format to entries if needed
        migrateOldFormat(entriesKey);
        rebuildChips(tripPrefix + entriesKey, chipGroup, totalView);
    }

    void rebuildChips(String fullKey, ChipGroup chipGroup, TextView totalView) {
        chipGroup.removeAllViews();
        List<String> entries = getEntries(fullKey);
        double total = 0;

        for (int i = 0; i < entries.size(); i++) {
            double val = parseDouble(entries.get(i));
            total += val;
            Chip chip = createChip(entries.get(i), fullKey, i, chipGroup, totalView);
            chipGroup.addView(chip);
        }

        // Dynamic layout: hide scroll area when no chips, input expands full
        android.view.View scrollView = (android.view.View) chipGroup.getParent();
        scrollView.setVisibility(entries.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);

        updateTotal(totalView, total);
    }

    Chip createChip(String amountStr, String fullKey, int index, ChipGroup chipGroup, TextView totalView) {
        Chip chip = new Chip(this);
        chip.setText("₹" + amountStr);
        chip.setCloseIconVisible(true);
        chip.setCheckable(false);

        // Dark theme styling
        chip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.surface_dark, null)));
        chip.setTextColor(getResources().getColor(R.color.text_primary, null));
        chip.setCloseIconTint(ColorStateList.valueOf(getResources().getColor(R.color.text_secondary, null)));
        chip.setChipStrokeColor(ColorStateList.valueOf(getResources().getColor(R.color.divider, null)));
        chip.setChipStrokeWidth(1f);

        // Derive entriesKey from fullKey by removing tripPrefix
        String entriesKey = fullKey.replace(tripPrefix, "");

        chip.setOnCloseIconClickListener(v -> removeEntry(entriesKey, index, chipGroup, totalView));

        return chip;
    }

    // ──── Storage helpers ────

    List<String> getEntries(String fullKey) {
        String raw = sharedPreferences.getString(fullKey, "");
        List<String> list = new ArrayList<>();
        if (raw != null && !raw.isEmpty()) {
            String[] parts = raw.split(",");
            for (String p : parts) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty()) {
                    list.add(trimmed);
                }
            }
        }
        return list;
    }

    void saveEntries(String fullKey, List<String> entries) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(entries.get(i));
        }
        sharedPreferences.edit().putString(fullKey, sb.toString()).apply();
    }

    /** Keep legacy budget_accommodation etc. keys in sync so dashboard reads work */
    void syncLegacyTotal(String entriesKey) {
        // entriesKey = "budget_accommodation_entries" → legacyKey = "budget_accommodation"
        String legacyKey = entriesKey.replace("_entries", "");
        String fullEntriesKey = tripPrefix + entriesKey;
        List<String> entries = getEntries(fullEntriesKey);
        double total = 0;
        for (String e : entries) total += parseDouble(e);
        sharedPreferences.edit().putString(tripPrefix + legacyKey, String.valueOf(total)).apply();
    }

    /** One-time migration: if old single-value key exists but no entries key, convert */
    void migrateOldFormat(String entriesKey) {
        String legacyKey = entriesKey.replace("_entries", "");
        String fullEntriesKey = tripPrefix + entriesKey;
        String fullLegacyKey = tripPrefix + legacyKey;

        // Only migrate if entries key doesn't exist yet but legacy does
        if (!sharedPreferences.contains(fullEntriesKey) && sharedPreferences.contains(fullLegacyKey)) {
            String oldVal = sharedPreferences.getString(fullLegacyKey, "0");
            double oldAmount = parseDouble(oldVal);
            if (oldAmount > 0) {
                sharedPreferences.edit().putString(fullEntriesKey, formatAmount(oldAmount)).apply();
            }
        }
    }

    // ──── Summary ────

    void refreshSummary() {
        saveTotalBudget();
        double total = parseDoubleFromInput(etTotalBudget);

        double spent = sumEntries("budget_accommodation_entries")
                     + sumEntries("budget_food_entries")
                     + sumEntries("budget_transport_entries")
                     + sumEntries("budget_activities_entries");

        double remaining = total - spent;

        tvSpent.setText(String.format("₹%.2f", spent));
        if (remaining >= 0) {
            tvRemaining.setText(String.format("₹%.2f", remaining));
            tvRemaining.setTextColor(getResources().getColor(R.color.accent_teal, null));
        } else {
            tvRemaining.setText(String.format("-₹%.2f", Math.abs(remaining)));
            tvRemaining.setTextColor(getResources().getColor(android.R.color.holo_red_light, null));
        }
    }

    double sumEntries(String entriesKey) {
        List<String> entries = getEntries(tripPrefix + entriesKey);
        double sum = 0;
        for (String e : entries) sum += parseDouble(e);
        return sum;
    }

    void updateTotal(TextView tv, double value) {
        tv.setText(String.format("₹%s", formatAmount(value)));
    }

    void saveTotalBudget() {
        sharedPreferences.edit()
            .putString(tripPrefix + "budget_total", getStr(etTotalBudget))
            .apply();
    }

    // ──── Reset ────

    void confirmReset() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Reset Expenses")
            .setMessage("This will delete all expense entries. Your total budget will be kept. Continue?")
            .setPositiveButton("Reset", (d, w) -> {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(tripPrefix + "budget_accommodation_entries", "");
                editor.putString(tripPrefix + "budget_food_entries", "");
                editor.putString(tripPrefix + "budget_transport_entries", "");
                editor.putString(tripPrefix + "budget_activities_entries", "");
                // Sync legacy keys
                editor.putString(tripPrefix + "budget_accommodation", "0");
                editor.putString(tripPrefix + "budget_food", "0");
                editor.putString(tripPrefix + "budget_transport", "0");
                editor.putString(tripPrefix + "budget_activities", "0");
                editor.apply();

                rebuildChips(tripPrefix + "budget_accommodation_entries", chipGroupAccommodation, tvAccommodationTotal);
                rebuildChips(tripPrefix + "budget_food_entries", chipGroupFood, tvFoodTotal);
                rebuildChips(tripPrefix + "budget_transport_entries", chipGroupTransport, tvTransportTotal);
                rebuildChips(tripPrefix + "budget_activities_entries", chipGroupActivities, tvActivitiesTotal);
                refreshSummary();
                Toast.makeText(this, "All expenses reset", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ──── Util ────

    String formatAmount(double val) {
        if (val == (long) val) {
            return String.valueOf((long) val);
        }
        return String.format("%.2f", val);
    }

    double parseDouble(String s) {
        try {
            return (s == null || s.trim().isEmpty()) ? 0 : Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    double parseDoubleFromInput(TextInputEditText et) {
        try {
            String text = et.getText() != null ? et.getText().toString().trim() : "";
            return text.isEmpty() ? 0 : Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    String getStr(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
