package app.stash

data class OfferItem(
    val id: String,
    val providerName: String,
    val logoUrl: String,
    val url: String,
    val openMode: String,
    val currency: String,
    val rating: Double,
    val priority: Int,
    val isFeatured: Boolean,
    val ctaText: String,
    val amount: String,
    val term: String,
    val interestRate: String,
    val processingTime: String,
    val approvalChance: String
)

data class AppDataConfig(
    val country: String = "ZZ",
    val locale: String = "en"
)

data class RemoteConfigResult(
    val taskValidator: Boolean,
    val appData: AppDataConfig,
    val offers: List<OfferItem>
)

data class ExpenseItem(
    val id: Long,
    val title: String,
    val category: String,
    val amount: Double,
    val timestamp: Long
)

data class SavingsGoal(
    val id: Long,
    val name: String,
    val saved: Double,
    val target: Double
)
