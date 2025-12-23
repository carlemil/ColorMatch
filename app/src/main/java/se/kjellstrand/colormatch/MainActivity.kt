package se.kjellstrand.colormatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import se.kjellstrand.colormatch.ui.theme.ColorMatchTheme
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ColorMatchTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ImageList(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ImageList(modifier: Modifier = Modifier) {
        val imageCount = 5
        var imageSeeds by remember { mutableStateOf((1..imageCount).toList()) }
        val baseColors = listOf(
            Color(0xFFC1CFD4), // extended-palette-01
            Color(0xFFA3ACB0), // extended-palette-02
            Color(0xFF8CDDFF), // extended-palette-03
            Color(0xFF0096D2), // extended-palette-04
            Color(0xFF005980), // extended-palette-05
            Color(0xFFD9ABFF), // extended-palette-06
            Color(0xFF6B3D99), // extended-palette-07
            Color(0xFFFFA6DA), // extended-palette-08
            Color(0xFFE52F7E), // extended-palette-09
            Color(0xFFFFAE66), // extended-palette-10
            Color(0xFFFFAA00), // extended-palette-11
            Color(0xFFF26A2F), // extended-palette-12
            Color(0xFFCCB100), // extended-palette-13
            Color(0xFFC7D900), // extended-palette-14
            Color(0xFF7AB51D), // extended-palette-15
            Color(0xFF508020), // extended-palette-16
            Color(0xFF95E5BB), // extended-palette-17
            Color(0xFF009985), // extended-palette-18
            Color(0xFF006658), // extended-palette-19
            Color(0xFFD7C7A2), // extended-palette-20
            Color(0xFFC3B289)  // extended-palette-21)
        )
        var isRefreshing by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    imageSeeds = (1..imageCount).map { (0..10000).random() }
                }
            },
            modifier = modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(imageSeeds) { seed ->

                    val allColors = baseColors
                    var colorsPickedFromImage by remember(seed) { mutableStateOf(listOf<Color>()) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        DrawColorListWithTitle(baseColors, "Available IKEA Colors")

                        val bestMatches = remember(colorsPickedFromImage, allColors) {
                            sortOptionsWithDistances(colorsPickedFromImage, allColors)
                        }

                        if (!bestMatches.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ColorBlob(color = bestMatches.first().first)
                            }
                        }

                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data("https://picsum.photos/seed/$seed/400/300")
                                .allowHardware(false)
                                .build(),
                            contentDescription = null,
                            onSuccess = { state ->
                                val bitmap = state.result.drawable.toBitmap()
                                Palette.from(bitmap).generate { palette ->
                                    // Extract a list of distinct swatches
                                    val swatches = listOfNotNull(
                                        palette?.vibrantSwatch,
                                        palette?.lightVibrantSwatch,
                                        palette?.darkVibrantSwatch,
                                        palette?.mutedSwatch,
                                        palette?.lightMutedSwatch,
                                        palette?.darkMutedSwatch
                                    ).map { Color(it.rgb) }.distinct()

                                    colorsPickedFromImage = swatches
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )

                        Row(modifier = Modifier.fillMaxWidth()) {
                            colorsPickedFromImage.forEach { color ->
                                ColorBlob(
                                    color = color,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DrawColorListWithTitle(baseColors: List<Color>, title: String) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            baseColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color)
                )
            }
        }
    }



    @Composable
    fun ColorBlob(color: Color, modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(20.dp)
                .background(color)
        )
    }

    fun sortOptionsWithDistances(
        targets: List<Color>,
        options: List<Color>
    ): List<Pair<Color, Double>> {
        // 1. Guard Clause to prevent NoSuchElementException
        if (targets.isEmpty() || options.isEmpty()) return emptyList()

        // 2. Pre-calculate LAB for targets (Optimization)
        val targetLabs = targets.map {
            val out = DoubleArray(3)
            ColorUtils.colorToLAB(it.toArgb(), out)
            out
        }

        return options.map { option ->
            val optionLab = DoubleArray(3)
            ColorUtils.colorToLAB(option.toArgb(), optionLab)

            // Find the distance to the CLOSEST target
            val minDistance = targetLabs.minOf { targetLab ->
                calculateEuclideanDistance(targetLab, optionLab)
            }

            // Return the Pair instead of just the Color
            option to minDistance
        }
            .sortedBy { it.second } // Sort by distance (ascending)
    }

    fun calculateEuclideanDistance(c1: DoubleArray, c2: DoubleArray): Double {
        return sqrt(
            (c1[0] - c2[0]).pow(2.0) +
                    (c1[1] - c2[1]).pow(2.0) +
                    (c1[2] - c2[2]).pow(2.0)
        )
    }

}