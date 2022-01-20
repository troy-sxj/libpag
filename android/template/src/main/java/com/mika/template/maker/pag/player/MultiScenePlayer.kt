package com.mika.template.maker.pag.player

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.view.Surface
import com.mika.template.maker.media.io.VideoFileReader
import com.mika.template.maker.pag.PAGUtils
import com.mika.template.maker.pag.model.PAGSceneInfo
import org.libpag.PAGFile
import org.libpag.PAGPlayer
import org.libpag.PAGSurface
import java.util.concurrent.ArrayBlockingQueue
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

    private val mPAGPlayerHandler: PlayerHandler
    private var handlerThread: HandlerThread = HandlerThread(TAG)

    init {
        handlerThread.start()
        mPAGPlayerHandler = PlayerHandler(handlerThread.looper)
    }

    fun setDataSource(sceneInfo: PAGSceneInfo, pagFile: PAGFile) {
        mSceneInfo = sceneInfo
        mPagFile = pagFile

        mPAGPlayerHandler.setDataSource(sceneInfo, pagFile)
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

        mSceneInfo?.imgScenes?.forEachIndexed { index, pagImageScene ->
            pagImageScene.imgPath?.let {
                PAGUtils.createPAGImage(mContext, it)?.run {
                    mPagFile?.replaceImage(index, this)
                }
            }
        }
        mPAGPlayerHandler.start()
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

        private lateinit var pagFile: PAGFile
        private var sceneInfo: PAGSceneInfo? = null
        private var mWorkThreadPool: ExecutorService? = null

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

        fun setDataSource(sceneInfo: PAGSceneInfo, pagFile: PAGFile) {
            this.pagFile = pagFile
            this.sceneInfo = sceneInfo
        }

        fun prepareAsync() {
            mWorkThreadPool = Executors.newFixedThreadPool(6)
            sceneInfo?.videoScenes?.run {
                this.forEach { sceneItem ->
                    val videoFileReader =
                        VideoFileReader(mContext, sceneItem.videoPath, object : (ArrayBlockingQueue<Bitmap>) -> Unit {

                            override fun invoke(blockQueue: ArrayBlockingQueue<Bitmap>) {
                                sceneItem.blockingQueue = blockQueue
                            }

                        })
                    mWorkThreadPool?.execute(videoFileReader)
                }
            }
        }

        fun start() {
            val frameRate = pagFile.frameRate()

            totalFrames = (pagFile.duration() * frameRate / 1000000).toInt()
            updateGap = (1000 / frameRate).roundToLong()

            pagPlayer?.composition = pagFile
            pagPlayer?.progress = 0.0
            sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, updateGap)
        }

        private fun updateProgress(frameIndex: Int) {
            val progress = frameIndex % totalFrames * 1.0f / totalFrames
//            val composition = pagPlayer?.composition

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