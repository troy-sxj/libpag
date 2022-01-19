package com.mika.template.media.decoder

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.mika.template.media.extractor.IExtractor
import com.mika.template.media.extractor.VideoExtractor
import java.nio.ByteBuffer

/**
 * Author: shangxiaojian
 * Date: 2022/1/18 5:46 下午
 */
class VideoDecoder(path: String, sfv: SurfaceView?, surface: Surface?) : BaseDecoder(path) {

    private  val TAG = "VideoDecoder"

    private val mSurfaceView = sfv
    private var mSurface = surface

    override fun check(): Boolean {
        if (mSurfaceView == null && mSurface == null) {
            Log.w(TAG, "SurfaceView和Surface都为空，至少需要一个不为空")
            mStateListener?.decoderError(this, "显示器为空")
            return false
        }
        return true
    }

    override fun initExtractor(path: String): IExtractor {
        return VideoExtractor(path)
    }

    override fun initSpecParams(format: MediaFormat) {
    }

    override fun initRender(): Boolean {
        return true
    }

    override fun render(outputBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
    }

    override fun configCodec(codec: MediaCodec, format: MediaFormat): Boolean {
        if (mSurface != null) {
            codec.configure(format, mSurface, null, 0)
            notifyDecode()
        } else if (mSurfaceView?.holder?.surface != null) {
            mSurface = mSurfaceView.holder?.surface
            configCodec(codec, format)
        } else {
            mSurfaceView?.holder?.addCallback(object : SurfaceHolder.Callback2 {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    mSurface = holder.surface
                    configCodec(codec, format)
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                }

                override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
                }
            })
            return false
        }
        return true
    }

    override fun finishDecode() {
    }
}