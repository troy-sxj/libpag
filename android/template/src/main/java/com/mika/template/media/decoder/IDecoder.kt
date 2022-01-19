package com.mika.template.media.decoder

import android.media.MediaFormat

/**
 * Author: shangxiaojian
 * Date: 2022/1/18 5:39 下午
 */
interface IDecoder : Runnable {

    fun pause()

    fun resume()

    fun seekTo(pos:Long): Long

    fun seekAndPlay(pos: Long): Long

    /**
     * 停止解码
     */
    fun stop()

    /**
     * 是否正在解码
     */
    fun isDecoding(): Boolean

    /**
     * 是否正在快进
     */
    fun isSeeking(): Boolean

    /**
     * 是否停止解码
     */
    fun isStop(): Boolean

    /**
     * 设置尺寸监听器
     */
    fun setSizeListener(l: IDecoderProgress)

    /**
     * 设置状态监听器
     */
    fun setStateListener(l: IDecoderStateListener?)

    /**
     * 获取视频宽
     */
    fun getWidth(): Int

    /**
     * 获取视频高
     */
    fun getHeight(): Int

    /**
     * 获取视频长度
     */
    fun getDuration(): Long

    /**
     * 当前帧时间，单位：ms
     */
    fun getCurTimeStamp(): Long

    /**
     * 获取视频旋转角度
     */
    fun getRotationAngle(): Int

    /**
     * 获取音视频对应的格式参数
     */
    fun getMediaFormat(): MediaFormat?

    /**
     * 获取音视频对应的媒体轨道
     */
    fun getTrack(): Int

    /**
     * 获取解码的文件路径
     */
    fun getFilePath(): String

    /**
     * 无需音视频同步
     */
    fun withoutSync(): IDecoder
}