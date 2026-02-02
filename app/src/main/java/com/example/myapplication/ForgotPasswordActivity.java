package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ForgotPasswordActivity extends AppCompatActivity {
    private TextInputEditText etUsername, etPet, etNewPassword;
    private TextView tvError;
    private MaterialButton btnReset;
    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Load dark mode preference
        SharedPreferences prefs = getSharedPreferences("AppSettings", 0);
        int darkMode = prefs.getInt("dark_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(darkMode);
        
        setContentView(R.layout.activity_forgot_password);

        dataManager = DataManager.getInstance(this);

        etUsername = findViewById(R.id.etUsername);
        etPet = findViewById(R.id.etPet);
        etNewPassword = findViewById(R.id.etNewPassword);
        tvError = findViewById(R.id.tvError);
        btnReset = findViewById(R.id.btnReset);

        btnReset.setOnClickListener(v -> handleReset());

        findViewById(R.id.tvBackToLogin).setOnClickListener(v -> {
            finish();
        });
    }

    private void handleReset() {
        String username = etUsername.getText().toString().trim();
        String pet = etPet.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString();

        if (username.isEmpty() || pet.isEmpty() || newPassword.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        boolean success = dataManager.resetPassword(username, pet, newPassword);
        if (success) {
            Toast.makeText(this, "Password reset successful. Please log in.", Toast.LENGTH_LONG).show();
            finish();
        } else {
            showError("Invalid username or security answer");
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
}
