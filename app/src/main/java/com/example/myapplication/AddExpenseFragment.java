package com.example.myapplication;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddExpenseFragment extends Fragment {
    private TextInputEditText etAmount, etNote, etDate, etCustomCategory;
    private com.google.android.material.textfield.TextInputLayout tilCustomCategory;
    private MaterialButton btnSave;
    private GridLayout gridCategories;
    private ImageView ivExpenseImage;
    private TextView tvAddImage;
    private MaterialCardView cardImage;
    private Uri selectedImageUri;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private String selectedCategory = "Food";
    private String customCategoryName = ""; // Store custom category name
    private TextView othersCategoryLabel; // Reference to "Others" category label
    private DataManager dataManager;
    private List<String> categoryList = new ArrayList<>();
    private final Map<String, String> iconMap = new HashMap<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                try {
                    requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                selectedImageUri = uri;
                ivExpenseImage.setImageURI(uri);
                ivExpenseImage.setVisibility(View.VISIBLE);
                tvAddImage.setVisibility(View.GONE);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_expense, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dataManager = DataManager.getInstance(requireContext());
        etAmount = view.findViewById(R.id.etAmount);
        etNote = view.findViewById(R.id.etNote);
        etDate = view.findViewById(R.id.etDate);
        etCustomCategory = view.findViewById(R.id.etCustomCategory);
        tilCustomCategory = view.findViewById(R.id.tilCustomCategory);
        btnSave = view.findViewById(R.id.btnSave);
        gridCategories = view.findViewById(R.id.gridCategories);
        ivExpenseImage = view.findViewById(R.id.ivExpenseImage);
        tvAddImage = view.findViewById(R.id.tvAddImage);
        cardImage = view.findViewById(R.id.cardImage);

        cardImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        // Initialize icons
        iconMap.put("Food", "🍔");
        iconMap.put("Transport", "🚗");
        iconMap.put("Shopping", "🛍️");
        iconMap.put("Bills", "📜");
        iconMap.put("Entertainment", "🍿");
        iconMap.put("Others", "✨");

        // Set default date
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        etDate.setText(sdf.format(new Date()));

        // Set up date picker
        etDate.setOnClickListener(v -> showDatePicker());

        // Listen for custom category input changes
        etCustomCategory.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                customCategoryName = s.toString().trim();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        loadCategories();
        btnSave.setOnClickListener(v -> saveExpense());
    }

    private void loadCategories() {
        categoryList = dataManager.getCategories();
        // Ensure selectedCategory is valid
        if (!categoryList.contains(selectedCategory)) {
            if (!categoryList.isEmpty()) {
                selectedCategory = categoryList.get(0);
            }
        }
        setupCategoryGrid();
    }

    private void setupCategoryGrid() {
        gridCategories.removeAllViews();

        for (int i = 0; i < categoryList.size(); i++) {
            final String category = categoryList.get(i);
            MaterialCardView card = new MaterialCardView(requireContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(i % 3, 1f);
            params.setMargins(8, 8, 8, 8);
            card.setLayoutParams(params);
            card.setRadius(24);
            card.setCardElevation(2);

            TextView tvIcon = new TextView(requireContext());
            String icon = iconMap.getOrDefault(category, "🏷️"); // Default icon for custom categories
            tvIcon.setText(icon);
            tvIcon.setTextSize(24);
            tvIcon.setPadding(24, 24, 24, 8);
            tvIcon.setGravity(android.view.Gravity.CENTER);

            TextView tvLabel = new TextView(requireContext());
            tvLabel.setText(category);
            tvLabel.setTextSize(10);
            tvLabel.setGravity(android.view.Gravity.CENTER);
            tvLabel.setPadding(8, 0, 8, 16);
            
            int textColor = getMaterialColor("colorOnSurfaceVariant");
            tvLabel.setTextColor(textColor);

            if (category.equals("Others")) {
                othersCategoryLabel = tvLabel;
            }

            android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            layout.addView(tvIcon);
            layout.addView(tvLabel);
            card.addView(layout);

            card.setOnClickListener(v -> {
                if (category.equals("Others")) {
                    selectedCategory = "Others";
                    tilCustomCategory.setVisibility(View.VISIBLE);
                    etCustomCategory.requestFocus();
                    updateCategorySelection();
                } else {
                    selectedCategory = category;
                    customCategoryName = "";
                    tilCustomCategory.setVisibility(View.GONE);
                    etCustomCategory.setText("");
                    if (othersCategoryLabel != null) {
                        othersCategoryLabel.setText("Others");
                    }
                    updateCategorySelection();
                }
            });

            gridCategories.addView(card);
        }

        // Add "Add Category" button as the last item
        addAddCategoryButton(categoryList.size());
        
        updateCategorySelection();
    }

    private void addAddCategoryButton(int index) {
        MaterialCardView card = new MaterialCardView(requireContext());
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(index % 3, 1f);
        params.setMargins(8, 8, 8, 8);
        card.setLayoutParams(params);
        card.setRadius(24);
        card.setCardElevation(2);
        card.setCardBackgroundColor(getMaterialColor("colorSurface"));

        TextView tvIcon = new TextView(requireContext());
        tvIcon.setText("➕");
        tvIcon.setTextSize(24);
        tvIcon.setPadding(24, 24, 24, 8);
        tvIcon.setGravity(android.view.Gravity.CENTER);

        TextView tvLabel = new TextView(requireContext());
        tvLabel.setText("Add New");
        tvLabel.setTextSize(10);
        tvLabel.setGravity(android.view.Gravity.CENTER);
        tvLabel.setPadding(8, 0, 8, 16);
        tvLabel.setTextColor(getMaterialColor("colorOnSurfaceVariant"));

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.addView(tvIcon);
        layout.addView(tvLabel);
        card.addView(layout);

        card.setOnClickListener(v -> showAddCategoryDialog());

        gridCategories.addView(card);
    }

    private void showAddCategoryDialog() {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_custom_category, null);
        TextInputEditText etName = view.findViewById(R.id.etCustomCategory);
        
        new AlertDialog.Builder(requireContext())
            .setTitle("Add New Category")
            .setView(view)
            .setPositiveButton("Add", (dialog, which) -> {
                String name = etName.getText().toString().trim();
                if (!name.isEmpty()) {
                    if (dataManager.addCategory(name)) {
                        Toast.makeText(requireContext(), "Category added", Toast.LENGTH_SHORT).show();
                        loadCategories(); // Refresh grid
                        // Select the new category
                        selectedCategory = name;
                        updateCategorySelection();
                    } else {
                        Toast.makeText(requireContext(), "Category already exists", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void updateCategorySelection() {
        // Get theme-aware colors using Material3 attributes
        int surfaceColor = getMaterialColor("colorSurface");
        int primaryColor = getMaterialColor("colorPrimary");
        int onSurfaceVariantColor = getMaterialColor("colorOnSurfaceVariant");
        int primaryContainerColor = getMaterialColor("colorPrimaryContainer");
        
        // Loop through all children except the last one (which is the Add button)
        int categoryCount = categoryList.size();
        
        for (int i = 0; i < categoryCount; i++) {
            if (i >= gridCategories.getChildCount()) break; // Safety check
            
            MaterialCardView card = (MaterialCardView) gridCategories.getChildAt(i);
            android.widget.LinearLayout layout = (android.widget.LinearLayout) card.getChildAt(0);
            TextView tvLabel = (TextView) layout.getChildAt(1);
            
            // Check if this is the "Others" category
            boolean isOthersCategory = (tvLabel == othersCategoryLabel);
            
            // Update "Others" label if custom name is set and it's selected
            if (isOthersCategory) {
                if (selectedCategory.equals("Others") && !customCategoryName.isEmpty()) {
                    tvLabel.setText(customCategoryName);
                } else {
                    tvLabel.setText("Others");
                }
            }
            
            // Determine if this category is selected
            boolean isSelected = false;
            String category = categoryList.get(i);
            if (isOthersCategory) {
                isSelected = selectedCategory.equals("Others");
            } else {
                isSelected = category.equals(selectedCategory);
            }
            
            if (isSelected) {
                // Selected category: use primary color background with stroke
                card.setCardBackgroundColor(primaryContainerColor);
                card.setStrokeWidth(4);
                card.setStrokeColor(primaryColor);
                tvLabel.setTextColor(primaryColor);
            } else {
                // Unselected category: use surface color
                card.setCardBackgroundColor(surfaceColor);
                card.setStrokeWidth(0);
                tvLabel.setTextColor(onSurfaceVariantColor);
            }
        }
    }
    
    private int getThemeColor(int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        if (requireContext().getTheme().resolveAttribute(attr, typedValue, true)) {
            if (typedValue.type >= android.util.TypedValue.TYPE_FIRST_COLOR_INT && 
                typedValue.type <= android.util.TypedValue.TYPE_LAST_COLOR_INT) {
                return typedValue.data;
            } else {
                return ContextCompat.getColor(requireContext(), typedValue.resourceId);
            }
        }
        return 0xFF000000;
    }
    
    private int getMaterialColor(String attrName) {
        int attrId = requireContext().getResources().getIdentifier(
            attrName, "attr", requireContext().getPackageName());
        if (attrId == 0) {
            attrId = requireContext().getResources().getIdentifier(
                attrName, "attr", "com.google.android.material");
        }
        if (attrId != 0) {
            return getThemeColor(attrId);
        }
        switch (attrName) {
            case "colorSurface": return getThemeColor(android.R.attr.colorBackground);
            case "colorPrimary": return getThemeColor(android.R.attr.colorPrimary);
            case "colorOnSurfaceVariant": return getThemeColor(android.R.attr.textColorSecondary);
            case "colorPrimaryContainer": return getThemeColor(android.R.attr.colorPrimary);
            default: return 0xFF000000;
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        
        String currentDate = etDate.getText().toString().trim();
        if (!currentDate.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                Date date = sdf.parse(currentDate);
                if (date != null) {
                    calendar.setTime(date);
                }
            } catch (Exception e) {}
        }
        
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            (view, selectedYear, selectedMonth, selectedDay) -> {
                Calendar selectedCalendar = Calendar.getInstance();
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay);
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                etDate.setText(sdf.format(selectedCalendar.getTime()));
            },
            year, month, day
        );
        
        datePickerDialog.show();
    }

    private void showBudgetExceededAlert(String category, DataManager.BudgetCheckResult budgetCheck, double amount, String note, String date) {
        String message = String.format(Locale.getDefault(),
            "Budget Limit Reached!\n\n" +
            "Category: %s\n" +
            "Budget Limit: $%.2f\n" +
            "Current Spent: $%.2f\n" +
            "This Expense: $%.2f\n" +
            "New Total: $%.2f\n\n" +
            "This expense will exceed your budget limit. Do you still want to proceed?",
            category,
            budgetCheck.budgetLimit,
            budgetCheck.currentSpent,
            amount,
            budgetCheck.newTotal
        );

        new AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Budget Limit Exceeded")
            .setMessage(message)
            .setPositiveButton("Save Anyway", (dialog, which) -> {
                performSave(category, amount, note, date);
            })
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }
    
    private void performSave(String category, double amount, String note, String date) {
        long id = dataManager.addExpense(category, amount, note.isEmpty() ? "No note" : note, date.isEmpty() ? "Today" : date, selectedImageUri != null ? selectedImageUri.toString() : null);
        if (id > 0) {
            Toast.makeText(requireContext(), "Expense saved", Toast.LENGTH_SHORT).show();
            etAmount.setText("");
            etNote.setText("");
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            etDate.setText(sdf.format(new Date()));
            selectedCategory = categoryList.get(0); 
            customCategoryName = ""; 
            etCustomCategory.setText(""); 
            tilCustomCategory.setVisibility(View.GONE); 
            
            // Reset image selection
            selectedImageUri = null;
            ivExpenseImage.setImageURI(null);
            ivExpenseImage.setVisibility(View.GONE);
            tvAddImage.setVisibility(View.VISIBLE);
            
            updateCategorySelection();
            
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).bottomNavigation.setSelectedItemId(R.id.nav_home);
            }
        } else {
            Toast.makeText(requireContext(), "Failed to save expense", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveExpense() {
        String amountStr = etAmount.getText().toString().trim();
        String note = etNote.getText().toString().trim();
        String date = etDate.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategory.equals("Others") && customCategoryName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a category name", Toast.LENGTH_SHORT).show();
            etCustomCategory.requestFocus();
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                Toast.makeText(requireContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }

            String categoryToSave = selectedCategory.equals("Others") ? customCategoryName : selectedCategory;
            
            DataManager.BudgetCheckResult budgetCheck = dataManager.checkBudget(categoryToSave, amount);
            if (budgetCheck.exceedsBudget) {
                showBudgetExceededAlert(categoryToSave, budgetCheck, amount, note, date);
                return;
            }
            
            performSave(categoryToSave, amount, note, date);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
        }
    }
}