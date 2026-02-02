package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {
    private List<BudgetItem> budgets;
    private OnBudgetClickListener listener;

    public interface OnBudgetClickListener {
        void onEditClick(DataManager.Budget budget);
        void onDeleteClick(DataManager.Budget budget);
    }

    public BudgetAdapter(List<BudgetItem> budgets, OnBudgetClickListener listener) {
        this.budgets = budgets;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        BudgetItem budgetItem = budgets.get(position);
        holder.bind(budgetItem);
    }

    @Override
    public int getItemCount() {
        return budgets.size();
    }

    public void updateBudgets(List<BudgetItem> newBudgets) {
        this.budgets = newBudgets;
        notifyDataSetChanged();
    }

    class BudgetViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCategory, tvCategoryIcon, tvSpent, tvLimit, tvWarning;
        private ProgressBar progressBar;
        private android.widget.ImageButton btnMenu;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvBudgetCategory);
            tvCategoryIcon = itemView.findViewById(R.id.tvBudgetCategoryIcon);
            tvSpent = itemView.findViewById(R.id.tvBudgetSpent);
            tvLimit = itemView.findViewById(R.id.tvBudgetLimit);
            tvWarning = itemView.findViewById(R.id.tvBudgetWarning);
            progressBar = itemView.findViewById(R.id.progressBudget);
            btnMenu = itemView.findViewById(R.id.btnMenuBudget);
        }

        public void bind(BudgetItem budgetItem) {
            DataManager.Budget budget = budgetItem.budget;
            double spent = budgetItem.spent;
            double limit = budget.limit;
            double percentage = limit > 0 ? (spent / limit) * 100 : 0;
            
            tvCategory.setText(budget.category);
            tvCategoryIcon.setText(getCategoryIcon(budget.category));
            tvSpent.setText(String.format(Locale.getDefault(), "$%.2f", spent));
            tvLimit.setText(String.format(Locale.getDefault(), "/ $%.2f", limit));
            
            // Set progress bar
            int progress = (int) Math.min(percentage, 100);
            progressBar.setProgress(progress);
            
            // Set progress bar color based on percentage
            if (percentage >= 100) {
                int redColor = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_dark);
                progressBar.getProgressDrawable().setColorFilter(
                    redColor,
                    android.graphics.PorterDuff.Mode.SRC_IN
                );
                tvWarning.setText("🚨 Budget Exceeded!");
                tvWarning.setVisibility(View.VISIBLE);
                tvWarning.setTextColor(redColor);
            } else if (percentage >= 80) {
                int orangeColor = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_orange_dark);
                progressBar.getProgressDrawable().setColorFilter(
                    orangeColor,
                    android.graphics.PorterDuff.Mode.SRC_IN
                );
                tvWarning.setText(String.format(Locale.getDefault(), "⚠️ %.0f%% of limit reached", percentage));
                tvWarning.setVisibility(View.VISIBLE);
                tvWarning.setTextColor(orangeColor);
            } else {
                int blueColor = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_blue_dark);
                progressBar.getProgressDrawable().setColorFilter(
                    blueColor,
                    android.graphics.PorterDuff.Mode.SRC_IN
                );
                tvWarning.setVisibility(View.GONE);
            }

            // Setup 3-dot menu button
            btnMenu.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                popupMenu.getMenu().add("Edit");
                popupMenu.getMenu().add("Delete");
                
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (listener != null) {
                            if (item.getTitle().toString().equals("Edit")) {
                                listener.onEditClick(budget);
                            } else if (item.getTitle().toString().equals("Delete")) {
                                listener.onDeleteClick(budget);
                            }
                        }
                        return true;
                    }
                });
                
                popupMenu.show();
            });
        }

        private String getCategoryIcon(String category) {
            switch (category) {
                case "Food": return "🍔";
                case "Transport": return "🚗";
                case "Shopping": return "🛍️";
                case "Bills": return "📜";
                case "Entertainment": return "🍿";
                case "Others": return "✨";
                default: return "📦";
            }
        }
    }

    public static class BudgetItem {
        public DataManager.Budget budget;
        public double spent;

        public BudgetItem(DataManager.Budget budget, double spent) {
            this.budget = budget;
            this.spent = spent;
        }
    }
}
