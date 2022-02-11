package com.mika.template.maker.media.decoder

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.opengl.GLES20
import android.util.Log
import android.view.Surface
import org.libpag.PAGImage

/**
 * Author: shangxiaojian
 * Date: 2022/2/11 10:25 上午
 */
class VideoExtractor(private val context: Context, private var filePath: AssetFileDescriptor) : Runnable {

    private val TAG = "VideoExtractor"

    private lateinit var mExtractor: MediaExtractor
    private var mCodec: MediaCodec? = null
    var videoFormat: MediaFormat? = null
    private var mStop: Boolean = false
    private var mIsEOS = false

    private val mLock = Object()

    private var mBufferInfo = MediaCodec.BufferInfo()


    private var mCurSampleTime: Long = 0    //当前帧时间戳
    private var mCurSampleFlag: Int = 0     //当前帧标志

    private var frameIndex = 0

    private var mSurfaceTexture: SurfaceTexture? = null
    private var mTextureId: Int = -1


    override fun run() {
        initExtractor()
        initCodec()
        try {
            while (!mStop) {
                if (!mIsEOS) {
                    mIsEOS = pushBufferToDecoder()
                }
                val index = pullBufferFromDecoder()
                if (index >= 0 && mBufferInfo.size != 0) {
                    val outputImage = mCodec!!.getOutputImage(index)
                    Log.i(TAG, "image format: " + outputImage!!.format)

//                    val bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)

                    //TODO sxj how to use frame
                    Log.d(TAG, "decode one frame")


                    outputImage.close()
                    mCodec!!.releaseOutputBuffer(index, true)
                }
                if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    Log.i(TAG, "解码结束")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            release()
        }
    }

    private fun eglSetup(){

    }

    fun getFrame(): PAGImage? {
//        PAGImage.FromTexture(mTextureId, width, height)
        return null
    }

    private fun initExtractor() {
        mExtractor = MediaExtractor()
        mExtractor.setDataSource(filePath.fileDescriptor, filePath.startOffset, filePath.length)

        for (i in 0..mExtractor.trackCount) {
            val mediaFormat = mExtractor.getTrackFormat(i)
            if (mediaFormat.getString(MediaFormat.KEY_MIME).contains("video")) {
                mExtractor.selectTrack(i)
                videoFormat = mediaFormat
                break
            }
        }
    }

    private var width: Int = 0
    private var height: Int = 0

    private fun initCodec() {
        val type = videoFormat!!.getString(MediaFormat.KEY_MIME)
        width = videoFormat!!.getInteger(MediaFormat.KEY_WIDTH)
        height = videoFormat!!.getInteger(MediaFormat.KEY_HEIGHT)

        mCodec = MediaCodec.createDecoderByType(type!!)
//        videoFormat!!.setInteger(
//            MediaFormat.KEY_COLOR_FORMAT,
//            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
//        )
        mCodec?.configure(videoFormat, createSurface(), null, 0)
        mCodec!!.start()
    }

    private fun createSurface(): Surface {
        val texture = IntArray(1)
        GLES20.glGenTextures(1, texture, 0)
        mTextureId = texture[0]
        mSurfaceTexture = SurfaceTexture(mTextureId)
        return Surface(mSurfaceTexture)
    }

    private fun pushBufferToDecoder(): Boolean {
        var inputBufferIndex = mCodec!!.dequeueInputBuffer(1000)
        var isEndOfStream = false

        if (inputBufferIndex >= 0) {
            val inputBuffer = mCodec?.getInputBuffer(inputBufferIndex)
            val sampleSize = mExtractor.readSampleData(inputBuffer!!, 0)

            if (sampleSize < 0) {
                //如果数据已经取完，压入数据结束标志：MediaCodec.BUFFER_FLAG_END_OF_STREAM
                mCodec!!.queueInputBuffer(
                    inputBufferIndex, 0, 0,
                    0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
                isEndOfStream = true
            } else {
                mExtractor.sampleTime
                mCodec!!.queueInputBuffer(
                    inputBufferIndex, 0,
                    sampleSize, mExtractor.sampleTime, 0
                )
                mExtractor.advance()
            }
        }
        return isEndOfStream
    }


    private fun pullBufferFromDecoder(): Int {
        var outputEos = false
        var outputBufferIndex = mCodec!!.dequeueOutputBuffer(mBufferInfo, 1000)
        if (outputBufferIndex >= 0) {
            if ((mBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                outputEos = true
            }
        }

        return outputBufferIndex
    }

    fun release() {

    }


}