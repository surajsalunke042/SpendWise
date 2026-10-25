package com.surajdev.spendwise.core.common

import kotlinx.datetime.TimeZone
import kotlin.uuid.Uuid

/** Creates globally unique IDs on the device, so records can be created offline and synced later. */
fun interface IdGenerator {
    fun newId(): String
}

class UuidIdGenerator : IdGenerator {
    override fun newId(): String = Uuid.random().toString()
}

/** The device's current time zone. Abstracted so tests can pin it. */
fun interface TimeZoneProvider {
    fun current(): TimeZone
}

class SystemTimeZoneProvider : TimeZoneProvider {
    override fun current(): TimeZone = TimeZone.currentSystemDefault()
}
