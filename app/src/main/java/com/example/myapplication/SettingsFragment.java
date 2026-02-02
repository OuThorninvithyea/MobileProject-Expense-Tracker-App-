package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsFragment extends Fragment {
    private TextView tvUsername, tvUserInitial;
    private MaterialButton btnLogout;
    private View btnClearData, btnEditProfile;
    private SwitchMaterial switchDarkMode;
    private DataManager dataManager;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_DARK_MODE = "dark_mode";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dataManager = DataManager.getInstance(requireContext());
        prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
        
        tvUsername = view.findViewById(R.id.tvUsername);
        tvUserInitial = view.findViewById(R.id.tvUserInitial);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        btnClearData = view.findViewById(R.id.btnClearData);
        
        // Load and set dark mode switch state
        loadDarkModeState();
        
        // Set switch listener
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleDarkMode(isChecked);
        });

        LinearLayout layoutDarkMode = view.findViewById(R.id.layoutDarkMode);
        layoutDarkMode.setOnClickListener(v -> {
             switchDarkMode.setChecked(!switchDarkMode.isChecked());
        });

        DatabaseHelper.User user = dataManager.getCurrentUser();
        if (user != null) {
            tvUsername.setText("@" + user.username);
            tvUserInitial.setText(user.username.substring(0, 1).toUpperCase());
        }

        btnLogout.setOnClickListener(v -> {
            dataManager.logout();
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
        });

        btnEditProfile.setOnClickListener(v -> {
            showEditProfileDialog();
        });

        btnClearData.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                .setTitle("Clear Data")
                .setMessage("Clear all expenses? This cannot be undone.")
                .setPositiveButton("Clear", (dialog, which) -> {
                    if (dataManager.clearExpenses()) {
                        Toast.makeText(requireContext(), "All expenses cleared", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    private void loadDarkModeState() {
        // Read from SharedPreferences to get the saved preference
        int savedMode = prefs.getInt(KEY_DARK_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        boolean isDarkMode = (savedMode == AppCompatDelegate.MODE_NIGHT_YES);
        
        // Set switch state without triggering listener
        switchDarkMode.setOnCheckedChangeListener(null);
        switchDarkMode.setChecked(isDarkMode);
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleDarkMode(isChecked);
        });
    }

    private void toggleDarkMode(boolean enable) {
        int newMode = enable ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        
        // Save preference
        prefs.edit().putInt(KEY_DARK_MODE, newMode).apply();
        
        // Apply the new mode
        AppCompatDelegate.setDefaultNightMode(newMode);
        
        // Recreate activity to apply theme changes immediately
        if (getActivity() != null) {
            getActivity().recreate();
        }
    }
    
    private void showEditProfileDialog() {
        DatabaseHelper.User user = dataManager.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null);
        TextInputEditText etNewUsername = dialogView.findViewById(R.id.etNewUsername);
        TextInputEditText etCurrentPassword = dialogView.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        TextView tvError = dialogView.findViewById(R.id.tvError);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        // Pre-fill current username
        etNewUsername.setText(user.username);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newUsername = etNewUsername.getText() != null ? etNewUsername.getText().toString().trim() : "";
            String currentPassword = etCurrentPassword.getText() != null ? etCurrentPassword.getText().toString() : "";
            String newPassword = etNewPassword.getText() != null ? etNewPassword.getText().toString() : "";

            tvError.setVisibility(View.GONE);

            // Validate username
            if (newUsername.isEmpty()) {
                tvError.setText("Username cannot be empty");
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            boolean usernameChanged = !newUsername.equals(user.username);
            boolean passwordChanged = !newPassword.isEmpty();

            // If password is being changed, current password is required
            if (passwordChanged && currentPassword.isEmpty()) {
                tvError.setText("Current password is required to change password");
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            // If password is being changed, validate new password length
            if (passwordChanged && newPassword.length() < 3) {
                tvError.setText("New password must be at least 3 characters");
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            // Update username if changed
            if (usernameChanged) {
                if (!dataManager.updateUsername(newUsername)) {
                    tvError.setText("Username already exists or update failed");
                    tvError.setVisibility(View.VISIBLE);
                    return;
                }
                // Update UI
                tvUsername.setText("@" + newUsername);
                tvUserInitial.setText(newUsername.substring(0, 1).toUpperCase());
                Toast.makeText(requireContext(), "Username updated successfully", Toast.LENGTH_SHORT).show();
            }

            // Update password if changed
            if (passwordChanged) {
                if (!dataManager.updatePassword(currentPassword, newPassword)) {
                    tvError.setText("Current password is incorrect or update failed");
                    tvError.setVisibility(View.VISIBLE);
                    return;
                }
                Toast.makeText(requireContext(), "Password updated successfully", Toast.LENGTH_SHORT).show();
            }

            if (!usernameChanged && !passwordChanged) {
                Toast.makeText(requireContext(), "No changes made", Toast.LENGTH_SHORT).show();
            } else if (usernameChanged && passwordChanged) {
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
            }

            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update switch state when fragment resumes (in case dark mode was changed elsewhere)
        loadDarkModeState();
        
        // Refresh user info in case it was updated
        DatabaseHelper.User user = dataManager.getCurrentUser();
        if (user != null) {
            tvUsername.setText("@" + user.username);
            tvUserInitial.setText(user.username.substring(0, 1).toUpperCase());
        }
    }
}
