package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * MainActivity is the central hub of the application after login.
 * It hosts the BottomNavigationView and manages the swapping of Fragments.
 *
 * Responsibilities:
 * 1. Validates session (redirects to LoginActivity if not logged in).
 * 2. Sets up the bottom navigation menu.
 * 3. Handles switching between Home, Analytics, Add, Budget, and Settings fragments.
 */
public class MainActivity extends AppCompatActivity {
    public BottomNavigationView bottomNavigation;
    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Load dark mode preference
        SharedPreferences prefs = getSharedPreferences("AppSettings", 0);
        int darkMode = prefs.getInt("dark_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(darkMode);
        
        setContentView(R.layout.activity_main);

        dataManager = DataManager.getInstance(this);

        // Security check: Ensure user is still logged in when reaching MainActivity
        if (dataManager.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        bottomNavigation = findViewById(R.id.bottomNavigation);
        
        // Handle navigation item clicks
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            
            // Map menu IDs to correspond Fragments
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_analytics) {
                selectedFragment = new AnalyticsFragment();
            } else if (itemId == R.id.nav_add) {
                selectedFragment = new AddExpenseFragment();
            } else if (itemId == R.id.nav_budget) {
                selectedFragment = new BudgetFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            // Perform the fragment transaction
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, selectedFragment)
                    .commit();
                return true;
            }
            return false;
        });

        // Load default fragment
        if (savedInstanceState == null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }
    }
}