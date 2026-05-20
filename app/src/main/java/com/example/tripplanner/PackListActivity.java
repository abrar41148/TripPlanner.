package com.example.tripplanner;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class PackListActivity extends AppCompatActivity {

    MaterialButton btnBack;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pack_list);

        sharedPreferences = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);

        boolean hasActive = sharedPreferences.getBoolean("has_active_plan", false);
        if (!hasActive) {
            Toast.makeText(this, "Please plan a trip first to view packing list", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        String destination = sharedPreferences.getString("last_destination", "");
        long startDate = sharedPreferences.getLong("last_start_date", 0);
        long endDate = sharedPreferences.getLong("last_end_date", 0);
        java.util.Set<String> activitiesSet = sharedPreferences.getStringSet("last_activities", new java.util.HashSet<>());
        ArrayList<String> activities = new ArrayList<>(activitiesSet);

        // Put args in Bundle for PackListFragment
        Bundle args = new Bundle();
        args.putString("destination", destination);
        args.putLong("startDate", startDate);
        args.putLong("endDate", endDate);
        args.putStringArrayList("activities", activities);

        PackListFragment fragment = new PackListFragment();
        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
