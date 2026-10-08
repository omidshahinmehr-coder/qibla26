package com.qibla.prayertimes.model

import androidx.annotation.StringRes
import com.qibla.prayertimes.R

/**
 * A holy shrine / mosque the "direction and distance" screen can point to.
 *
 * [id] is also used to look for an optional photo: if a drawable named `shrine_<id>` exists in
 * res/drawable-nodpi (e.g. shrine_najaf.png) it is shown, otherwise a drawn illustration is used.
 */
data class Shrine(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val cityRes: Int,
    val lat: Double,
    val lon: Double,
    /** Illustration style: dome color (ARGB) and number of minarets. */
    val domeColor: Long,
    val minarets: Int
)

private const val GOLD = 0xFFE8B84A
private const val GREEN = 0xFF2E9E6B

/** Coordinates are of the shrine buildings themselves (from Wikipedia / latlong.net). */
val SHRINES: List<Shrine> = listOf(
    Shrine("najaf", R.string.shrine_najaf_name, R.string.shrine_najaf_city, 31.9959, 44.3146, GOLD, 2),
    Shrine("karbala", R.string.shrine_karbala_name, R.string.shrine_karbala_city, 32.6164, 44.0325, GOLD, 2),
    Shrine("kadhimiya", R.string.shrine_kadhimiya_name, R.string.shrine_kadhimiya_city, 33.3800, 44.3381, GOLD, 4),
    Shrine("samarra", R.string.shrine_samarra_name, R.string.shrine_samarra_city, 34.1989, 43.8735, GOLD, 2),
    Shrine("mashhad", R.string.shrine_mashhad_name, R.string.shrine_mashhad_city, 36.2880, 59.6157, GOLD, 2),
    Shrine("nabawi", R.string.shrine_nabawi_name, R.string.shrine_nabawi_city, 24.4670, 39.6109, GREEN, 4),
    Shrine("zaynab", R.string.shrine_zaynab_name, R.string.shrine_zaynab_city, 33.4444, 36.3408, GOLD, 2)
)
