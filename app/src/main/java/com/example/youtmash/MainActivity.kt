package com.example.youtmash


import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.example.youtmash.ui.theme.YoutMashTheme
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YoutMashTheme {

                    MainScreen(

                )
                }
            }
        }
    }








@Composable
fun MainScreen() {
    var videoIdList by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        val results = searchYouTube("Best Places in kenya")
        if (results.isNotEmpty()){
            videoIdList = results

        }
    }

    YouTubePlayer(videoIds = videoIdList)

}


@Composable
fun YouTubePlayer(videoIds: List<String>){
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            YouTubePlayerView(context).apply {
                addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                    private var currentVideoIndex = 0
                    override fun onReady(youTubePlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer){
                        if(videoIds.isNotEmpty()){
                            currentVideoIndex = 0
                            youTubePlayer.loadVideo(videoIds[currentVideoIndex], 0f)
                        }
                    }

                    override fun onStateChange(
                        youTubePlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer,
                        state: PlayerConstants.PlayerState
                    ) {
                        if (state == PlayerConstants.PlayerState.ENDED) {
                            currentVideoIndex++
                            if(currentVideoIndex < videoIds.size){
                                youTubePlayer.loadVideo(videoIds[currentVideoIndex], 0f)
                            }
                        }
                    }
                },

                )

            }

        },
        update = { view ->

            if (videoIds.isNotEmpty()) {
                view.getYouTubePlayerWhenReady(object : com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback {
                    override fun onYouTubePlayer(youTubePlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer) {
                        youTubePlayer.loadVideo(videoIds.first(), 0f)
                    }
                })
            }
        }
    )
}


private suspend fun searchYouTube(query: String): List<String> {
    return withContext(Dispatchers.IO)  {
        try {
            val transport = NetHttpTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()

            val youTube = YouTube.Builder(transport, jsonFactory, null)
                .setApplicationName("YoutMash")
                .build()
            val search = youTube.search().list(listOf("id","snippet").joinToString(",")).apply {
                key = BuildConfig.YOUTUBE_API_KEY
                q = query
                type = "video"
                maxResults = 5
                fields = "items(id/videoId,snippet/title)"
            }

            val response = search.execute()
            val videoIds = response.items?.map { it.id.videoId } ?: emptyList()
            Log.d("searchYouTube", "Video IDs: $videoIds")
            videoIds
        } catch (e: Exception) {
            Log.e("searchYouTube", "Error searching YouTube", e)
            emptyList<String>()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    YoutMashTheme {
        MainScreen()
    }
}