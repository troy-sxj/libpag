package com.mika.template.media.decoder

/**
 * Author: shangxiaojian
 * Date: 2022/1/18 5:44 下午
 */
interface IDecoderProgress {
    /**
     * 视频宽高回调
     */
    fun videoSizeChange(width: Int, height: Int, rotationAngle: Int)

    /**
     * 视频播放进度回调
     */
    fun videoProgressChange(pos: Long)
}