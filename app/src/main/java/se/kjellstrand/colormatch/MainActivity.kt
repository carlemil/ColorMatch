package se.kjellstrand.colormatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import se.kjellstrand.colormatch.ui.theme.ColorMatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow)
        enableEdgeToEdge()
        setContent {
            ColorMatchTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    ColorList(colors)
                    ImageList()
                }
            }
        }
    }

    @Composable
    fun ColorList(colors: List<Color>, modifier: Modifier = Modifier) {
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(colors) { color ->
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }

    @Composable
    fun ImageList(modifier: Modifier = Modifier) {
        // We generate a list of 10 unique seeds to ensure we get different random images
        val imageSeeds = remember { (1..10).toList() }

        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(imageSeeds) { seed ->
                AsyncImage(
                    // Lorem Picsum: https://picsum.photos/seed/{seed}/{width}/{height}
                    model = "https://picsum.photos/seed/$seed/400/300",
                    contentDescription = "Random Image $seed",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp) // Define height to prevent layout jumps
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}