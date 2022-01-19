package com.mika.template.media.decoder

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import com.mika.template.media.extractor.IExtractor
import java.io.File
import java.nio.ByteBuffer

/**
 * Author: shangxiaojian
 * Date: 2022/1/18 5:40 下午
 */
abstract class BaseDecoder(private val mFilePath: String) : IDecoder {

    private val TAG = "BaseDecoder"

    //---------------------线程相关----------------------
    private var mIsRunning = true   //解码器是否正在运行
    private var mLock = Object()   //线程等待锁
    private val mReadyForDecode = false     //是否可以进入解码

    //---------------------状态相关----------------------
    private var mCodec: MediaCodec? = null         //音视频解码器
    private var mExtractor: IExtractor? = null   //音视频数据读取器
    private var mInputBuffers: Array<ByteBuffer>? = null  //解码输入缓存区
    private var mOutputBuffers: Array<ByteBuffer>? = null //解码输出缓存区
    private var mBufferInfo = MediaCodec.BufferInfo()   //解码数据信息

    private var mState = DecodeState.STOP
    private var mIsEOS = false      //流是否结束
    private var mStartTimeForSync = -1L //开始解码时间，用于音视频同步
    private var mSyncRender = true      // 是否需要音视频渲染同步

    private var mDuration: Long = 0
    private var mStartPos: Long = 0
    private var mEndPos: Long = 0

    protected var mStateListener: IDecoderStateListener? = null
    protected var mVideoWidth = 0
    protected var mVideoHeight = 0

    override fun run() {
        if (mState == DecodeState.STOP) {
            mState = DecodeState.START
        }
        mStateListener?.decoderPrepare(this)

        //1. 初始化解码器
        if (!init()) return
        Log.i(TAG, "开始解码")
        try {
            while (mIsRunning) {
                if (mState != DecodeState.START &&
                    mState != DecodeState.DECODING &&
                    mState != DecodeState.SEEKING
                ) {
                    Log.i(TAG, "进入等待：$mState")

                    waitDecode()

                    //恢复同步的起始时间，即去除等待流失的时间
                    mStartTimeForSync = System.currentTimeMillis() - getCurTimeStamp()
                }

                if (!mIsRunning ||
                    mState == DecodeState.STOP
                ) {
                    mIsRunning = false
                    break
                }

                if (mStartTimeForSync == -1L) {
                    mStartTimeForSync = System.currentTimeMillis()
                }
                //如果数据没有解码完成，将数据推入解码器解码
                if (!mIsEOS) {
                    //2. 将数据压缩解码器输入缓冲区
                    mIsEOS = pushBufferToDecoder()
                }
                //3. 将解码好的数据从输出缓冲区中取出
                val index = pullBufferFromDecoder()
                if (index >= 0) {
                    if (mSyncRender && mState == DecodeState.DECODING) {
                        sleepRender()
                    }
                    //4. 渲染
                    if (mSyncRender) {
                        render(mOutputBuffers!![index], mBufferInfo)
                    }

                    val frame = Frame()
                    frame.buffer = mOutputBuffers!![index]
                    frame.setBufferInfo(mBufferInfo)
                    mStateListener?.decodeOneFrame(this, frame)

                    //5. 释放输出缓冲
                    mCodec!!.releaseOutputBuffer(index, true)

                    if (mState == DecodeState.START) {
                        mState = DecodeState.PAUSE
                    }
                }
                //6. 判断解码是否完成
                if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    Log.i(TAG, "解码结束")
                    mState = DecodeState.FINISH
                    mStateListener?.decoderFinish(this)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            finishDecode()
            release()
        }
    }

    private fun init(): Boolean {
        if (mFilePath.isEmpty() || !File(mFilePath).exists()) {
            Log.w(TAG, "文件路径为空")
            mStateListener?.decoderError(this, "文件路径为空")
            return false
        }
        if (!check()) return false

        //初始化数据提取器
        mExtractor = initExtractor(mFilePath)
        if (mExtractor == null ||
            mExtractor!!.getFormat() == null
        ) {
            Log.w(TAG, "无法解析文件")
            return false
        }

        if (!initParams()) return false

        if (!initRender()) return false

        if (!initCodec()) return false
        return true
    }

    private fun initParams(): Boolean {
        try {
            val format = mExtractor!!.getFormat()!!
            mDuration = format.getLong(MediaFormat.KEY_DURATION) / 1000
            if (mEndPos == 0L) mEndPos = mDuration

            initSpecParams(mExtractor!!.getFormat()!!)
        } catch (e: Exception) {
            return false
        }
        return true
    }

    private fun initCodec(): Boolean {
        try {
            val type = mExtractor!!.getFormat()!!.getString(MediaFormat.KEY_MIME)!!
            mCodec = MediaCodec.createDecoderByType(type)
            if (!configCodec(mCodec!!, mExtractor!!.getFormat()!!)) {
                waitDecode()
            }
            mCodec!!.start()

            //TODO sxj: use getInputBuffer to instead of this method, sometimes buffer will auto clear
            mInputBuffers = mCodec?.inputBuffers
            mOutputBuffers = mCodec?.outputBuffers
        } catch (e: Exception) {
            return false
        }
        return true
    }

    private fun pushBufferToDecoder(): Boolean {
        //从codec中获取可用的inputBuffer index
        var inputBufferIndex = mCodec!!.dequeueInputBuffer(1000)
        var isEndOfStream = false

        if (inputBufferIndex >= 0) {
            //通过index从inputBuffers中获取指定buffer
            val inputBuffer = mInputBuffers!![inputBufferIndex]
            //音视频提取器将数据读取到指定buffer
            val sampleSize = mExtractor!!.readBuffer(inputBuffer)

            if (sampleSize < 0) {
                //如果读取size返回-1，说明文件结束，接入结束符号
                mCodec!!.queueInputBuffer(
                    inputBufferIndex,
                    0,
                    0,
                    0,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
                isEndOfStream = true
            } else {
                //否则，将inputbuffer输入解码器进行解码
                mCodec!!.queueInputBuffer(
                    inputBufferIndex,
                    0,
                    sampleSize,
                    mExtractor!!.getCurrentTimestamp(),
                    0
                )
            }
        }
        return isEndOfStream
    }

    private fun pullBufferFromDecoder(): Int {
        // 查询是否有解码完成的数据，index >=0 时，表示数据有效，并且index为缓冲区索引
        var index = mCodec!!.dequeueOutputBuffer(mBufferInfo, 1000)
        when (index) {
            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {}
            MediaCodec.INFO_TRY_AGAIN_LATER -> {}
            MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                mOutputBuffers = mCodec!!.outputBuffers
            }
            else -> {
                return index
            }
        }
        return -1
    }

    private fun waitDecode() {
        try {
            if (mState == DecodeState.PAUSE) {
                mStateListener?.decoderPause(this)
            }
            synchronized(mLock) {
                mLock.wait()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    protected fun notifyDecode() {
        synchronized(mLock) {
            mLock.notifyAll()
        }
        if (mState == DecodeState.DECODING) {
            mStateListener?.decoderRunning(this)
        }
    }

    private fun sleepRender() {
        val passTime = System.currentTimeMillis() - mStartTimeForSync
        val curTime = getCurTimeStamp()
        if (curTime > passTime) {
            Thread.sleep(curTime - passTime)
        }
    }

    private fun release() {
        try {
            Log.i(TAG, "解码停止，释放解码器")
            mState = DecodeState.STOP
            mIsEOS = false
            mExtractor?.stop()
            mCodec?.stop()
            mCodec?.release()
            mStateListener?.decoderDestroy(this)
        } catch (e: Exception) {
        }
    }

    override fun pause() {
        mState = DecodeState.DECODING
    }

    override fun resume() {
        mState = DecodeState.DECODING
        notifyDecode()
    }

    override fun seekTo(pos: Long): Long {
        return 0
    }

    override fun seekAndPlay(pos: Long): Long {
        return 0
    }

    override fun stop() {
        mState = DecodeState.STOP
        mIsRunning = false
        notifyDecode()
    }

    override fun isDecoding(): Boolean {
        return mState == DecodeState.DECODING
    }

    override fun isSeeking(): Boolean {
        return mState == DecodeState.SEEKING
    }

    override fun isStop(): Boolean {
        return mState == DecodeState.STOP
    }

    override fun setSizeListener(l: IDecoderProgress) {
    }

    override fun setStateListener(l: IDecoderStateListener?) {
        mStateListener = l
    }

    override fun getWidth(): Int {
        return mVideoWidth
    }

    override fun getHeight(): Int {
        return mVideoHeight
    }

    override fun getDuration(): Long {
        return mDuration
    }

    override fun getCurTimeStamp(): Long {
        return mBufferInfo.presentationTimeUs / 1000
    }

    override fun getRotationAngle(): Int {
        return 0
    }

    override fun getMediaFormat(): MediaFormat? {
        return mExtractor?.getFormat()
    }

    override fun getTrack(): Int {
        return 0
    }

    override fun getFilePath(): String {
        return mFilePath
    }

    override fun withoutSync(): IDecoder {
        mSyncRender = false
        return this
    }

    /**
     * 检查子类参数
     */
    abstract fun check(): Boolean

    /**
     * 初始化数据提取器
     */
    abstract fun initExtractor(path: String): IExtractor

    /**
     * 初始化子类特有参数
     */
    abstract fun initSpecParams(format: MediaFormat)

    /**
     * 初始化渲染器
     */
    abstract fun initRender(): Boolean

    /**
     * 渲染
     */
    abstract fun render(outputBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo)

    /**
     * 配置解码器
     */
    abstract fun configCodec(codec: MediaCodec, format: MediaFormat): Boolean

    abstract fun finishDecode()
}