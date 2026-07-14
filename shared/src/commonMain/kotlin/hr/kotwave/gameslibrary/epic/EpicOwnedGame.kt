package hr.kotwave.gameslibrary.epic

/**
 * One Epic game the account owns, resolved from its library entitlement. [offerId] is the store
 * offer the title-bridge picked in the item's namespace — the id IGDB keys Epic games by — and is
 * null for delisted/renamed titles whose offer no longer exists.
 */
data class EpicOwnedGame(
    val catalogItemId: String,
    val namespace: String,
    val title: String,
    val offerId: String?,
) {
    /** Every per-account uid of this entitlement: the stable catalogItemId plus the offerId when bridged. */
    val uids: List<String> get() = listOfNotNull(catalogItemId, offerId)
}
