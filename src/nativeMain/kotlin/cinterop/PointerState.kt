package cinterop

enum class PointerState {
    IMMUTABLY_BORROWED, MUTABLY_BORROWED, ACTIVE, FREED, RELEASED
}