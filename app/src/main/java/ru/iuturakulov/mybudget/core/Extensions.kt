package ru.iuturakulov.mybudget.core

object Extensions {

    inline fun Boolean.ifTrue(action: () -> Unit): Boolean {
        if (this) action()
        return this
    }

    inline fun Boolean.ifFalse(action: () -> Unit): Boolean {
        if (!this) action()
        return this
    }

    inline fun Boolean.ifElse(ifTrue: () -> Unit, ifFalse: () -> Unit) {
        if (this) ifTrue() else ifFalse()
    }

    inline fun <T> T.ifCondition(condition: Boolean, action: (T) -> Unit): T {
        if (condition) action(this)
        return this
    }

    inline fun <T> Boolean.whenTrue(value: T): T? = if (this) value else null

    inline fun <T> Boolean.whenFalse(value: T): T? = if (!this) value else null

    inline fun <T> T?.ifNotNull(action: (T) -> Unit): T? {
        this?.let(action)
        return this
    }
}