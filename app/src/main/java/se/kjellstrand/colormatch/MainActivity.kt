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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import se.kjellstrand.colormatch.ui.theme.ColorMatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ColorMatchTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    ImageList()
                }
            }
        }
    }

    @Composable
    fun ColorList(
        colors: List<Color>,
        highlightedColor: Color?,
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            colors.forEach { color ->
                val weight = if (color == highlightedColor) 2f else 1f

                Box(
                    modifier = Modifier
                        .weight(weight)
                        .height(20.dp)
                        .background(color)
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ImageList(modifier: Modifier = Modifier) {
        val imageCount = 5
        var imageSeeds by remember { mutableStateOf((1..imageCount).toList()) }
        val baseColors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow)
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

                    val randomColors = remember(seed) { (1..4).map { generateRandomColor() } }
                    val allColors = baseColors + randomColors
                    val selectedColorFromList = remember(seed) { allColors.random() }
                    val completelyRandomColor = remember(seed) { generateRandomColor() }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        ColorList(
                            colors = allColors,
                            highlightedColor = selectedColorFromList
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ColorBlob(color = selectedColorFromList)
                            ColorBlob(color = completelyRandomColor)
                        }

                        AsyncImage(
                            model = "https://picsum.photos/seed/$seed/400/300",
                            contentDescription = null,
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

    @Composable
    fun ColorBlob(color: Color, modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(color)
        )
    }

    fun generateRandomColor() = Color(
        red = (0..255).random() / 255f,
        green = (0..255).random() / 255f,
        blue = (0..255).random() / 255f,
        alpha = 1f
    )

}