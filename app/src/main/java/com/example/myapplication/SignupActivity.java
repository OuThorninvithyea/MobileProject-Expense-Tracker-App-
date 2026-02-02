package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class SignupActivity extends AppCompatActivity {
    private TextInputEditText etUsername, etPassword, etPet;
    private TextView tvError;
    private MaterialButton btnSignup;
    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Load dark mode preference
        SharedPreferences prefs = getSharedPreferences("AppSettings", 0);
        int darkMode = prefs.getInt("dark_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(darkMode);
        
        setContentView(R.layout.activity_signup);

        dataManager = DataManager.getInstance(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etPet = findViewById(R.id.etPet);
        tvError = findViewById(R.id.tvError);
        btnSignup = findViewById(R.id.btnSignup);

        btnSignup.setOnClickListener(v -> handleSignup());

        findViewById(R.id.tvBackToLogin).setOnClickListener(v -> {
            finish();
        });
    }

    private void handleSignup() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();
        String pet = etPet.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty() || pet.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        DataManager.SignupResult result = dataManager.signup(username, password, pet);
        if (result.success) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            showError(result.error);
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
}
