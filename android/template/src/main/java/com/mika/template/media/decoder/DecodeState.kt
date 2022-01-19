package com.mika.template.media.decoder

/**
 * Author: shangxiaojian
 * Date: 2022/1/18 5:44 下午
 */
enum class DecodeState {
    /**开始状态*/
    START,
    /**解码中*/
    DECODING,
    /**解码暂停*/
    PAUSE,
    /**正在快进*/
    SEEKING,
    /**解码完成*/
    FINISH,
    /**解码器释放*/
    STOP
}