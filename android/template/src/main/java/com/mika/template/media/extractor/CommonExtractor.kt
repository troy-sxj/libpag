package com.mika.template.media.extractor

import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer

/**
 * Author: shangxiaojian
 * Date: 2022/1/18 5:32 下午
 */
class CommonExtractor(path: String) {

    private var mExtractor: MediaExtractor = MediaExtractor()

    private var mAudioTrack = -1            //音频通道索引
    private var mVideoTrack = -1            //视频通道索引
    private var mCurSampleTime: Long = 0     //当前帧时间戳
    private var mCurSampleFlag: Int = 0     //当前帧标志
    private var mStartPos: Long = 0          //开始解码时间点

    init {
        mExtractor.setDataSource(path)
    }

    fun getVideoFormat(): MediaFormat? {
        for (i in 0 until mExtractor.trackCount) {
            val mediaFormat = mExtractor.getTrackFormat(i)
            val mime = mediaFormat.getString(MediaFormat.KEY_MIME)

            if (mime?.startsWith("video/") == true) {
                mVideoTrack = i
                break
            }
        }
        return if (mVideoTrack >= 0) {
            mExtractor.getTrackFormat(mVideoTrack)
        } else null
    }

    fun getAudioFormat(): MediaFormat? {
        for (i in 0 until mExtractor.trackCount) {
            val mediaFormat = mExtractor.getTrackFormat(i)
            val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                mAudioTrack = i
                break
            }
        }
        return if (mAudioTrack >= 0) {
            mExtractor.getTrackFormat(mAudioTrack)
        } else null
    }

    fun readBuffer(byteBuffer: ByteBuffer): Int {
        byteBuffer.clear()
        selectSourceTrack()
        var readSampleCount = mExtractor.readSampleData(byteBuffer, 0)
        if (readSampleCount < 0) {
            return -1
        }
        //记录当前帧的时间戳
        mCurSampleTime = mExtractor.sampleTime
        mCurSampleFlag = mExtractor.sampleFlags
        //进入下一帧
        mExtractor.advance()
        return readSampleCount
    }

    private fun selectSourceTrack() {
        if (mVideoTrack >= 0) {
            mExtractor.selectTrack(mVideoTrack)
        } else if (mAudioTrack >= 0) {
            mExtractor.selectTrack(mAudioTrack)
        }
    }

    /**
     * 用于跳播，快速将数据定位到指定的播放位置。通常只能seek到I帧。
     * seek类型：
     * 1. SEEK_TO_PREVIOUS_SYNC 跳播位置的上一个关键帧
     * 2. SEEK_TO_NEXT_SYNC 跳播位置的下一个关键帧
     * 3. SEEK_TO_CLOSEST_SYNC 距离跳播位置的最近的关键帧
     */
    fun seek(pos: Long): Long {
        mExtractor.seekTo(pos, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        return mExtractor.sampleTime
    }

    fun stop() {
        mExtractor.release()
    }

    fun getVideoTrack(): Int {
        return mVideoTrack
    }

    fun getAudioTrack(): Int {
        return mAudioTrack
    }

    fun setStartPos(pos: Long) {
        mStartPos = pos
    }

    fun getCurrentTimestamp(): Long {
        return mCurSampleTime
    }

    fun getSampleFlag(): Int {
        return mCurSampleFlag
    }

}