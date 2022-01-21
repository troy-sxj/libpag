package com.mika.template.maker.pag

import android.content.Context
import android.util.Log
import com.mika.template.maker.pag.model.PAGSceneInfo
import com.mika.template.maker.pag.model.PAGTextScene
import com.mika.template.maker.pag.model.PAGVideoScene
import org.libpag.PAGFile
import org.libpag.PAGImageLayer
import org.libpag.PAGLayer

/**
 * <p>
 *     load pag file, parse basic info
 *     TODO: need to change io/parse action async
 * </p>
 * Author: shangxiaojian
 * Date: 2022/1/19 11:29 上午
 */
class PAGFileReader(private val context: Context, private var mCallback: PAGFileLoaderCallback? = null) {

    private val TAG = "PAGFileReader"

    private var mPAGFile: PAGFile? = null
    private var mPAGSceneInfo: PAGSceneInfo = PAGSceneInfo()

    fun openFile(filePath: String) {
        openPAGFile(false, filePath)
    }

    fun openAssertFile(assertPath: String) {
        openPAGFile(true, assertPath)
    }

    private fun openPAGFile(isAssert: Boolean, path: String) {
        //TODO sxj check permission
        mPAGFile = if (isAssert) {
            PAGFile.Load(context.assets, path)
        } else {
            PAGFile.Load(path)
        }
        parsePAG()
    }

    private fun parsePAG() {
        if (mPAGFile == null) {
            Log.w(TAG, "parsePAG: parse failed, pag file not exit")
            return
        }
        val numTexts = mPAGFile?.numTexts()
        if (numTexts ?: 0 > 0) {
            val textScenes = ArrayList<PAGTextScene>()
            for (i in 0 until numTexts!!) {
                val textData = mPAGFile?.getTextData(i)
                textScenes.add(PAGTextScene(textData!!.text))
            }
            mPAGSceneInfo.textScenes = textScenes
        }

        val numImages = mPAGFile?.numImages()
        if (numImages ?: 0 > 0) {
            val videoScenes = ArrayList<PAGVideoScene>()
            for (i in 0 until numImages!!) {
                val layersByEditableIndex = mPAGFile?.getLayersByEditableIndex(i, PAGLayer.LayerTypeImage)
                if (layersByEditableIndex?.isNotEmpty() == true && layersByEditableIndex[0] is PAGImageLayer) {
                    val curImgLayer = layersByEditableIndex[0] as PAGImageLayer

                    val duration = curImgLayer.localTimeToGlobal(curImgLayer.duration())


//                    val duration = curImgLayer.duration()
                    val startTime = curImgLayer.localTimeToGlobal(curImgLayer.startTime())

//                    Log.d(TAG, "content duration: $contentDuration, startTime=$startTime" + ", duration=" + duration)
                    val videoPath = "video/vod_" + (i % 3) + ".mp4"

                    videoScenes.add(PAGVideoScene(videoPath, startTime = startTime, duration = duration))
                }
            }
            mPAGSceneInfo.videoScenes = videoScenes
        }

        mCallback?.onPAGLoaded(mPAGFile, mPAGSceneInfo)
    }

    fun setLoadCallback(callback: PAGFileLoaderCallback) {
        mCallback = callback
    }

    interface PAGFileLoaderCallback {

        fun onPAGLoaded(pageFile: PAGFile?, sceneInfo: PAGSceneInfo)

        fun onPAGLoadFailed(errCode: Int, errMsg: String? = null)
    }
}