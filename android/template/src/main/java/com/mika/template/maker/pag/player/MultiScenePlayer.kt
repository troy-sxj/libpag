package com.mika.template.maker.pag.player

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.view.Surface
import com.mika.template.maker.pag.model.PAGSceneInfo
import org.libpag.PAGFile
import org.libpag.PAGImage
import org.libpag.PAGPlayer
import org.libpag.PAGSurface
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToLong

/**
 * Author: shangxiaojian
 * Date: 2022/1/19 1:46 下午
 */
class MultiScenePlayer(private val mContext: Context) {

    private val TAG = "MultiScenePlayer"
    private var mSceneInfo: PAGSceneInfo? = null
    private var mPagFile: PAGFile? = null

    private val mWorkThreadPool: ExecutorService
    private val mPAGPlayerHandler: PlayerHandler
    private var handlerThread: HandlerThread

    init {
        //if has video scene, thread count defer to scene size
        mWorkThreadPool = Executors.newFixedThreadPool(6)

        handlerThread = HandlerThread(TAG)
        handlerThread.start()
        mPAGPlayerHandler = PlayerHandler(handlerThread.looper)
    }

    fun setDataSource(sceneInfo: PAGSceneInfo, pagFile: PAGFile) {
        mSceneInfo = sceneInfo
        mPagFile = pagFile
    }

    fun setSurface(surface: Surface) {
        mPAGPlayerHandler.setSurface(surface)
    }

    fun start() {
        mSceneInfo?.textScenes?.forEachIndexed { index, pagTextScene ->
            val textData = mPagFile?.getTextData(index)
            textData?.text = pagTextScene.modifiedText
            mPagFile?.replaceText(index, textData)
        }

//        mSceneInfo?.imgScenes?.forEachIndexed { index, pagImageScene ->
//            pagImageScene.imgPath?.let {
//                createPAGImage(it)?.run {
//                    mPagFile?.replaceImage(index, this)
//                }
//            }
//        }
        mPagFile?.let { mPAGPlayerHandler.start(it) }
    }

    private fun createPAGImage(imgPath: String): PAGImage? {
        var stream: InputStream? = null
        try {
            stream = mContext.assets.open(imgPath)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val bitmap = BitmapFactory.decodeStream(stream) ?: return null
        return PAGImage.FromBitmap(bitmap)
    }

    fun release() {
        mPAGPlayerHandler.release()
        handlerThread.quitSafely()

    }

    inner class PlayerHandler(looper: Looper) : Handler(looper) {

        private val MSG_UPDATE_PROGRESS = 0x00001

        private var pagPlayer: PAGPlayer? = PAGPlayer()
        private var updateGap: Long = 42
        private var totalFrames: Int = 0
        private var curFrame: Int = 0

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                MSG_UPDATE_PROGRESS -> {
                    curFrame += 1
                    updateProgress(curFrame)
                }
            }
        }

        fun setSurface(surface: Surface) {
            pagPlayer?.surface = PAGSurface.FromSurface(surface)
        }

        fun start(pagFile: PAGFile) {
            val frameRate = pagFile.frameRate()

            totalFrames = (pagFile.duration() * frameRate / 1000000).toInt()
            updateGap = (1000 / frameRate).roundToLong()

            pagPlayer?.composition = pagFile
            pagPlayer?.progress = 0.0
            sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, updateGap)
        }

        private fun updateProgress(frameIndex: Int) {
            val progress = frameIndex % totalFrames * 1.0f / totalFrames
            val composition = pagPlayer?.composition

            pagPlayer?.progress = progress.toDouble()
            pagPlayer?.flush()
            sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, updateGap)
        }

        private fun loadFrame() {

        }

        fun release() {
            pagPlayer?.release()
            pagPlayer = null
        }
    }
}