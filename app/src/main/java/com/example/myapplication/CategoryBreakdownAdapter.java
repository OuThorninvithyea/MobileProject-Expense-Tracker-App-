package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class CategoryBreakdownAdapter extends RecyclerView.Adapter<CategoryBreakdownAdapter.CategoryBreakdownViewHolder> {
    private List<CategoryBreakdown> breakdowns;

    public CategoryBreakdownAdapter(List<CategoryBreakdown> breakdowns) {
        this.breakdowns = breakdowns;
    }

    @NonNull
    @Override
    public CategoryBreakdownViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_category_breakdown, parent, false);
        return new CategoryBreakdownViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryBreakdownViewHolder holder, int position) {
        CategoryBreakdown breakdown = breakdowns.get(position);
        holder.bind(breakdown);
    }

    @Override
    public int getItemCount() {
        return breakdowns != null ? breakdowns.size() : 0;
    }

    public void updateBreakdowns(List<CategoryBreakdown> newBreakdowns) {
        this.breakdowns = newBreakdowns;
        notifyDataSetChanged();
    }

    class CategoryBreakdownViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCategoryIcon, tvCategoryName, tvCategoryAmount, tvCategoryPercentage;

        public CategoryBreakdownViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryIcon = itemView.findViewById(R.id.tvCategoryIcon);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvCategoryAmount = itemView.findViewById(R.id.tvCategoryAmount);
            tvCategoryPercentage = itemView.findViewById(R.id.tvCategoryPercentage);
        }

        public void bind(CategoryBreakdown breakdown) {
            tvCategoryName.setText(breakdown.category);
            tvCategoryAmount.setText(String.format(Locale.getDefault(), "$%.2f", breakdown.amount));
            tvCategoryPercentage.setText(String.format(Locale.getDefault(), "%.1f%%", breakdown.percentage));
            
            // Set category icon
            String icon = getCategoryIcon(breakdown.category);
            tvCategoryIcon.setText(icon);
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

    public static class CategoryBreakdown {
        String category;
        double amount;
        double percentage;

        CategoryBreakdown(String category, double amount, double percentage) {
            this.category = category;
            this.amount = amount;
            this.percentage = percentage;
        }
    }
}

