package com.mika.template.maker.media.io

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.mika.template.maker.media.converter.YuvToRgbConverter
import java.util.concurrent.ArrayBlockingQueue

/**
 * Author: shangxiaojian
 * Date: 2022/1/20 2:01 下午
 */
class VideoFileReader(
    private val context: Context,
    private var filePath: String,
    frameQueue: (ArrayBlockingQueue<Bitmap>) -> Unit
) : Runnable {

    private val TAG = "VideoFileReader"
    private lateinit var mExtractor: MediaExtractor
    private var mCodec: MediaCodec? = null
    var videoFormat: MediaFormat? = null
    private var mStop: Boolean = false
    private var mIsEOS = false
    private var mBufferInfo = MediaCodec.BufferInfo()

    private var width: Int = 0
    private var height: Int = 0

    private lateinit var yuvToRgbConverter: YuvToRgbConverter
    private var blockQueue: ArrayBlockingQueue<Bitmap> = ArrayBlockingQueue(5)

    init{
        frameQueue.invoke(blockQueue)
    }

    override fun run() {
        yuvToRgbConverter = YuvToRgbConverter(context)
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

                    val bmp = Bitmap.createBitmap(outputImage.width, outputImage.height, Bitmap.Config.ARGB_8888)
                    yuvToRgbConverter.yuvToRgb(outputImage, bmp)

                    blockQueue.put(bmp)

                    outputImage.close()
                    mCodec!!.releaseOutputBuffer(index, true)
                }
                if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    Log.i(TAG, "解码结束")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initExtractor() {
        mExtractor = MediaExtractor()
        val openFd = context.assets.openFd(filePath)
        mExtractor.setDataSource(openFd.fileDescriptor, openFd.startOffset, openFd.length)

        for (i in 0..mExtractor.trackCount) {
            val mediaFormat = mExtractor.getTrackFormat(i)
            if (mediaFormat.getString(MediaFormat.KEY_MIME)?.contains("video") == true) {
                mExtractor.selectTrack(i)
                videoFormat = mediaFormat
                break
            }
        }
    }

    private fun initCodec() {
        val type = videoFormat!!.getString(MediaFormat.KEY_MIME)
        width = videoFormat!!.getInteger(MediaFormat.KEY_WIDTH)
        height = videoFormat!!.getInteger(MediaFormat.KEY_HEIGHT)
        mCodec = MediaCodec.createDecoderByType(type!!)
        videoFormat!!.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
        )

        mCodec?.configure(videoFormat, null, null, 0)
        mCodec!!.start()
    }

    private fun pushBufferToDecoder(): Boolean {
        val inputBufferIndex = mCodec!!.dequeueInputBuffer(1000)
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

    fun getFrame(): Bitmap? {
        if (blockQueue.isEmpty()) return null
        return blockQueue.poll()
    }
}