package ru.kbzhu.tracker.ui.util

import androidx.annotation.StringRes
import ru.kbzhu.tracker.R
import ru.kbzhu.tracker.domain.model.FoodValidationError
import ru.kbzhu.tracker.domain.model.NetworkError

@get:StringRes
val NetworkError.messageRes: Int
    get() = when (this) {
        NetworkError.NO_CONNECTION -> R.string.error_no_connection
        NetworkError.TIMEOUT -> R.string.error_timeout
        NetworkError.SERVER -> R.string.error_server
    }

@get:StringRes
val FoodValidationError.messageRes: Int
    get() = when (this) {
        FoodValidationError.EMPTY_NAME -> R.string.error_empty_name
        FoodValidationError.INVALID_NUMBERS -> R.string.error_invalid_numbers
        FoodValidationError.MACROS_EXCEED_100G -> R.string.error_macros_exceed
        FoodValidationError.NO_INGREDIENTS -> R.string.error_no_ingredients
        FoodValidationError.INVALID_WEIGHT -> R.string.error_invalid_weight
    }
