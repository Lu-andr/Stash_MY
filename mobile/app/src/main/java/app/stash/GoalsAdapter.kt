package app.stash

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.stash.databinding.ItemGoalBinding
import java.util.Locale

class GoalsAdapter(
    private var items: List<SavingsGoal>,
    private var currency: String,
    private val onTopUp: (SavingsGoal) -> Unit
) : RecyclerView.Adapter<GoalsAdapter.GoalViewHolder>() {

    fun updateData(newItems: List<SavingsGoal>, newCurrency: String) {
        items = newItems
        currency = newCurrency
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val binding = ItemGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class GoalViewHolder(private val b: ItemGoalBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(goal: SavingsGoal) {
            val pct = if (goal.target > 0) ((goal.saved / goal.target) * 100).toInt().coerceIn(0, 100) else 0
            b.tvGoalName.text = goal.name
            b.tvGoalPercent.text = "$pct%"
            b.progressGoal.progress = pct
            b.tvGoalAmounts.text = String.format(
                Locale.US,
                "Saved: %s%.0f of %s%.0f",
                currency,
                goal.saved,
                currency,
                goal.target
            )
            b.btnTopUpGoal.setOnClickListener { onTopUp(goal) }
        }
    }
}
