package com.example.tripplanner;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;

public class TripPagerAdapter extends FragmentStateAdapter {

    String destination;
    long startDate, endDate;
    ArrayList<String> activities;
    int tripId;

    public TripPagerAdapter(FragmentActivity activity, String destination,
                            long startDate, long endDate, ArrayList<String> activities, int tripId) {
        super(activity);
        this.destination = destination;
        this.startDate = startDate;
        this.endDate = endDate;
        this.activities = activities;
        this.tripId = tripId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Bundle args = new Bundle();
        args.putString("destination", destination);
        args.putLong("startDate", startDate);
        args.putLong("endDate", endDate);
        args.putStringArrayList("activities", activities);
        args.putInt("tripId", tripId);

        Fragment fragment;
        if (position == 0) {
            fragment = new WeatherFragment();
        } else if (position == 1) {
            fragment = new AttractionsFragment();
        } else if (position == 2) {
            fragment = new VisitedFragment();
        } else if (position == 3) {
            fragment = new PackListFragment();
        } else {
            fragment = ItineraryFragment.newInstance(tripId, destination, startDate, endDate, activities);
            fragment.setArguments(args);
            return fragment;
        }

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}
