package se.kjellstrand.colormatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                    var colorsPickedFromImage by remember(seed) { mutableStateOf(listOf<Color>()) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        DrawColorListWithTitle(
                            colors = baseColors,
                            title = "Available IKEA Colors"
                        )

                        DrawColorListWithTitle(
                            colors = colorsPickedFromImage,
                            title = "IKEA colors matching the image"
                        )
                        Spacer(Modifier.height(8.dp))

                        val frameColor = colorsPickedFromImage.firstOrNull() ?: Color.Transparent

                        val animatedFrameColor by animateColorAsState(
                            targetValue = frameColor,
                            label = "FrameColorAnimation"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(animatedFrameColor)
                                .padding(12.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data("https://picsum.photos/seed/$seed/400/300")
                                    .allowHardware(false)
                                    .build(),
                                contentDescription = null,
                                onSuccess = { state ->
                                    val bitmap = state.result.drawable.toBitmap()
                                    Palette.from(bitmap).generate { palette ->
                                        palette?.let {
                                            scope.launch {
                                                colorsPickedFromImage =
                                                    sortColorsByWeightedProminence(it, baseColors)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun sortColorsByWeightedProminence(
        palette: Palette,
        availableColors: List<Color>
    ): List<Color> = withContext(Dispatchers.Default) {

        val availableLabs = availableColors.map { color ->
            val lab = DoubleArray(3)
            ColorUtils.colorToLAB(color.toArgb(), lab)
            color to lab
        }

        val colorScores = mutableMapOf<Color, Float>()

        palette.swatches.forEach { swatch ->
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(swatch.rgb, hsl)
            val saturation = hsl[1] // Range 0.0 to 1.0

            // Determine the "Weight" of this color cluster.
            // We add a small constant (0.1) so neutrals aren't multiplied by zero.
            val weight = saturation + 0.1f
            val weightedPopulation = swatch.population * weight

            val swatchLab = DoubleArray(3)
            ColorUtils.colorToLAB(swatch.rgb, swatchLab)

            val closestIkeaColor = availableLabs.minBy { (_, lab) ->
                calculateEuclideanDistance(lab, swatchLab)
            }.first

            colorScores[closestIkeaColor] =
                colorScores.getOrDefault(closestIkeaColor, 0f) + weightedPopulation
        }

        return@withContext colorScores.toList()
            .sortedByDescending { it.second }
            .map { it.first }
    }

    @Composable
    private fun DrawColorListWithTitle(colors: List<Color>, title: String) {
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
            colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color)
                )
            }
        }
    }

    fun calculateEuclideanDistance(c1: DoubleArray, c2: DoubleArray): Double {
        return sqrt(
            (c1[0] - c2[0]).pow(2.0) +
                    (c1[1] - c2[1]).pow(2.0) +
                    (c1[2] - c2[2]).pow(2.0)
        )
    }

}