package com.example.myapplication;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsFragment extends Fragment {
    private RecyclerView rvCategoryBreakdown;
    private TextView tvTotalExpenses, tvTransactionCount;
    private TextInputEditText etSearch;
    private MaterialButton btnSort;
    private DataManager dataManager;
    private CategoryBreakdownAdapter adapter;
    private List<CategoryBreakdownAdapter.CategoryBreakdown> allBreakdowns;
    private String currentSortType = "amount_desc"; // Default: highest amount first
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analytics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dataManager = DataManager.getInstance(requireContext());
        rvCategoryBreakdown = view.findViewById(R.id.rvCategoryBreakdown);
        tvTotalExpenses = view.findViewById(R.id.tvTotalExpenses);
        tvTransactionCount = view.findViewById(R.id.tvTransactionCount);
        etSearch = view.findViewById(R.id.etSearchAnalytics);
        btnSort = view.findViewById(R.id.btnSortAnalytics);

        // Setup search
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase().trim();
                loadAnalytics();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup sort button
        btnSort.setOnClickListener(v -> showSortMenu());

        loadAnalytics();
    }

    private void loadAnalytics() {
        List<DataManager.Expense> expenses = dataManager.getExpenses();
        
        double total = 0;
        Map<String, Double> categoryTotals = new HashMap<>();
        
        for (DataManager.Expense expense : expenses) {
            total += expense.amount;
            categoryTotals.put(expense.category, 
                categoryTotals.getOrDefault(expense.category, 0.0) + expense.amount);
        }

        tvTotalExpenses.setText(String.format(Locale.getDefault(), "$%.2f", total));
        tvTransactionCount.setText(expenses.size() + " transactions");

        // Create category breakdown list
        allBreakdowns = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            double percentage = total > 0 ? (entry.getValue() / total) * 100 : 0;
            allBreakdowns.add(new CategoryBreakdownAdapter.CategoryBreakdown(entry.getKey(), entry.getValue(), percentage));
        }

        // Filter breakdowns based on search query
        List<CategoryBreakdownAdapter.CategoryBreakdown> filteredBreakdowns = filterBreakdowns(allBreakdowns);
        
        // Sort breakdowns
        List<CategoryBreakdownAdapter.CategoryBreakdown> sortedBreakdowns = sortBreakdowns(filteredBreakdowns);

        // Set up RecyclerView with adapter
        if (adapter == null) {
            adapter = new CategoryBreakdownAdapter(sortedBreakdowns);
            LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
            rvCategoryBreakdown.setLayoutManager(layoutManager);
            rvCategoryBreakdown.setAdapter(adapter);
        } else {
            adapter.updateBreakdowns(sortedBreakdowns);
        }
    }

    private List<CategoryBreakdownAdapter.CategoryBreakdown> filterBreakdowns(List<CategoryBreakdownAdapter.CategoryBreakdown> breakdowns) {
        if (searchQuery.isEmpty()) {
            return new ArrayList<>(breakdowns);
        }

        List<CategoryBreakdownAdapter.CategoryBreakdown> filtered = new ArrayList<>();
        for (CategoryBreakdownAdapter.CategoryBreakdown breakdown : breakdowns) {
            // Search in category name, amount, and percentage
            if (breakdown.category != null && breakdown.category.toLowerCase().contains(searchQuery)) {
                filtered.add(breakdown);
            } else if (String.format(Locale.getDefault(), "%.2f", breakdown.amount).contains(searchQuery)) {
                filtered.add(breakdown);
            } else if (String.format(Locale.getDefault(), "%.1f", breakdown.percentage).contains(searchQuery)) {
                filtered.add(breakdown);
            }
        }
        return filtered;
    }

    private List<CategoryBreakdownAdapter.CategoryBreakdown> sortBreakdowns(List<CategoryBreakdownAdapter.CategoryBreakdown> breakdowns) {
        List<CategoryBreakdownAdapter.CategoryBreakdown> sorted = new ArrayList<>(breakdowns);
        
        switch (currentSortType) {
            case "amount_desc":
                Collections.sort(sorted, (a, b) -> Double.compare(b.amount, a.amount)); // Highest first
                break;
            case "amount_asc":
                Collections.sort(sorted, (a, b) -> Double.compare(a.amount, b.amount)); // Lowest first
                break;
            case "name_asc":
                Collections.sort(sorted, (a, b) -> {
                    String c1 = a.category != null ? a.category : "";
                    String c2 = b.category != null ? b.category : "";
                    return c1.compareToIgnoreCase(c2);
                });
                break;
            case "name_desc":
                Collections.sort(sorted, (a, b) -> {
                    String c1 = a.category != null ? a.category : "";
                    String c2 = b.category != null ? b.category : "";
                    return c2.compareToIgnoreCase(c1);
                });
                break;
            case "percentage_desc":
                Collections.sort(sorted, (a, b) -> Double.compare(b.percentage, a.percentage)); // Highest first
                break;
            case "percentage_asc":
                Collections.sort(sorted, (a, b) -> Double.compare(a.percentage, b.percentage)); // Lowest first
                break;
        }
        
        return sorted;
    }

    private void showSortMenu() {
        PopupMenu popupMenu = new PopupMenu(requireContext(), btnSort);
        popupMenu.getMenu().add("Amount (High to Low)");
        popupMenu.getMenu().add("Amount (Low to High)");
        popupMenu.getMenu().add("Category (A-Z)");
        popupMenu.getMenu().add("Category (Z-A)");
        popupMenu.getMenu().add("Percentage (High to Low)");
        popupMenu.getMenu().add("Percentage (Low to High)");
        
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String title = item.getTitle().toString();
                if (title.equals("Amount (High to Low)")) {
                    currentSortType = "amount_desc";
                } else if (title.equals("Amount (Low to High)")) {
                    currentSortType = "amount_asc";
                } else if (title.equals("Category (A-Z)")) {
                    currentSortType = "name_asc";
                } else if (title.equals("Category (Z-A)")) {
                    currentSortType = "name_desc";
                } else if (title.equals("Percentage (High to Low)")) {
                    currentSortType = "percentage_desc";
                } else if (title.equals("Percentage (Low to High)")) {
                    currentSortType = "percentage_asc";
                }
                loadAnalytics();
                return true;
            }
        });
        
        popupMenu.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAnalytics();
    }
}
