package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText etUsername, etPassword;
    private TextView tvError;
    private MaterialButton btnLogin;
    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Load dark mode preference
        SharedPreferences prefs = getSharedPreferences("AppSettings", 0);
        int darkMode = prefs.getInt("dark_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(darkMode);
        
        setContentView(R.layout.activity_login);

        dataManager = DataManager.getInstance(this);

        // Check if user is already logged in
        if (dataManager.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        tvError = findViewById(R.id.tvError);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> handleLogin());

        findViewById(R.id.tvSignup).setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
        });

        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
        
        // Add long-press on logo/title to reset database (for debugging)
        // Long press on "Welcome Back" text to reset database
        View welcomeText = findViewById(android.R.id.title);
        if (welcomeText == null) {
            // Try to find any view to attach long press
            View rootView = findViewById(android.R.id.content);
            if (rootView != null) {
                rootView.setOnLongClickListener(v -> {
                    resetDatabase();
                    return true;
                });
            }
        }
    }
    
    private void resetDatabase() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reset Database")
            .setMessage("This will delete ALL data including all users and expenses. This cannot be undone. Are you sure?")
            .setPositiveButton("Reset", (dialog, which) -> {
                dataManager.resetDatabase();
                Toast.makeText(this, "Database reset. Please restart the app.", Toast.LENGTH_LONG).show();
                finish();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void handleLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        DataManager.LoginResult result = dataManager.login(username, password);
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
