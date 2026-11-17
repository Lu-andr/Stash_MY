package app.stash

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.stash.databinding.ItemOfferBinding
import coil.load
import java.util.Locale

class OffersAdapter(
    private var items: List<OfferItem>,
    private val localeCode: String,
    private val onOfferClick: (OfferItem) -> Unit
) : RecyclerView.Adapter<OffersAdapter.OfferViewHolder>() {

    fun updateData(newItems: List<OfferItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferViewHolder {
        val binding = ItemOfferBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OfferViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OfferViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class OfferViewHolder(private val b: ItemOfferBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(offer: OfferItem) {
            b.tvProviderName.text = offer.providerName
            b.tvRating.text = String.format(Locale.US, "★ %.1f", offer.rating)

            b.tvBadgeFeatured.visibility = if (offer.isFeatured) View.VISIBLE else View.GONE
            b.tvBadgeFeatured.text = when (localeCode) {
                "ru", "kk" -> "ТОП"
                "vi" -> "NỔI BẬT"
                "uz" -> "TOP"
                else -> "TOP"
            }

            val labels = getLabels(localeCode)
            b.tvLabelAmount.text = labels.amount
            b.tvValueAmount.text = offer.amount.ifBlank { "—" }

            b.tvLabelRate.text = labels.rate
            b.tvValueRate.text = offer.interestRate.ifBlank { "—" }

            b.tvLabelTerm.text = labels.term
            b.tvValueTerm.text = offer.term.ifBlank { "—" }

            val extraParts = mutableListOf<String>()
            if (offer.processingTime.isNotBlank()) extraParts.add("⏱ ${offer.processingTime}")
            if (offer.approvalChance.isNotBlank()) extraParts.add("✓ ${offer.approvalChance}")
            b.tvExtraMeta.text = extraParts.joinToString("   •   ")
            b.tvExtraMeta.visibility = if (extraParts.isEmpty()) View.GONE else View.VISIBLE

            b.btnApply.text = offer.ctaText

            val initials = offer.providerName.trim().take(2).uppercase(Locale.US)
            b.tvLogoFallback.text = initials

            if (offer.logoUrl.isNotBlank()) {
                b.ivLogo.visibility = View.VISIBLE
                b.tvLogoFallback.visibility = View.GONE
                b.ivLogo.load(offer.logoUrl) {
                    crossfade(true)
                    listener(
                        onError = { _, _ ->
                            b.ivLogo.visibility = View.GONE
                            b.tvLogoFallback.visibility = View.VISIBLE
                        }
                    )
                }
            } else {
                b.ivLogo.visibility = View.GONE
                b.tvLogoFallback.visibility = View.VISIBLE
            }

            b.btnApply.setOnClickListener { onOfferClick(offer) }
            b.root.setOnClickListener { onOfferClick(offer) }
        }
    }

    private data class ParamLabels(val amount: String, val rate: String, val term: String)

    private fun getLabels(locale: String): ParamLabels {
        return when (locale) {
            "ru", "kk" -> ParamLabels("Сумма", "Ставка", "Срок")
            "vi" -> ParamLabels("Số tiền", "Lãi suất", "Thời hạn")
            "uz" -> ParamLabels("Miqdor", "Stavka", "Muddat")
            "fil" -> ParamLabels("Halaga", "Interes", "Termino")
            else -> ParamLabels("Amount", "Rate", "Term")
        }
    }
}
