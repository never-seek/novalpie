package com.novalpie.nativeapp

/** Phones and emulator handsets stay portrait-first; larger tablets may follow system rotation. */
internal const val NOVALPIE_TABLET_SMALLEST_WIDTH_DP = 600

internal fun novalPieShouldLockPortrait(smallestScreenWidthDp: Int): Boolean =
    // MuMu's OPPO handset profile reports exactly 600dp while still being used as a portrait
    // phone. Treat the boundary as handset-sized so a stale system landscape rotation cannot
    // turn the mobile catalogue and reader into their tablet layout.
    smallestScreenWidthDp <= NOVALPIE_TABLET_SMALLEST_WIDTH_DP
