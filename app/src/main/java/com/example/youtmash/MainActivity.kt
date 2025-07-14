package com.ciamuthama.youtmash


import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.example.youtmash.ui.theme.YoutMashTheme
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.google.gson.annotations.SerializedName
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query


data class Clip(
    @SerializedName("videoId") val videoId: String,
    @SerializedName("title") val title: String,
    @SerializedName("highlightTimestamp") val highlightTimestamp: Int
)

data class ReelResponse(
    @SerializedName("clip") val clips: List<Clip>
)
interface ReelApiService {
    @GET("/generate-reel")
    suspend fun generateReel(@Query("q") query: String): ReelResponse
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            YoutMashTheme {
                    MainScreen( Modifier.background(Color.Black) )
                }
            }
        }
    }

@Composable
fun MainScreen(modifier: Modifier = Modifier ) {
    var clipList by remember { mutableStateOf<List<Clip>>(emptyList()) }

    LaunchedEffect(Unit) {
        // We call the updated searchYouTube function
        val results = searchYouTube("maasai mara")
        if (results.isNotEmpty()) {
            clipList = results
        }
    }

    // Pass the full list of clips to the player
    YouTubePlayer(clips = clipList, modifier = modifier)

}


@Composable
fun YouTubePlayer(clips: List<Clip>, modifier: Modifier = Modifier){
    val updatedClips by rememberUpdatedState(clips)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            YouTubePlayerView(context).apply {
                addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                    private var currentVideoIndex = 0
                    override fun onReady(youTubePlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer){
                        if (currentVideoIndex < updatedClips.size) {
                            val nextClip = updatedClips[currentVideoIndex]
                            youTubePlayer.loadVideo(nextClip.videoId, nextClip.highlightTimestamp.toFloat())
                        }
                    }

                    override fun onStateChange(
                        youTubePlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer,
                        state: PlayerConstants.PlayerState
                    ) {
                        if (state == PlayerConstants.PlayerState.ENDED) {
                            currentVideoIndex++
                            if (currentVideoIndex < clips.size) {
                                // FIX 2: Autoplay the NEXT video using its specific timestamp
                                val nextClip = clips[currentVideoIndex]
                                youTubePlayer.loadVideo(nextClip.videoId, nextClip.highlightTimestamp.toFloat())
                            }
                        }
                    }
                },

                )

            }

        },
        update = { view ->

            if (clips.isNotEmpty()) {
                view.getYouTubePlayerWhenReady(object : com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback {
                    override fun onYouTubePlayer(youTubePlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer) {
                        val firstClip = clips.first()
                        youTubePlayer.loadVideo(firstClip.videoId, firstClip.highlightTimestamp.toFloat())
                    }
                })
            }
        }
    )
}


private suspend fun searchYouTube(query: String): List<Clip> {
   val baseUrl = "http://192.168.31.176:8080/"
    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiServices = retrofit.create(ReelApiService::class.java)

    return try {
        val reelResponse = apiServices.generateReel(query)
        val clips = reelResponse.clips
        Log.d("BackendSearch", "Successfully fetched clips from backend: $clips")
        clips
    } catch (e: Exception) {
        Log.e("BackendSearch", "Error fetching from backend", e)
        emptyList()
    }
}

