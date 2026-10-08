package com.qibla.prayertimes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qibla.prayertimes.R
import com.qibla.prayertimes.data.QiblaMath
import com.qibla.prayertimes.model.SHRINES
import com.qibla.prayertimes.model.Shrine
import com.qibla.prayertimes.sensor.rememberDeviceHeading
import com.qibla.prayertimes.ui.theme.*
import com.qibla.prayertimes.viewmodel.QiblaViewModel
import kotlinx.coroutines.launch

/**
 * "Direction & distance to the holy shrines": a swipeable row of shrine pictures (with the name
 * underneath), and below it the same compass as the home screen — but pointing at the chosen
 * shrine instead of the Kaaba. The distance to every shrine from the currently selected city is
 * listed at the bottom.
 */
@Composable
fun ShrineDirectionScreen(viewModel: QiblaViewModel, onBack: () -> Unit) {
    val city by viewModel.selectedCity.collectAsState()
    val pagerState = rememberPagerState(pageCount = { SHRINES.size })
    val scope = rememberCoroutineScope()
    val shrine = SHRINES[pagerState.currentPage]

    val bearing = QiblaMath.bearingBetween(city.lat, city.lon, shrine.lat, shrine.lon).toFloat()
    val distanceKm = QiblaMath.distanceKmBetween(city.lat, city.lon, shrine.lat, shrine.lon)

    val deviceHeading = rememberDeviceHeading()
    val needleAngle = if (deviceHeading != null) (bearing - deviceHeading + 360f) % 360f else bearing
    val dialRotation = if (deviceHeading != null) (360f - deviceHeading) % 360f else 0f
    val isAligned = deviceHeading != null && minOf(needleAngle, 360f - needleAngle) < 6f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NightMid)
            .verticalScroll(rememberScrollState())
            .padding(top = 28.dp, bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = AmberText
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                stringResource(R.string.menu_shrines),
                color = AmberText,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        // Swipeable shrine pictures, each with its name underneath.
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 56.dp),
            pageSpacing = 14.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            ShrinePage(SHRINES[page])
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            SHRINES.indices.forEach { i ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (i == pagerState.currentPage) 9.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (i == pagerState.currentPage) BrassLight else AmberFaint)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.shrine_swipe_hint),
            color = AmberFaint,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(18.dp))

        // Compass pointing at the selected shrine.
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CompassDial(
                bearingDegrees = needleAngle,
                dialSize = 250.dp,
                animationMillis = if (deviceHeading != null) 150 else 700,
                centerLabel = "${"%.1f".format(bearing)}°",
                captionText = if (deviceHeading != null)
                    stringResource(R.string.compass_live_caption)
                else
                    stringResource(R.string.compass_static_caption),
                dialRotationDegrees = dialRotation,
                isAligned = isAligned
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.shrine_distance_line, distanceKm),
            color = AmberText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(R.string.shrine_measured_from, city.name),
            color = AmberMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(22.dp))

        // Distances to every shrine; tapping a row jumps the pager to it.
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                stringResource(R.string.shrine_all_distances),
                color = BrassLight,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            SHRINES.forEachIndexed { index, s ->
                val selected = index == pagerState.currentPage
                val km = QiblaMath.distanceKmBetween(city.lat, city.lon, s.lat, s.lon)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) CardSurface.copy(alpha = 0.18f) else CardSurface)
                        .border(1.dp, if (selected) BrassLight else CardBorder, RoundedCornerShape(12.dp))
                        .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(s.nameRes), color = AmberText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(stringResource(s.cityRes), color = AmberMuted, fontSize = 11.sp)
                    }
                    Text(
                        stringResource(R.string.shrine_distance_km, km),
                        color = if (selected) BrassLight else AmberMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ShrinePage(shrine: Shrine) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ShrinePicture(
            shrine,
            Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(shrine.nameRes),
            color = AmberText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            stringResource(shrine.cityRes),
            color = AmberMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

/** A photo if the app ships one (res/drawable-nodpi/shrine_<id>.png|jpg), else a drawn picture. */
@Composable
private fun ShrinePicture(shrine: Shrine, modifier: Modifier) {
    val context = LocalContext.current
    val photoRes = remember(shrine.id) {
        context.resources.getIdentifier("shrine_${shrine.id}", "drawable", context.packageName)
    }
    if (photoRes != 0) {
        Image(
            painter = painterResource(photoRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier.background(NightDeep)
        )
    } else {
        ShrineIllustration(shrine, modifier)
    }
}

/** Simple night-sky drawing: a domed hall flanked by minarets. */
@Composable
private fun ShrineIllustration(shrine: Shrine, modifier: Modifier) {
    val dome = Color(shrine.domeColor)
    val wall = Color(0xFFE9DCC0)
    val wallShade = Color(0xFFCDBB95)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF0A1830), Color(0xFF1F4268), Color(0xFFD9A15C))
            )
        )
        listOf(0.1f to 0.12f, 0.3f to 0.22f, 0.62f to 0.1f, 0.9f to 0.25f, 0.78f to 0.14f).forEach { (x, y) ->
            drawCircle(Color.White.copy(alpha = 0.8f), radius = 1.6f, center = Offset(w * x, h * y))
        }
        val groundTop = h * 0.84f
        drawRect(Color(0xFF0A1220), topLeft = Offset(0f, groundTop), size = Size(w, h - groundTop))

        // Minarets
        val positions = if (shrine.minarets >= 4) listOf(0.1f, 0.24f, 0.76f, 0.9f) else listOf(0.16f, 0.84f)
        positions.forEachIndexed { i, fx ->
            val tall = if (shrine.minarets >= 4 && (i == 0 || i == 3)) 0.52f else 0.62f
            val mw = w * 0.035f
            val cx = w * fx
            val top = groundTop - h * tall
            drawRect(wall, topLeft = Offset(cx - mw, top), size = Size(mw * 2, groundTop - top))
            drawRect(wallShade, topLeft = Offset(cx, top), size = Size(mw, groundTop - top))
            drawRect(dome, topLeft = Offset(cx - mw * 1.5f, top + h * 0.05f), size = Size(mw * 3f, h * 0.018f))
            drawArc(
                color = dome, startAngle = 180f, sweepAngle = 180f, useCenter = true,
                topLeft = Offset(cx - mw * 1.3f, top - mw * 1.3f), size = Size(mw * 2.6f, mw * 2.6f)
            )
            drawLine(dome, Offset(cx, top - mw * 1.3f), Offset(cx, top - mw * 2.8f), strokeWidth = 2f)
        }

        // Main hall
        val hallW = w * 0.42f
        val hallTop = groundTop - h * 0.22f
        val hallLeft = (w - hallW) / 2
        drawRect(wall, topLeft = Offset(hallLeft, hallTop), size = Size(hallW, groundTop - hallTop))
        drawRect(wallShade, topLeft = Offset(hallLeft + hallW * 0.5f, hallTop), size = Size(hallW * 0.5f, groundTop - hallTop))
        // Door arch
        val doorW = hallW * 0.22f
        drawArc(
            color = Color(0xFF2B1F10), startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(w / 2 - doorW / 2, groundTop - h * 0.14f), size = Size(doorW, doorW)
        )
        drawRect(
            Color(0xFF2B1F10),
            topLeft = Offset(w / 2 - doorW / 2, groundTop - h * 0.14f + doorW / 2),
            size = Size(doorW, h * 0.14f - doorW / 2)
        )
        // Dome, drum and finial
        val domeW = hallW * 0.72f
        val drumH = h * 0.06f
        drawRect(wall, topLeft = Offset(w / 2 - domeW * 0.42f, hallTop - drumH), size = Size(domeW * 0.84f, drumH))
        val domeTop = hallTop - drumH
        drawArc(
            brush = Brush.verticalGradient(listOf(dome, dome.copy(alpha = 0.65f)), startY = domeTop - domeW / 2, endY = domeTop),
            startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(w / 2 - domeW / 2, domeTop - domeW / 2), size = Size(domeW, domeW)
        )
        drawLine(dome, Offset(w / 2, domeTop - domeW / 2), Offset(w / 2, domeTop - domeW / 2 - h * 0.07f), strokeWidth = 3f)
        drawCircle(dome, radius = 4f, center = Offset(w / 2, domeTop - domeW / 2 - h * 0.075f))
    }
}
