package ru.iuturakulov.mybudget.domain.mappers

object CategoryIconMapper {
    private val categoryToIconMap = mapOf(
        "Еда" to "\uD83C\uDF55",
        "Транспорт" to "\uD83D\uDE97",
        "Развлечения" to "\uD83C\uDF89",
        "Прочее" to "\uD83D\uDE0A"
    )

//    <string-array name="emoji_list">
//    <item>😊</item>
//    <item>🚗</item>
//    <item>🍕</item>
//    <item>🎉</item>
//    <item>💵</item>
//    <item>💳</item>
//    <item>📈</item>
//    <item>💰</item>
//    <item>🛒</item>
//    <item>🍔</item>
//    <item>🎮</item>
//    <item>✈️</item>
//    <item>🏡</item>
//    </string-array>

    fun getIconForCategory(category: String): String {
        return categoryToIconMap[category] ?: "ic_default"
    }
}
