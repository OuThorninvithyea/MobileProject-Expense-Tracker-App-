package com.example.myapplication;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * HomeFragment displays the main dashboard of the application.
 * It shows a list of expenses and the total amount spent.
 *
 * Responsibilities:
 * 1. Loads expenses from DataManager.
 * 2. Provides search/filtering functionality.
 * 3. Provides sorting options (Date, Amount, Category).
 * 4. Handles clicks to edit or delete expenses.
 */
public class HomeFragment extends Fragment {
    private RecyclerView rvExpenses;
    private TextView tvTotalAmount;
    private ExpenseAdapter adapter;
    private DataManager dataManager;
    private TextInputEditText etSearch;
    private MaterialButton btnSort;
    private List<DataManager.Expense> allExpenses;
    private String currentSortType = "date_desc"; // Default: newest first
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize DataManager with context
        dataManager = DataManager.getInstance(requireContext());
        
        // Setup Views
        rvExpenses = view.findViewById(R.id.rvExpenses);
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount);
        etSearch = view.findViewById(R.id.etSearch);
        btnSort = view.findViewById(R.id.btnSort);

        // Setup search: Listen for text changes to filter list in real-time
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase().trim();
                loadExpenses(); // Reload list with new filter
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup sort button to show popup menu
        btnSort.setOnClickListener(v -> showSortMenu());

        // Initialize RecyclerView Adapter with empty list and click listeners
        adapter = new ExpenseAdapter(new ArrayList<>(), new ExpenseAdapter.OnExpenseClickListener() {
            @Override
            public void onEditClick(DataManager.Expense expense) {
                showEditDialog(expense);
            }

            @Override
            public void onDeleteClick(DataManager.Expense expense) {
                // Show confirmation before deletion
                new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Expense")
                    .setMessage("Are you sure to delete it?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        if (dataManager.deleteExpense(expense.id)) {
                            loadExpenses(); // Refresh list after delete
                            Toast.makeText(requireContext(), "Expense deleted", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            }
        });

        rvExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvExpenses.setAdapter(adapter);

        // Initial load of data
        loadExpenses();
    }

    /**
     * Loads, filters, sorts, and displays expenses.
     * Also calculates and updates the total amount.
     */
    private void loadExpenses() {
        allExpenses = dataManager.getExpenses();
        
        // Filter expenses based on search query
        List<DataManager.Expense> filteredExpenses = filterExpenses(allExpenses);
        
        // Sort expenses based on current sort criteria
        List<DataManager.Expense> sortedExpenses = sortExpenses(filteredExpenses);
        
        // Update adapter to refresh UI
        adapter.updateExpenses(sortedExpenses);

        // Calculate total amount from the currently displayed (filtered) list
        double total = 0;
        for (DataManager.Expense expense : sortedExpenses) {
            total += expense.amount;
        }
        tvTotalAmount.setText(String.format(Locale.getDefault(), "$%.2f", total));
    }

    private List<DataManager.Expense> filterExpenses(List<DataManager.Expense> expenses) {
        if (searchQuery.isEmpty()) {
            return new ArrayList<>(expenses);
        }

        List<DataManager.Expense> filtered = new ArrayList<>();
        for (DataManager.Expense expense : expenses) {
            // Search in note, category, amount, and date
            if (expense.note != null && expense.note.toLowerCase().contains(searchQuery)) {
                filtered.add(expense);
            } else if (expense.category != null && expense.category.toLowerCase().contains(searchQuery)) {
                filtered.add(expense);
            } else if (String.format(Locale.getDefault(), "%.2f", expense.amount).contains(searchQuery)) {
                filtered.add(expense);
            } else if (expense.date != null && expense.date.toLowerCase().contains(searchQuery)) {
                filtered.add(expense);
            }
        }
        return filtered;
    }

    private List<DataManager.Expense> sortExpenses(List<DataManager.Expense> expenses) {
        List<DataManager.Expense> sorted = new ArrayList<>(expenses);
        
        switch (currentSortType) {
            case "date_desc":
                Collections.sort(sorted, (e1, e2) -> {
                    Date d1 = parseDate(e1.date);
                    Date d2 = parseDate(e2.date);
                    if (d1 == null && d2 == null) return 0;
                    if (d1 == null) return 1;
                    if (d2 == null) return -1;
                    return d2.compareTo(d1); // Newest first
                });
                break;
            case "date_asc":
                Collections.sort(sorted, (e1, e2) -> {
                    Date d1 = parseDate(e1.date);
                    Date d2 = parseDate(e2.date);
                    if (d1 == null && d2 == null) return 0;
                    if (d1 == null) return 1;
                    if (d2 == null) return -1;
                    return d1.compareTo(d2); // Oldest first
                });
                break;
            case "amount_desc":
                Collections.sort(sorted, (e1, e2) -> Double.compare(e2.amount, e1.amount)); // Highest first
                break;
            case "amount_asc":
                Collections.sort(sorted, (e1, e2) -> Double.compare(e1.amount, e2.amount)); // Lowest first
                break;
            case "category_asc":
                Collections.sort(sorted, (e1, e2) -> {
                    String c1 = e1.category != null ? e1.category : "";
                    String c2 = e2.category != null ? e2.category : "";
                    return c1.compareToIgnoreCase(c2);
                });
                break;
            case "category_desc":
                Collections.sort(sorted, (e1, e2) -> {
                    String c1 = e1.category != null ? e1.category : "";
                    String c2 = e2.category != null ? e2.category : "";
                    return c2.compareToIgnoreCase(c1);
                });
                break;
        }
        
        return sorted;
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || dateStr.equals("Today")) {
            return new Date(); // Return current date for "Today"
        }
        
        SimpleDateFormat[] formats = {
            new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()),
            new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()),
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        };
        
        for (SimpleDateFormat format : formats) {
            try {
                return format.parse(dateStr);
            } catch (ParseException e) {
                // Try next format
            }
        }
        return null;
    }

    private void showSortMenu() {
        PopupMenu popupMenu = new PopupMenu(requireContext(), btnSort);
        popupMenu.getMenu().add("Date (Newest First)");
        popupMenu.getMenu().add("Date (Oldest First)");
        popupMenu.getMenu().add("Amount (High to Low)");
        popupMenu.getMenu().add("Amount (Low to High)");
        popupMenu.getMenu().add("Category (A-Z)");
        popupMenu.getMenu().add("Category (Z-A)");
        
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String title = item.getTitle().toString();
                if (title.equals("Date (Newest First)")) {
                    currentSortType = "date_desc";
                } else if (title.equals("Date (Oldest First)")) {
                    currentSortType = "date_asc";
                } else if (title.equals("Amount (High to Low)")) {
                    currentSortType = "amount_desc";
                } else if (title.equals("Amount (Low to High)")) {
                    currentSortType = "amount_asc";
                } else if (title.equals("Category (A-Z)")) {
                    currentSortType = "category_asc";
                } else if (title.equals("Category (Z-A)")) {
                    currentSortType = "category_desc";
                }
                loadExpenses();
                return true;
            }
        });
        
        popupMenu.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadExpenses();
    }

    private void showEditDialog(DataManager.Expense expense) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_expense, null);
        
        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        TextInputEditText etNote = dialogView.findViewById(R.id.etNote);
        TextInputEditText etDate = dialogView.findViewById(R.id.etDate);
        GridLayout gridCategories = dialogView.findViewById(R.id.gridCategories);
        
        // Pre-fill with existing values
        etAmount.setText(String.valueOf(expense.amount));
        etNote.setText(expense.note);
        etDate.setText(expense.date);
        
        // Set up date picker
        etDate.setOnClickListener(v -> showDatePickerDialog(etDate, expense.date));
        
        String[] categories = {"Food", "Transport", "Shopping", "Bills", "Entertainment", "Others"};
        String[] categoryIcons = {"🍔", "🚗", "🛍️", "📜", "🍿", "✨"};
        String[] selectedCategory = {expense.category};
        
        // Setup category grid
        for (int i = 0; i < categories.length; i++) {
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
            tvIcon.setText(categoryIcons[i]);
            tvIcon.setTextSize(24);
            tvIcon.setPadding(24, 24, 24, 8);
            tvIcon.setGravity(android.view.Gravity.CENTER);

            TextView tvLabel = new TextView(requireContext());
            tvLabel.setText(categories[i]);
            tvLabel.setTextSize(10);
            tvLabel.setGravity(android.view.Gravity.CENTER);
            tvLabel.setPadding(8, 0, 8, 16);
            tvLabel.setTextColor(requireContext().getColor(android.R.color.darker_gray));

            android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            layout.addView(tvIcon);
            layout.addView(tvLabel);
            card.addView(layout);

            final String category = categories[i];
            card.setOnClickListener(v -> {
                selectedCategory[0] = category;
                updateCategorySelection(gridCategories, categories, selectedCategory[0]);
            });

            gridCategories.addView(card);
        }
        updateCategorySelection(gridCategories, categories, selectedCategory[0]);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
            .setTitle("Edit Expense")
            .setView(dialogView)
            .setPositiveButton("Save", (d, w) -> {
                String amountStr = etAmount.getText().toString().trim();
                String note = etNote.getText().toString().trim();
                String date = etDate.getText().toString().trim();

                if (amountStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    double amount = Double.parseDouble(amountStr);
                    if (amount <= 0) {
                        Toast.makeText(requireContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Check budget before updating (only if category changed or amount changed)
                    if (!selectedCategory[0].equals(expense.category) || amount != expense.amount) {
                        DataManager.BudgetCheckResult budgetCheck;
                        if (!selectedCategory[0].equals(expense.category)) {
                            // Category changed, check new category budget
                            budgetCheck = dataManager.checkBudget(selectedCategory[0], amount);
                        } else {
                            // Same category, check with expense ID to exclude it from calculation
                            budgetCheck = dataManager.checkBudgetOnUpdate(selectedCategory[0], amount, expense.id);
                        }
                        
                        if (budgetCheck.exceedsBudget) {
                            showBudgetExceededAlert(selectedCategory[0], budgetCheck, amount, expense, note, date);
                            return;
                        }
                    }

                    if (dataManager.updateExpense(expense.id, selectedCategory[0], amount, 
                            note.isEmpty() ? "No note" : note, date.isEmpty() ? "Today" : date, expense.imageUri)) {
                        loadExpenses();
                        Toast.makeText(requireContext(), "Expense updated", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Failed to update expense", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .create();

        dialog.show();
    }

    private void showDatePickerDialog(TextInputEditText etDate, String currentDateStr) {
        Calendar calendar = Calendar.getInstance();
        
        // Try to parse existing date if available
        if (currentDateStr != null && !currentDateStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                Date date = sdf.parse(currentDateStr);
                if (date != null) {
                    calendar.setTime(date);
                }
            } catch (Exception e) {
                // If parsing fails, use current date
            }
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

    private void showBudgetExceededAlert(String category, DataManager.BudgetCheckResult budgetCheck, double amount, DataManager.Expense expense, String note, String date) {
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
            .setPositiveButton("Update Anyway", (dialog, which) -> {
                // User chose to update despite exceeding budget
                if (dataManager.updateExpense(expense.id, category, amount, 
                        note.isEmpty() ? "No note" : note, date.isEmpty() ? "Today" : date, expense.imageUri)) {
                    loadExpenses();
                    Toast.makeText(requireContext(), "Expense updated", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Failed to update expense", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }

    private void updateCategorySelection(GridLayout gridCategories, String[] categories, String selected) {
        for (int i = 0; i < gridCategories.getChildCount(); i++) {
            MaterialCardView card = (MaterialCardView) gridCategories.getChildAt(i);
            TextView tvLabel = (TextView) ((android.widget.LinearLayout) card.getChildAt(0)).getChildAt(1);
            if (tvLabel.getText().toString().equals(selected)) {
                card.setCardBackgroundColor(requireContext().getColor(android.R.color.white));
                card.setStrokeWidth(4);
                card.setStrokeColor(requireContext().getColor(android.R.color.holo_blue_dark));
            } else {
                card.setCardBackgroundColor(requireContext().getColor(android.R.color.white));
                card.setStrokeWidth(0);
            }
        }
    }
}
