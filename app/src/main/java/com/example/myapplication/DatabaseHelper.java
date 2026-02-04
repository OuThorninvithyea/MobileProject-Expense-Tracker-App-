package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * DatabaseHelper manages the SQLite database creation and version management.
 * It extends SQLiteOpenHelper to handle database lifecycle events (create, upgrade, open).
 *
 * This class defines the database schema including tables for Users, Expenses, and Budgets.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "expense_tracker.db";
    private static final int DATABASE_VERSION = 5; // Incremented to add image URI

    // Users table
    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "id";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD_HASH = "password_hash";
    private static final String COL_PET_HASH = "pet_hash";

    // Expenses table
    private static final String TABLE_EXPENSES = "expenses";
    private static final String COL_EXPENSE_ID = "id";
    private static final String COL_EXPENSE_USER_ID = "user_id";
    private static final String COL_EXPENSE_CATEGORY = "category";
    private static final String COL_EXPENSE_AMOUNT = "amount";
    private static final String COL_EXPENSE_NOTE = "note";
    private static final String COL_EXPENSE_DATE = "date";
    private static final String COL_EXPENSE_IMAGE_URI = "image_uri";

    // Budgets table
    private static final String TABLE_BUDGETS = "budgets";
    private static final String COL_BUDGET_USER_ID = "user_id";
    private static final String COL_BUDGET_CATEGORY = "category";
    private static final String COL_BUDGET_LIMIT = "limit_amount";

    private Context context;
    
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        Log.d("DatabaseHelper", "DatabaseHelper constructor called");
    }

    /**
     * Called when the database is created for the first time.
     * This is where the creation of tables and the initial population of the tables should happen.
     *
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            Log.d("DatabaseHelper", "Creating database tables...");
            
            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys = ON");
            
            // Create users table
            String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                    COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_USERNAME + " TEXT UNIQUE NOT NULL, " +
                    COL_PASSWORD_HASH + " TEXT NOT NULL, " +
                    COL_PET_HASH + " TEXT NOT NULL)";
            db.execSQL(createUsersTable);
            Log.d("DatabaseHelper", "Users table created");

            // Create expenses table
            String createExpensesTable = "CREATE TABLE " + TABLE_EXPENSES + " (" +
                    COL_EXPENSE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_EXPENSE_USER_ID + " INTEGER NOT NULL, " +
                    COL_EXPENSE_CATEGORY + " TEXT NOT NULL, " +
                    COL_EXPENSE_AMOUNT + " REAL NOT NULL, " +
                    COL_EXPENSE_NOTE + " TEXT, " +
                    COL_EXPENSE_DATE + " TEXT, " +
                    COL_EXPENSE_IMAGE_URI + " TEXT, " +
                    "FOREIGN KEY(" + COL_EXPENSE_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + "))";
            db.execSQL(createExpensesTable);
            Log.d("DatabaseHelper", "Expenses table created");

            // Create budgets table
            String createBudgetsTable = "CREATE TABLE " + TABLE_BUDGETS + " (" +
                    COL_BUDGET_USER_ID + " INTEGER NOT NULL, " +
                    COL_BUDGET_CATEGORY + " TEXT NOT NULL, " +
                    COL_BUDGET_LIMIT + " REAL NOT NULL, " +
                    "PRIMARY KEY(" + COL_BUDGET_USER_ID + ", " + COL_BUDGET_CATEGORY + "), " +
                    "FOREIGN KEY(" + COL_BUDGET_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + "))";
            db.execSQL(createBudgetsTable);
            Log.d("DatabaseHelper", "Budgets table created");
            Log.d("DatabaseHelper", "Database created successfully");
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error creating database: " + e.getMessage(), e);
            e.printStackTrace();
            throw e;
        }
    }
    
    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Enable foreign keys every time database is opened
        db.execSQL("PRAGMA foreign_keys = ON");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("DatabaseHelper", "Upgrading database from version " + oldVersion + " to " + newVersion);
        // Drop all tables and recreate
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUDGETS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
        Log.d("DatabaseHelper", "Database upgrade completed");
    }
    
    // Method to completely reset the database
    /**
     * Completely resets the database by deleting the database file and recreating it.
     * This is a destructive operation used for debugging or "Factory Reset" features.
     *
     * @param context Context needed to delete the database file
     */
    public void resetDatabase(Context context) {
        Log.d("DatabaseHelper", "=== RESETTING DATABASE COMPLETELY ===");
        try {
            // Close any open database connections first
            SQLiteDatabase db = null;
            try {
                // Get writable database to close it properly if open
                db = this.getWritableDatabase();
                if (db != null && db.isOpen()) {
                    // Drop all tables
                    db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUDGETS);
                    db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
                    db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
                    Log.d("DatabaseHelper", "All tables dropped");
                    db.close();
                }
            } catch (Exception e) {
                Log.e("DatabaseHelper", "Error dropping tables: " + e.getMessage());
                if (db != null && db.isOpen()) {
                    db.close();
                }
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error closing database: " + e.getMessage());
        }
        
        // Delete the database file completely
        try {
            boolean deleted = context.deleteDatabase(DATABASE_NAME);
            Log.d("DatabaseHelper", "Database file deleted: " + deleted);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error deleting database file: " + e.getMessage(), e);
        }
        
        // Recreate database by getting a new instance
        try {
            SQLiteDatabase newDb = this.getWritableDatabase();
            if (newDb != null) {
                onCreate(newDb);
                Log.d("DatabaseHelper", "Database recreated successfully");
                newDb.close();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error recreating database: " + e.getMessage(), e);
        }
        
        Log.d("DatabaseHelper", "=== DATABASE RESET COMPLETED ===");
    }

    /**
     * Hashes a password using SHA-256 for secure storage.
     * Never store plain text passwords!
     *
     * @param password The plain text password
     * @return The SHA-256 hash string, or null if error
     */
    private String hashPassword(String password) {
        if (password == null) {
            Log.e("DatabaseHelper", "Hash error: password is null");
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e("DatabaseHelper", "Hash error", e);
            return null;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Hash error: " + e.getMessage(), e);
            return null;
        }
    }

    public long signup(String username, String password, String pet) {
        SQLiteDatabase db = null;
        try {
            // Validate inputs
            if (username == null || username.trim().isEmpty()) {
                Log.e("DatabaseHelper", "Signup failed: Username is null or empty");
                return -1;
            }
            if (password == null || password.isEmpty()) {
                Log.e("DatabaseHelper", "Signup failed: Password is null or empty");
                return -1;
            }
            if (pet == null || pet.trim().isEmpty()) {
                Log.e("DatabaseHelper", "Signup failed: Pet/security answer is null or empty");
                return -1;
            }
            
            db = this.getWritableDatabase();
            if (db == null) {
                Log.e("DatabaseHelper", "Signup failed: Cannot get writable database");
                return -1;
            }
            
            // Verify table exists - SQLiteOpenHelper should have created it, but check anyway
            Cursor checkTable = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{TABLE_USERS});
            boolean tableExists = checkTable != null && checkTable.getCount() > 0;
            if (checkTable != null) checkTable.close();
            
            if (!tableExists) {
                Log.e("DatabaseHelper", "Signup failed: Users table does not exist! Database may be corrupted.");
                Log.e("DatabaseHelper", "Please uninstall and reinstall the app to recreate the database.");
                return -1;
            }
            
            // Trim username for consistency
            String trimmedUsername = username.trim();
            String trimmedPet = pet.trim().toLowerCase();
            
            Log.d("DatabaseHelper", "Signup attempt for username: " + trimmedUsername);
            
            String passwordHash = hashPassword(password);
            String petHash = hashPassword(trimmedPet);
            
            if (passwordHash == null || petHash == null) {
                Log.e("DatabaseHelper", "Signup failed: Hash generation failed - passwordHash: " + (passwordHash != null) + ", petHash: " + (petHash != null));
                return -1;
            }

            // Enable foreign keys for this connection
            db.execSQL("PRAGMA foreign_keys = ON");
            
            ContentValues values = new ContentValues();
            values.put(COL_USERNAME, trimmedUsername);
            values.put(COL_PASSWORD_HASH, passwordHash);
            values.put(COL_PET_HASH, petHash);

            Log.d("DatabaseHelper", "Attempting to insert user: " + trimmedUsername);
            
            // First check if username already exists
            Cursor checkUser = db.query(TABLE_USERS, new String[]{COL_USER_ID}, COL_USERNAME + "=?", new String[]{trimmedUsername}, null, null, null);
            boolean usernameExists = checkUser != null && checkUser.getCount() > 0;
            if (checkUser != null) checkUser.close();
            
            if (usernameExists) {
                Log.e("DatabaseHelper", "Signup failed: Username '" + trimmedUsername + "' already exists");
                return -2; // Return -2 to indicate username exists (different from -1 for other errors)
            }
            
            long id = -1;
            try {
                id = db.insertOrThrow(TABLE_USERS, null, values);
                Log.d("DatabaseHelper", "Signup successful for user: " + trimmedUsername + " with ID: " + id);
            } catch (SQLException e) {
                Log.e("DatabaseHelper", "SQLException during insert: " + e.getMessage(), e);
                // Check if it's a unique constraint violation (username already exists)
                if (e.getMessage() != null && (e.getMessage().contains("UNIQUE constraint") || e.getMessage().contains("unique"))) {
                    Log.e("DatabaseHelper", "Username '" + trimmedUsername + "' already exists (caught in exception)");
                    id = -2; // Username exists
                } else {
                    Log.e("DatabaseHelper", "Database insert failed: " + e.getMessage());
                    id = -1; // Other database error
                }
            } catch (Exception e) {
                Log.e("DatabaseHelper", "Unexpected exception during insert: " + e.getMessage(), e);
                id = -1; // Database error
            }
            
            return id;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Signup exception: " + e.getMessage(), e);
            e.printStackTrace();
            return -1;
        }
    }

    public User login(String username, String password) {
        Cursor cursor = null;
        SQLiteDatabase db = null;
        try {
            // Validate inputs
            if (username == null || username.trim().isEmpty()) {
                Log.e("DatabaseHelper", "Login failed: Username is null or empty");
                return null;
            }
            if (password == null || password.isEmpty()) {
                Log.e("DatabaseHelper", "Login failed: Password is null or empty");
                return null;
            }
            
            db = this.getReadableDatabase();
            if (db == null) {
                Log.e("DatabaseHelper", "Login failed: Cannot get readable database");
                return null;
            }
            
            // Verify table exists
            Cursor checkTable = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{TABLE_USERS});
            boolean tableExists = checkTable != null && checkTable.getCount() > 0;
            if (checkTable != null) checkTable.close();
            
            if (!tableExists) {
                Log.e("DatabaseHelper", "Login failed: Users table does not exist!");
                return null;
            }
            
            // Trim username to match signup behavior
            String trimmedUsername = username.trim();
            Log.d("DatabaseHelper", "Attempting login for user: '" + trimmedUsername + "'");
            Log.d("DatabaseHelper", "Password length: " + (password != null ? password.length() : 0));
            
            String passwordHash = hashPassword(password);
            if (passwordHash == null) {
                Log.e("DatabaseHelper", "Login failed: Hash generation failed");
                return null;
            }
            Log.d("DatabaseHelper", "Password hash generated (length: " + passwordHash.length() + ")");
            
            // First check if username exists
            Cursor userCheck = db.query(TABLE_USERS, 
                    new String[]{COL_USER_ID, COL_USERNAME, COL_PASSWORD_HASH}, 
                    COL_USERNAME + "=?", 
                    new String[]{trimmedUsername}, 
                    null, null, null);
            
            if (userCheck != null && userCheck.getCount() > 0) {
                userCheck.moveToFirst();
                int userId = userCheck.getInt(0);
                String storedUsername = userCheck.getString(1);
                String storedPasswordHash = userCheck.getString(2);
                Log.d("DatabaseHelper", "Username found! User ID: " + userId + ", Username: '" + storedUsername + "'");
                Log.d("DatabaseHelper", "Stored password hash length: " + (storedPasswordHash != null ? storedPasswordHash.length() : 0));
                Log.d("DatabaseHelper", "Input password hash length: " + passwordHash.length());
                
                // Compare hashes
                if (storedPasswordHash != null && storedPasswordHash.equals(passwordHash)) {
                    Log.d("DatabaseHelper", "Password hash matches! Login successful.");
                    User user = new User(userId, storedUsername);
                    userCheck.close();
                    return user;
                } else {
                    Log.e("DatabaseHelper", "Password hash mismatch!");
                    Log.e("DatabaseHelper", "Stored hash: " + (storedPasswordHash != null ? storedPasswordHash.substring(0, Math.min(20, storedPasswordHash.length())) + "..." : "null"));
                    Log.e("DatabaseHelper", "Input hash:  " + passwordHash.substring(0, Math.min(20, passwordHash.length())) + "...");
                    userCheck.close();
                    return null;
                }
            } else {
                if (userCheck != null) userCheck.close();
                Log.e("DatabaseHelper", "Login failed: Username '" + trimmedUsername + "' does not exist in database");
                
                // Debug: List all usernames in database
                Cursor allUsers = db.query(TABLE_USERS, new String[]{COL_USERNAME}, null, null, null, null, null);
                if (allUsers != null) {
                    Log.d("DatabaseHelper", "All usernames in database:");
                    while (allUsers.moveToNext()) {
                        Log.d("DatabaseHelper", "  - '" + allUsers.getString(0) + "'");
                    }
                    allUsers.close();
                }
                return null;
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Login exception: " + e.getMessage(), e);
            e.printStackTrace();
            if (cursor != null) cursor.close();
            return null;
        }
    }

    public boolean updateUsername(int userId, String newUsername) {
        Cursor cursor = null;
        SQLiteDatabase db = null;
        try {
            if (newUsername == null || newUsername.trim().isEmpty()) {
                Log.e("DatabaseHelper", "Update username failed: New username is null or empty");
                return false;
            }
            
            String trimmedUsername = newUsername.trim();
            
            db = this.getWritableDatabase();
            if (db == null) {
                Log.e("DatabaseHelper", "Update username failed: Cannot get writable database");
                return false;
            }
            
            // Check if new username already exists (excluding current user)
            cursor = db.query(TABLE_USERS, new String[]{COL_USER_ID}, 
                    COL_USERNAME + "=? AND " + COL_USER_ID + "!=?", 
                    new String[]{trimmedUsername, String.valueOf(userId)}, null, null, null);
            boolean usernameExists = cursor != null && cursor.getCount() > 0;
            if (cursor != null) cursor.close();
            
            if (usernameExists) {
                Log.e("DatabaseHelper", "Update username failed: Username '" + trimmedUsername + "' already exists");
                return false;
            }
            
            ContentValues values = new ContentValues();
            values.put(COL_USERNAME, trimmedUsername);
            
            int rows = db.update(TABLE_USERS, values, COL_USER_ID + "=?", 
                    new String[]{String.valueOf(userId)});
            
            if (rows > 0) {
                Log.d("DatabaseHelper", "Username updated successfully for user ID: " + userId);
                return true;
            } else {
                Log.e("DatabaseHelper", "Update username failed: No rows affected for user ID " + userId);
                return false;
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Update username exception: " + e.getMessage(), e);
            return false;
        }
    }
    
    public boolean updatePassword(int userId, String currentPassword, String newPassword) {
        Cursor cursor = null;
        SQLiteDatabase db = null;
        try {
            if (newPassword == null || newPassword.length() < 3) {
                Log.e("DatabaseHelper", "Update password failed: New password is too short");
                return false;
            }
            
            if (currentPassword == null || currentPassword.isEmpty()) {
                Log.e("DatabaseHelper", "Update password failed: Current password is required");
                return false;
            }
            
            db = this.getReadableDatabase();
            if (db == null) {
                Log.e("DatabaseHelper", "Update password failed: Cannot get readable database");
                return false;
            }
            
            // Verify current password
            cursor = db.query(TABLE_USERS, new String[]{COL_PASSWORD_HASH}, 
                    COL_USER_ID + "=?", new String[]{String.valueOf(userId)}, null, null, null);
            
            if (cursor == null || !cursor.moveToFirst()) {
                Log.e("DatabaseHelper", "Update password failed: User not found");
                if (cursor != null) cursor.close();
                return false;
            }
            
            String storedHash = cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD_HASH));
            cursor.close();
            
            String currentPasswordHash = hashPassword(currentPassword);
            if (currentPasswordHash == null || !currentPasswordHash.equals(storedHash)) {
                Log.e("DatabaseHelper", "Update password failed: Current password is incorrect");
                return false;
            }
            
            // Update password
            db = this.getWritableDatabase();
            String newPasswordHash = hashPassword(newPassword);
            if (newPasswordHash == null) {
                Log.e("DatabaseHelper", "Update password failed: Hash generation failed");
                return false;
            }
            
            ContentValues values = new ContentValues();
            values.put(COL_PASSWORD_HASH, newPasswordHash);
            
            int rows = db.update(TABLE_USERS, values, COL_USER_ID + "=?", 
                    new String[]{String.valueOf(userId)});
            
            if (rows > 0) {
                Log.d("DatabaseHelper", "Password updated successfully for user ID: " + userId);
                return true;
            } else {
                Log.e("DatabaseHelper", "Update password failed: No rows affected for user ID " + userId);
                return false;
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Update password exception: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean resetPassword(String username, String pet, String newPassword) {
        Cursor cursor = null;
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            // Trim username for consistency
            String trimmedUsername = username != null ? username.trim() : "";
            if (trimmedUsername.isEmpty()) {
                Log.e("DatabaseHelper", "Reset password failed: Username is empty");
                return false;
            }
            
            String petHash = hashPassword(pet != null ? pet.toLowerCase().trim() : "");
            String newPasswordHash = hashPassword(newPassword);
            
            if (petHash == null || newPasswordHash == null) {
                Log.e("DatabaseHelper", "Reset password failed: Hash generation failed");
                return false;
            }

            cursor = db.query(TABLE_USERS,
                    new String[]{COL_USER_ID},
                    COL_USERNAME + "=? AND " + COL_PET_HASH + "=?",
                    new String[]{trimmedUsername, petHash},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                ContentValues values = new ContentValues();
                values.put(COL_PASSWORD_HASH, newPasswordHash);
                int rows = db.update(TABLE_USERS, values, COL_USER_ID + "=?",
                        new String[]{String.valueOf(cursor.getInt(0))});
                cursor.close();
                return rows > 0;
            }
            if (cursor != null) cursor.close();
            Log.e("DatabaseHelper", "Reset password failed: Invalid username or security answer");
            return false;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Reset password exception: " + e.getMessage(), e);
            if (cursor != null) cursor.close();
            return false;
        }
    }

    /**
     * Inserts a new expense into the database.
     *
     * @param userId   The ID of the user owning the expense
     * @param category Expense category
     * @param amount   Expense amount
     * @param note     Optional note
     * @param date     Date of expense
     * @param imageUri Optional receipt image URI
     * @return The row ID of the newly inserted expense, or -1 if an error occurred
     */
    public long addExpense(int userId, String category, double amount, String note, String date, String imageUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_EXPENSE_USER_ID, userId);
        values.put(COL_EXPENSE_CATEGORY, category);
        values.put(COL_EXPENSE_AMOUNT, amount);
        values.put(COL_EXPENSE_NOTE, note);
        values.put(COL_EXPENSE_DATE, date);
        values.put(COL_EXPENSE_IMAGE_URI, imageUri);

        long id = db.insert(TABLE_EXPENSES, null, values);
        db.close();
        return id;
    }

    public String getExpenses(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXPENSES,
                new String[]{COL_EXPENSE_ID, COL_EXPENSE_CATEGORY, COL_EXPENSE_AMOUNT, COL_EXPENSE_NOTE, COL_EXPENSE_DATE, COL_EXPENSE_IMAGE_URI},
                COL_EXPENSE_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null, COL_EXPENSE_ID + " DESC");

        StringBuilder json = new StringBuilder("[");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                if (json.length() > 1) json.append(",");
                json.append("{")
                    .append("\"id\":").append(cursor.getInt(0)).append(",")
                    .append("\"category\":\"").append(cursor.getString(1) != null ? cursor.getString(1) : "").append("\",")
                    .append("\"amount\":").append(cursor.getDouble(2)).append(",")
                    .append("\"note\":\"").append(escapeJson(cursor.isNull(3) ? "" : cursor.getString(3))).append("\",")
                    .append("\"date\":\"").append(escapeJson(cursor.isNull(4) ? "" : cursor.getString(4))).append("\",")
                    .append("\"imageUri\":\"").append(escapeJson(cursor.isNull(5) ? "" : cursor.getString(5))).append("\"")
                    .append("}");
            }
            cursor.close();
        }
        json.append("]");
        db.close();
        return json.toString();
    }

    public boolean updateExpense(int expenseId, String category, double amount, String note, String date, String imageUri) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            if (db == null) {
                Log.e("DatabaseHelper", "Update expense failed: Cannot get writable database");
                return false;
            }
            
            ContentValues values = new ContentValues();
            values.put(COL_EXPENSE_CATEGORY, category);
            values.put(COL_EXPENSE_AMOUNT, amount);
            values.put(COL_EXPENSE_NOTE, note);
            values.put(COL_EXPENSE_DATE, date);
            values.put(COL_EXPENSE_IMAGE_URI, imageUri);

            int rows = db.update(TABLE_EXPENSES, values, COL_EXPENSE_ID + "=?",
                    new String[]{String.valueOf(expenseId)});
            
            if (rows > 0) {
                Log.d("DatabaseHelper", "Expense updated successfully: ID " + expenseId);
                return true;
            } else {
                Log.e("DatabaseHelper", "Update expense failed: No rows affected for ID " + expenseId);
                return false;
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Update expense exception: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean deleteExpense(int expenseId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_EXPENSES, COL_EXPENSE_ID + "=?",
                new String[]{String.valueOf(expenseId)});
        db.close();
        return rows > 0;
    }

    public boolean clearExpenses(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_EXPENSES, COL_EXPENSE_USER_ID + "=?",
                new String[]{String.valueOf(userId)});
        db.close();
        return rows >= 0;
    }

    public boolean setBudget(int userId, String category, double limit) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_BUDGET_USER_ID, userId);
        values.put(COL_BUDGET_CATEGORY, category);
        values.put(COL_BUDGET_LIMIT, limit);

        long id = db.insertWithOnConflict(TABLE_BUDGETS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
        return id > 0;
    }

    public String getBudgets(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_BUDGETS,
                new String[]{COL_BUDGET_CATEGORY, COL_BUDGET_LIMIT},
                COL_BUDGET_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null, null);

        StringBuilder json = new StringBuilder("[");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                if (json.length() > 1) json.append(",");
                json.append("{")
                    .append("\"category\":\"").append(cursor.getString(0)).append("\",")
                    .append("\"limit\":").append(cursor.getDouble(1))
                    .append("}");
            }
            cursor.close();
        }
        json.append("]");
        db.close();
        return json.toString();
    }

    public boolean deleteBudget(int userId, String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_BUDGETS,
                COL_BUDGET_USER_ID + "=? AND " + COL_BUDGET_CATEGORY + "=?",
                new String[]{String.valueOf(userId), category});
        db.close();
        return rows > 0;
    }

    public boolean checkUserExists(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_USERS, new String[]{COL_USER_ID}, 
                    COL_USER_ID + "=?", new String[]{String.valueOf(userId)}, 
                    null, null, null);
            boolean exists = (cursor != null && cursor.getCount() > 0);
            if (cursor != null) cursor.close();
            db.close();
            return exists;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error checking user existence: " + e.getMessage());
            if (cursor != null) cursor.close();
            db.close();
            return false;
        }
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    // Debug method to check database state
    public boolean verifyDatabase() {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            if (db == null) {
                Log.e("DatabaseHelper", "Database verification failed: Cannot get database");
                return false;
            }
            
            // Check if users table exists
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{TABLE_USERS});
                boolean exists = cursor != null && cursor.getCount() > 0;
                
                if (exists) {
                    // Count users
                    Cursor userCount = null;
                    try {
                        userCount = db.query(TABLE_USERS, new String[]{"COUNT(*) as count"}, null, null, null, null, null);
                        int count = 0;
                        if (userCount != null && userCount.moveToFirst()) {
                            count = userCount.getInt(0);
                        }
                        Log.d("DatabaseHelper", "Database verified: Users table exists with " + count + " users");
                        return true;
                    } finally {
                        if (userCount != null) userCount.close();
                    }
                } else {
                    Log.e("DatabaseHelper", "Database verification failed: Users table does not exist");
                    return false;
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Database verification exception: " + e.getMessage(), e);
            return false;
        }
    }

    public static class User {
        public int id;
        public String username;

        public User(int id, String username) {
            this.id = id;
            this.username = username;
        }
    }
}
