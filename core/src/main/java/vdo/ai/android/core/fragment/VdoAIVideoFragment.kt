package vdo.ai.android.core.fragment

import android.net.Uri
import android.os.Bundle
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventListener
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventType
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaItem.AdsConfiguration
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.Util
import vdo.ai.android.core.base.VdoBaseFragment
import vdo.ai.android.core.databinding.VideoAdFragmentBinding

/**
 *  created by Ashish Saini at 6th Oct 2023
 *
 **/
class VdoAIVideoFragment : VdoBaseFragment<VideoAdFragmentBinding>(VideoAdFragmentBinding::inflate) {


    var videoUrl : String? = ""
    var vastTagUrl : String? = ""

    companion object{
        const val VIDEO_URL_KEY = "SAMPLE_VIDEO_URL"
        const val VAST_TAG_URL_KEY = "SAMPLE_VAST_TAG_URL"
//        const val SAMPLE_VIDEO_URL = "https://storage.googleapis.com/gvabox/media/samples/stock.mp4"
//        const val SAMPLE_VAST_TAG_URL = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/single_ad_samples&sz=640x480&cust_params=sample_ct%3Dlinear&ciu_szs=300x250%2C728x90&gdfp_req=1&output=vast&unviewed_position_start=1&env=vp&impl=s&correlator="
    }

    private var player: ExoPlayer? = null
    private var adsLoader: ImaAdsLoader? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            videoUrl = it.getString(VIDEO_URL_KEY, "")
            vastTagUrl = it.getString(VAST_TAG_URL_KEY, "")
        }
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Create an AdsLoader.
        adsLoader = ImaAdsLoader.Builder(requireContext())
            .setAdEventListener(buildAdEventListener())
            .build()
    }

    private fun buildAdEventListener(): AdEventListener {

        val imaAdEventListener = AdEventListener { adEvent ->
                val eventType = adEvent.type
                val log = "IMA event: $eventType"
                if (eventType == AdEventType.AD_PROGRESS) {
                    return@AdEventListener
                }
            }
        return imaAdEventListener
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initializePlayer()
            binding.playerView.onResume()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer()
            binding.playerView.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            binding.playerView.onPause()
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            binding.playerView.onPause()
            releasePlayer()
        }
    }

     override fun onDestroy() {
        adsLoader?.release()
        super.onDestroy()
    }

    private fun releasePlayer() {
        adsLoader?.setPlayer(null)
        binding.playerView.player = null
        player?.release()
        player = null
    }


    private fun initializePlayer() {

        // Set up the factory for media sources, passing the ads loader and ad view providers.
        val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(requireContext())
        val mediaSourceFactory: MediaSource.Factory = DefaultMediaSourceFactory(dataSourceFactory)
            .setLocalAdInsertionComponents({
                    unusedAdTagUri: AdsConfiguration? -> adsLoader },
                    binding.playerView
            )

        // Create an ExoPlayer and set it as the player for content and ads.
        player = ExoPlayer.Builder(requireContext()).setMediaSourceFactory(mediaSourceFactory).build()
        binding.playerView.player = player
        adsLoader?.setPlayer(player)

        // Create the MediaItem to play, specifying the content URI and ad tag URI.
        val contentUri = Uri.parse(videoUrl)
        val adTagUri = Uri.parse(vastTagUrl)
        val mediaItem = MediaItem.Builder()
            .setUri(contentUri)
            .setAdsConfiguration(AdsConfiguration.Builder(adTagUri).build())
            .build()

        player?.apply {

            // Prepare the content and ad to be played with the SimpleExoPlayer.
            setMediaItem(mediaItem)
            prepare()
            // Set PlayWhenReady. If true, content and ads will autoplay.
            playWhenReady = true

        }

    }

}