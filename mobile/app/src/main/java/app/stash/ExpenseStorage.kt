package app.stash

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ExpenseStorage {
    private const val PREFS_NAME = "stash_safe_tracker"
    private const val KEY_ITEMS = "expense_items"
    private const val KEY_BUDGET = "monthly_budget"

    fun getBudget(context: Context): Double {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_BUDGET, 2500f).toDouble()
    }

    fun setBudget(context: Context, budget: Double) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_BUDGET, budget.toFloat()).apply()
    }

    fun getExpenses(context: Context): MutableList<ExpenseItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ITEMS, null)
        if (raw.isNullOrBlank()) {
            val defaults = mutableListOf(
                ExpenseItem(1L, "Groceries & Market", "Food", 64.50, System.currentTimeMillis() - 86400000L),
                ExpenseItem(2L, "City Transit Pass", "Transport", 28.00, System.currentTimeMillis() - 172800000L),
                ExpenseItem(3L, "Cloud & Internet", "Bills", 45.00, System.currentTimeMillis() - 259200000L)
            )
            saveExpenses(context, defaults)
            return defaults
        }
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<ExpenseItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    ExpenseItem(
                        id = o.getLong("id"),
                        title = o.getString("title"),
                        category = o.getString("category"),
                        amount = o.getDouble("amount"),
                        timestamp = o.getLong("timestamp")
                    )
                )
            }
            list
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun saveExpenses(context: Context, list: List<ExpenseItem>) {
        val arr = JSONArray()
        for (item in list) {
            val o = JSONObject()
            o.put("id", item.id)
            o.put("title", item.title)
            o.put("category", item.category)
            o.put("amount", item.amount)
            o.put("timestamp", item.timestamp)
            arr.put(o)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ITEMS, arr.toString()).apply()
    }
}
