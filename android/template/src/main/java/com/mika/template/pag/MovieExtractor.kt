package com.mika.template.pag

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo.CodecCapabilities
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.mika.template.media.decoder.Frame
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import android.graphics.BitmapFactory

import android.graphics.Bitmap
import java.nio.ByteBuffer


/**
 * Author: shangxiaojian
 * Date: 2022/1/18 5:58 下午
 */
class MovieExtractor(private val context: Context, private var filePath: String) : Runnable {

    private val TAG = "MovieExtractor"

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

    private var blockQueue: ArrayBlockingQueue<Bitmap> = ArrayBlockingQueue(5)

    private lateinit var yuvToRgbConverter:YuvToRgbConverter

    override fun run() {
        yuvToRgbConverter =YuvToRgbConverter(context)
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
//                    val bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)

                    //TODO sxj how to use frame
                    Log.d(TAG, "decode one frame")
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
        } finally {
            release()
        }
    }

    private fun compressToJpeg(fileName: String, image: Image) {
        val outStream: FileOutputStream
        try {
            outStream = FileOutputStream(fileName)
        } catch (ioe: IOException) {
            throw RuntimeException("Unable to create output file $fileName", ioe)
        }
        val rect = image.cropRect
        val yuvImage = YuvImage(getDataFromImage(image), ImageFormat.NV21, rect.width(), rect.height(), null)
        yuvImage.compressToJpeg(rect, 100, outStream)
    }


    private fun getDataFromImage(image: Image): ByteArray? {
        val crop = image.cropRect
        val format = image.format
        val width = crop.width()
        val height = crop.height()
        val planes = image.planes
        val data = ByteArray(width * height * ImageFormat.getBitsPerPixel(format)/8)
        val rowData = ByteArray(planes[0].rowStride)
        val channelOffset = 0
        val outputStride = 1
        return null
    }


    fun getFrame(): Bitmap? {
        if (blockQueue.isEmpty()) return null
        return blockQueue.poll()
    }

    private fun initExtractor() {
        mExtractor = MediaExtractor()
        mExtractor.setDataSource(filePath)

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
        videoFormat!!.setInteger(MediaFormat.KEY_COLOR_FORMAT, CodecCapabilities.COLOR_FormatYUV420Flexible)

        mCodec?.configure(videoFormat, null, null, 0)
        mCodec!!.start()
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