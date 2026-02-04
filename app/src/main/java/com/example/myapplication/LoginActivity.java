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

/**
 * LoginActivity handles user authentication.
 * It serves as the entry point of the application (LAUNCHER activity).
 *
 * Responsibilities:
 * 1. Checks if a user is already logged in.
 * 2. If yes, redirects to MainActivity.
 * 3. If no, provides UI for logging in or navigating to Signup/Forgot Password.
 */
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

        // Initialize DataManager singleton
        dataManager = DataManager.getInstance(this);

        // Check persistent login state using SharedPreferences info stored in DataManager
        // If a user is currently logged in, skip the login screen and go directly to the dashboard
        if (dataManager.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish(); // Finish LoginActivity so user can't go back to it with 'Back' button
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

    /**
     * Handles the login button click.
     * Validates input and delegates authentication to DataManager.
     */
    private void handleLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();

        // Basic validation
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        // Attempt login via DataManager
        DataManager.LoginResult result = dataManager.login(username, password);
        if (result.success) {
            // Navigate to main app
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            // Show error message
            showError(result.error);
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
}
