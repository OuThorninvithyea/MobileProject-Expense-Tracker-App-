package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * DataManager serves as the central data access layer (Repository Pattern) for the application.
 * It manages interactions between the UI controllers (Activities/Fragments) and the underlying data sources
 * (SQLite Database and SharedPreferences).
 *
 * It uses the Singleton pattern to ensure only one instance exists throughout the app lifecycle.
 */
public class DataManager {
    private static DataManager instance;
    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;
    private Context context;

    private DataManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("ExpenseTracker", Context.MODE_PRIVATE);
        
        // Initialize database helper
        try {
            this.dbHelper = new DatabaseHelper(context);
            android.util.Log.d("DataManager", "DatabaseHelper initialized");
        } catch (Exception e) {
            android.util.Log.e("DataManager", "Error initializing DatabaseHelper: " + e.getMessage(), e);
            // Try to recover by deleting and recreating
            try {
                context.deleteDatabase("expense_tracker.db");
                this.dbHelper = new DatabaseHelper(context);
            } catch (Exception e2) {
                android.util.Log.e("DataManager", "Failed to recover database: " + e2.getMessage(), e2);
            }
        }
    }
    
    // Public method to reset database
    public void resetDatabase() {
        android.util.Log.d("DataManager", "Resetting database via DataManager...");
        // Clear SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        
        // Reset database
        dbHelper.resetDatabase(context);
        
        android.util.Log.d("DataManager", "Database reset completed");
    }

    /**
     * Public method to get the singleton instance of DataManager.
     * Uses double-checked locking (if synchronized) or simple null check here for thread safety context.
     *
     * @param context Application context needed for database and prefs initialization
     * @return The single instance of DataManager
     */
    public static synchronized DataManager getInstance(Context context) {
        if (instance == null) {
            instance = new DataManager(context.getApplicationContext());
        }
        return instance;
    }

    // Authentication methods
    /**
     * Authenticates a user with the provided credentials.
     *
     * @param username The username input
     * @param password The password input
     * @return LoginResult containing success status, user object, or error message
     */
    public LoginResult login(String username, String password) {
        android.util.Log.d("DataManager", "Login attempt for: " + username);
        DatabaseHelper.User user = dbHelper.login(username, password);
        if (user != null) {
            // Save login session to SharedPreferences to keep user logged in
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("userId", user.id);
            editor.putString("username", user.username);
            editor.apply();
            android.util.Log.d("DataManager", "Login success, saving user to prefs");
            return new LoginResult(true, user, null);
        }
        android.util.Log.e("DataManager", "Login failed for: " + username);
        return new LoginResult(false, null, "Invalid username or password");
    }

    public SignupResult signup(String username, String password, String pet) {
        android.util.Log.d("DataManager", "Signup attempt for: " + username);
        if (username == null || username.trim().isEmpty()) {
            android.util.Log.e("DataManager", "Signup failed: Username is required");
            return new SignupResult(false, null, "Username is required");
        }
        if (password == null || password.length() < 3) {
            android.util.Log.e("DataManager", "Signup failed: Password too short");
            return new SignupResult(false, null, "Password must be at least 3 characters");
        }
        if (pet == null || pet.trim().isEmpty()) {
            android.util.Log.e("DataManager", "Signup failed: Security answer required");
            return new SignupResult(false, null, "Security answer is required");
        }

        long userId = dbHelper.signup(username, password, pet);
        if (userId > 0) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("userId", (int) userId);
            editor.putString("username", username.trim());
            editor.apply();
            android.util.Log.d("DataManager", "Signup success, user ID: " + userId);
            DatabaseHelper.User user = new DatabaseHelper.User((int) userId, username.trim());
            return new SignupResult(true, user, null);
        }
        android.util.Log.e("DataManager", "Signup failed: Database returned userId: " + userId);
        // Check if it's a duplicate username or other error
        if (userId == -2) {
            return new SignupResult(false, null, "Username already exists. Please choose a different username.");
        } else if (userId == -1) {
            return new SignupResult(false, null, "Database error occurred. Please try again.");
        }
        return new SignupResult(false, null, "Signup failed. Please try again.");
    }

    public boolean resetPassword(String username, String pet, String newPassword) {
        if (newPassword == null || newPassword.length() < 3) {
            return false;
        }
        return dbHelper.resetPassword(username, pet, newPassword);
    }
    
    public boolean updateUsername(String newUsername) {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return false;
        
        if (newUsername == null || newUsername.trim().isEmpty()) {
            return false;
        }
        
        boolean success = dbHelper.updateUsername(userId, newUsername);
        if (success) {
            // Update SharedPreferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("username", newUsername.trim());
            editor.apply();
        }
        return success;
    }
    
    public boolean updatePassword(String currentPassword, String newPassword) {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return false;
        
        if (newPassword == null || newPassword.length() < 3) {
            return false;
        }
        
        return dbHelper.updatePassword(userId, currentPassword, newPassword);
    }

    public DatabaseHelper.User getCurrentUser() {
        int userId = prefs.getInt("userId", -1);
        String username = prefs.getString("username", null);
        
        if (userId > 0 && username != null) {
            // Verify user actually exists in DB (in case of DB reset)
            if (dbHelper.checkUserExists(userId)) {
                return new DatabaseHelper.User(userId, username);
            } else {
                // User in prefs but not in DB - likely DB was reset
                android.util.Log.w("DataManager", "User found in prefs but not in DB. Logging out.");
                logout(); // Clear invalid prefs
                return null;
            }
        }
        return null;
    }

    public void logout() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("userId");
        editor.remove("username");
        editor.apply();
    }

    // Expense methods
    /**
     * Adds a new expense record for the currently logged-in user.
     *
     * @param category Expense category (e.g., Food, Transport)
     * @param amount   Monetary value
     * @param note     Optional description
     * @param date     Date string
     * @param imageUri Optional URI for receipt image
     * @return The ID of the new expense, or -1 if failed
     */
    public long addExpense(String category, double amount, String note, String date, String imageUri) {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return -1;
        return dbHelper.addExpense(userId, category, amount, note, date, imageUri);
    }

    /**
     * Retrieves all expenses for the current user.
     * Parses the JSON string returned by DatabaseHelper into a List of Expense objects.
     *
     * @return List of Expense objects, or empty list if none found or error
     */
    public List<Expense> getExpenses() {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return new ArrayList<>();
        
        try {
            // DatabaseHelper returns data as a JSON string to decouple implementation
            String json = dbHelper.getExpenses(userId);
            JSONArray jsonArray = new JSONArray(json);
            List<Expense> expenses = new ArrayList<>();
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                Expense expense = new Expense(
                    obj.getInt("id"),
                    obj.getString("category"),
                    obj.getDouble("amount"),
                    obj.getString("note"),
                    obj.getString("date"),
                    obj.optString("imageUri", "")
                );
                expenses.add(expense);
            }
            return expenses;
        } catch (JSONException e) {
            return new ArrayList<>();
        }
    }

    public boolean updateExpense(int expenseId, String category, double amount, String note, String date, String imageUri) {
        return dbHelper.updateExpense(expenseId, category, amount, note, date, imageUri);
    }

    public boolean deleteExpense(int expenseId) {
        return dbHelper.deleteExpense(expenseId);
    }

    public boolean clearExpenses() {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return false;
        return dbHelper.clearExpenses(userId);
    }

    // Budget methods
    public boolean setBudget(String category, double limit) {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return false;
        return dbHelper.setBudget(userId, category, limit);
    }

    public List<Budget> getBudgets() {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return new ArrayList<>();
        
        try {
            String json = dbHelper.getBudgets(userId);
            JSONArray jsonArray = new JSONArray(json);
            List<Budget> budgets = new ArrayList<>();
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                Budget budget = new Budget(
                    obj.getString("category"),
                    obj.getDouble("limit")
                );
                budgets.add(budget);
            }
            return budgets;
        } catch (JSONException e) {
            return new ArrayList<>();
        }
    }

    public boolean deleteBudget(String category) {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return false;
        return dbHelper.deleteBudget(userId, category);
    }

    // Category methods
    public List<String> getCategories() {
        int userId = prefs.getInt("userId", -1);
        List<String> categories = new ArrayList<>();
        
        // Default categories
        String[] defaults = {"Food", "Transport", "Shopping", "Bills", "Entertainment", "Others"};
        
        if (userId <= 0) {
            for (String c : defaults) categories.add(c);
            return categories;
        }

        String json = prefs.getString("categories_" + userId, null);
        if (json == null) {
            for (String c : defaults) categories.add(c);
            return categories;
        }

        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                categories.add(jsonArray.getString(i));
            }
        } catch (JSONException e) {
            for (String c : defaults) categories.add(c);
        }
        return categories;
    }

    public boolean addCategory(String category) {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return false;
        
        List<String> categories = getCategories();
        if (categories.contains(category)) return false;
        
        // Insert before "Others" if it exists, otherwise at end
        int othersIndex = categories.indexOf("Others");
        if (othersIndex != -1) {
            categories.add(othersIndex, category);
        } else {
            categories.add(category);
        }
        
        return saveCategories(userId, categories);
    }
    
    private boolean saveCategories(int userId, List<String> categories) {
        JSONArray jsonArray = new JSONArray();
        for (String c : categories) {
            jsonArray.put(c);
        }
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("categories_" + userId, jsonArray.toString());
        editor.apply();
        return true;
    }

    // Check if adding an expense would exceed the budget
    /**
     * Checks if adding a new expense amount would exceed the set budget for that category.
     *
     * @param category The category to check
     * @param amount   The amount of the new expense
     * @return BudgetCheckResult containing calculation details and whether budget is exceeded
     */
    public BudgetCheckResult checkBudget(String category, double amount) {
        List<Budget> budgets = getBudgets();
        
        // Find budget for this category
        DataManager.Budget budget = null;
        for (DataManager.Budget b : budgets) {
            if (b.category.equals(category)) {
                budget = b;
                break;
            }
        }
        
        // If no budget set for this category, no check needed
        if (budget == null) {
            return new BudgetCheckResult(false, 0, 0, 0);
        }
        
        // Calculate current total spent for this category
        List<Expense> expenses = getExpenses();
        double totalSpent = 0;
        for (Expense expense : expenses) {
            if (expense.category.equals(category)) {
                totalSpent += expense.amount;
            }
        }
        
        // Calculate new total if this expense is added
        double newTotal = totalSpent + amount;
        boolean exceedsBudget = newTotal >= budget.limit;
        
        return new BudgetCheckResult(exceedsBudget, budget.limit, totalSpent, newTotal);
    }

    // Check budget when updating an expense (need expense ID to exclude it from calculation)
    public BudgetCheckResult checkBudgetOnUpdate(String category, double newAmount, int expenseId) {
        List<Budget> budgets = getBudgets();
        
        // Find budget for this category
        DataManager.Budget budget = null;
        for (DataManager.Budget b : budgets) {
            if (b.category.equals(category)) {
                budget = b;
                break;
            }
        }
        
        // If no budget set for this category, no check needed
        if (budget == null) {
            return new BudgetCheckResult(false, 0, 0, 0);
        }
        
        // Calculate current total spent for this category (excluding the expense being updated)
        List<Expense> expenses = getExpenses();
        double totalSpent = 0;
        for (Expense expense : expenses) {
            if (expense.category.equals(category) && expense.id != expenseId) {
                totalSpent += expense.amount;
            }
        }
        
        // Add new amount
        double newTotal = totalSpent + newAmount;
        boolean exceedsBudget = newTotal >= budget.limit;
        
        return new BudgetCheckResult(exceedsBudget, budget.limit, totalSpent, newTotal);
    }

    // Result classes
    public static class LoginResult {
        public boolean success;
        public DatabaseHelper.User user;
        public String error;

        public LoginResult(boolean success, DatabaseHelper.User user, String error) {
            this.success = success;
            this.user = user;
            this.error = error;
        }
    }

    public static class SignupResult {
        public boolean success;
        public DatabaseHelper.User user;
        public String error;

        public SignupResult(boolean success, DatabaseHelper.User user, String error) {
            this.success = success;
            this.user = user;
            this.error = error;
        }
    }

    // Data classes
    public static class Expense {
        public int id;
        public String category;
        public double amount;
        public String note;
        public String date;
        public String imageUri;

        public Expense(int id, String category, double amount, String note, String date, String imageUri) {
            this.id = id;
            this.category = category;
            this.amount = amount;
            this.note = note;
            this.date = date;
            this.imageUri = imageUri;
        }
    }

    public static class Budget {
        public String category;
        public double limit;

        public Budget(String category, double limit) {
            this.category = category;
            this.limit = limit;
        }
    }

    public static class BudgetCheckResult {
        public boolean exceedsBudget;
        public double budgetLimit;
        public double currentSpent;
        public double newTotal;

        public BudgetCheckResult(boolean exceedsBudget, double budgetLimit, double currentSpent, double newTotal) {
            this.exceedsBudget = exceedsBudget;
            this.budgetLimit = budgetLimit;
            this.currentSpent = currentSpent;
            this.newTotal = newTotal;
        }
    }
}
