package app.stash

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.stash.databinding.ItemExpenseBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpensesAdapter(
    private var items: List<ExpenseItem>,
    private var currency: String,
    private val onDelete: (ExpenseItem) -> Unit
) : RecyclerView.Adapter<ExpensesAdapter.ExpenseViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

    fun updateData(newItems: List<ExpenseItem>, newCurrency: String) {
        items = newItems
        currency = newCurrency
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ExpenseViewHolder(private val b: ItemExpenseBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: ExpenseItem) {
            b.tvExpenseTitle.text = item.title
            b.tvExpenseMeta.text = "${item.category} • ${dateFormat.format(Date(item.timestamp))}"
            b.tvExpenseAmount.text = String.format(Locale.US, "-%s%.2f", currency, item.amount)
            b.btnDeleteExpense.setOnClickListener { onDelete(item) }
        }
    }
}
