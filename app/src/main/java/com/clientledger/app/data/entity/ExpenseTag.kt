package com.clientledger.app.data.entity

enum class ExpenseTag(val displayName: String) {
    TAXI("Такси"),
    RENT("Аренда"),
    PAINT("Краска"),
    TOOLS("Инструменты"),
    SUPPLIES("Расходники"),
    TRANSPORT("Проезд"),
    MEAL("Обед");
    
    companion object {
        fun fromDisplayName(name: String): ExpenseTag? {
            return values().find { it.displayName == name }
        }
    }
}
