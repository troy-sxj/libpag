package com.mika.template.pag

import android.graphics.BitmapFactory
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import com.mika.template.R
import com.mika.template.maker.media.decoder.VideoExtractor
import org.libpag.*
import java.util.concurrent.Executors
import kotlin.math.roundToLong

/**
 * Author: shangxiaojian
 * Date: 2022/1/18 11:20 上午
 */
class PAGFileTestActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private val TAG: String = "PAGFileTestActivity"
    private lateinit var mContainer: FrameLayout
    private lateinit var mPAGView: PAGView
    private lateinit var surfaceView: SurfaceView

    private var mPAGPlayer: PAGPlayer? = null
    private var mPAGFile: PAGFile? = null

    private var mPagPlayerHandler: PlayerHandler? = null
    private val threadPool = Executors.newFixedThreadPool(2)

    private lateinit var movieExtractor: VideoExtractor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pag_file)

        mContainer = findViewById(R.id.container)
        surfaceView = findViewById(R.id.surfaceView)
        surfaceView.holder.addCallback(this)

        val openFd = assets.openFd("video/test.mp4")
        movieExtractor = VideoExtractor(this, openFd)
        threadPool.execute(movieExtractor)

        val handlerThread = HandlerThread("PAGPlayer")
        handlerThread.start()
        mPagPlayerHandler = PlayerHandler(handlerThread.looper)

        loadPAGFile()
    }

    private fun loadPAGFile() {
        mPAGFile = PAGFile.Load(assets, "resources/timestretch/repeat.pag")
//        mPAGFile?.replaceImage(0, PAGImage.FromAssets(assets, "img/test.jpg"))
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        Log.d(TAG, "surfaceCreated")
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        if (holder == null) {
            Log.w(TAG, "system error, create surface error")
            return
        }
        if (mPAGFile == null) {
            loadPAGFile()
        }
        mPAGFile?.let {
            mPagPlayerHandler?.play(it, holder.surface)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        Log.d(TAG, "surfaceDestroyed")
        release()
    }

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    private fun release() {
        mPagPlayerHandler?.release()
        mPAGFile?.removeAllLayers()
        mPAGPlayer = null
    }

    inner class PlayerHandler(looper: Looper) : Handler(looper) {

        private val MSG_UPDATE_PROGRESS = 0x00001

        var pagPlayer: PAGPlayer = PAGPlayer()
        var updateGap: Long = 42

        var totalFrames: Int = 0
        var curFrame: Int = 0

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                MSG_UPDATE_PROGRESS -> {
                    curFrame += 1
                    updateProgress(curFrame)
                }
            }
        }

        fun play(pagFile: PAGFile, surface: Surface) {
            loadPagMovieInfo(pagFile)
            pagPlayer.surface = PAGSurface.FromSurface(surface)
            pagPlayer.composition = pagFile
            pagPlayer.progress = 0.0

            sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, updateGap)
        }

        private fun loadPagMovieInfo(pagFile: PAGFile) {
            val frameRate = pagFile.frameRate()

            totalFrames = (pagFile.duration() * frameRate / 1000000).toInt()
            updateGap = (1000 / frameRate).roundToLong()
        }

        private fun updateProgress(frameIndex: Int) {
            val progress = frameIndex % totalFrames * 1.0f / totalFrames
            val composition = pagPlayer.composition
            if (composition is PAGFile) {
                val loadFrame = loadFrame()
                if (loadFrame != null) {
                    composition.replaceImage(0, loadFrame)
                }
//                composition.replaceImage(0, PAGImage.FromA)
//                composition.replaceImage(0, PAGImage.FromAssets(assets, "img/test.jpg"))
            }
            pagPlayer.progress = progress.toDouble()
//            Log.d(TAG, "updateProgress ---- progress = $progress")
            pagPlayer.flush()
            sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, updateGap)
        }

        private fun loadFrame(): PAGImage? {
            return movieExtractor.getFrame()
        }

        fun release() {
            pagPlayer.surface?.freeCache()
            pagPlayer.surface = null
            pagPlayer.release()
        }
    }

}