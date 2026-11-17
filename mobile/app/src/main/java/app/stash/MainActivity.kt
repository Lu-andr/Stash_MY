package app.stash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import app.stash.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private var expenses = mutableListOf<ExpenseItem>()
    private lateinit var expensesAdapter: ExpensesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        setupSafeTracker()
        loadRemoteConfig()

        b.swipeRefresh.setOnRefreshListener {
            loadRemoteConfig()
        }
    }

    private fun loadRemoteConfig() {
        b.progressLoading.visibility = View.VISIBLE
        RemoteConfigRepository.fetchConfig { result ->
            b.progressLoading.visibility = View.GONE
            b.swipeRefresh.isRefreshing = false

            if (result.taskValidator && result.offers.isNotEmpty()) {
                showOffersShowcase(result)
            } else {
                showSafeTracker()
            }
        }
    }

    private fun showOffersShowcase(result: RemoteConfigResult) {
        b.layoutSafeTracker.visibility = View.GONE
        b.layoutOffersShowcase.visibility = View.VISIBLE

        val loc = result.appData.locale
        b.tvShowcaseTitle.text = when (loc) {
            "ru", "kk" -> "Персональные предложения"
            "vi" -> "Đề xuất tài chính"
            "uz" -> "Shaxsiy takliflar"
            "fil" -> "Mga Alok para sa Iyo"
            else -> "Financial Offers"
        }
        b.tvShowcaseSubtitle.text = when (loc) {
            "ru", "kk" -> "Одобрение онлайн за 15 минут • ${result.offers.size} предложений"
            "vi" -> "Duyệt trực tuyến trong 15 phút • ${result.offers.size} đề xuất"
            "uz" -> "15 daqiqada onlayn tasdiqlash • ${result.offers.size} taklif"
            "fil" -> "Online approval sa 15 min • ${result.offers.size} alok"
            else -> "Fast online approval • ${result.offers.size} offers available"
        }

        b.rvOffers.layoutManager = LinearLayoutManager(this)
        b.rvOffers.adapter = OffersAdapter(result.offers, loc) { offer ->
            openOffer(offer)
        }
    }

    private fun openOffer(offer: OfferItem) {
        if (offer.url.isBlank()) return
        if (offer.openMode.equals("external", ignoreCase = true)) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(offer.url)))
            } catch (_: Exception) {
                Toast.makeText(this, "Cannot open link", Toast.LENGTH_SHORT).show()
            }
        } else {
            val intent = Intent(this, OfferWebViewActivity::class.java).apply {
                putExtra(OfferWebViewActivity.EXTRA_URL, offer.url)
                putExtra(OfferWebViewActivity.EXTRA_TITLE, offer.providerName)
            }
            startActivity(intent)
        }
    }

    private fun showSafeTracker() {
        b.layoutOffersShowcase.visibility = View.GONE
        b.layoutSafeTracker.visibility = View.VISIBLE
        refreshTrackerSummary()
    }

    private fun setupSafeTracker() {
        expenses = ExpenseStorage.getExpenses(this)
        expensesAdapter = ExpensesAdapter(expenses) { item ->
            expenses.removeAll { it.id == item.id }
            ExpenseStorage.saveExpenses(this, expenses)
            expensesAdapter.updateData(expenses)
            refreshTrackerSummary()
        }
        b.rvExpenses.layoutManager = LinearLayoutManager(this)
        b.rvExpenses.adapter = expensesAdapter

        b.btnAddExpense.setOnClickListener {
            showAddExpenseDialog()
        }

        b.cardBudgetSummary.setOnClickListener {
            showEditBudgetDialog()
        }

        refreshTrackerSummary()
    }

    private fun refreshTrackerSummary() {
        val budget = ExpenseStorage.getBudget(this)
        val spent = expenses.sumOf { it.amount }
        val remaining = (budget - spent).coerceAtLeast(0.0)

        b.tvBalanceRemaining.text = String.format(Locale.US, "$%.2f", remaining)
        b.tvBudgetStats.text = String.format(
            Locale.US,
            "Spent: $%.2f  •  Monthly Budget: $%.2f (tap to edit)",
            spent,
            budget
        )
        val progress = if (budget > 0) ((spent / budget) * 100).toInt().coerceIn(0, 100) else 0
        b.progressBudget.progress = progress
    }

    private fun showAddExpenseDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
        }
        val etTitle = EditText(this).apply { hint = "Expense title (e.g. Coffee)" }
        val etCategory = EditText(this).apply { hint = "Category (e.g. Food, Bills)" }
        val etAmount = EditText(this).apply {
            hint = "Amount (e.g. 12.50)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        container.addView(etTitle)
        container.addView(etCategory)
        container.addView(etAmount)

        AlertDialog.Builder(this)
            .setTitle("New Expense")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val title = etTitle.text.toString().trim().ifBlank { "General Expense" }
                val cat = etCategory.text.toString().trim().ifBlank { "General" }
                val amount = etAmount.text.toString().trim().toDoubleOrNull() ?: 0.0
                if (amount > 0) {
                    expenses.add(0, ExpenseItem(System.currentTimeMillis(), title, cat, amount, System.currentTimeMillis()))
                    ExpenseStorage.saveExpenses(this, expenses)
                    expensesAdapter.updateData(expenses)
                    refreshTrackerSummary()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditBudgetDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
        }
        val etBudget = EditText(this).apply {
            hint = "Monthly budget"
            setText(ExpenseStorage.getBudget(this@MainActivity).toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        container.addView(etBudget)

        AlertDialog.Builder(this)
            .setTitle("Monthly Budget")
            .setView(container)
            .setPositiveButton("Update") { _, _ ->
                val newBudget = etBudget.text.toString().trim().toDoubleOrNull()
                if (newBudget != null && newBudget > 0) {
                    ExpenseStorage.setBudget(this, newBudget)
                    refreshTrackerSummary()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
