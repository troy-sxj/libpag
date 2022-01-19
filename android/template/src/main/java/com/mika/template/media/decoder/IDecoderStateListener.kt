package com.mika.template.media.decoder

/**
 * Author: shangxiaojian
 * Date: 2022/1/18 5:40 下午
 */
interface IDecoderStateListener {

    fun decoderPrepare(decodeJob: BaseDecoder?)

    fun decoderReady(decodeJob: BaseDecoder?)

    fun decoderRunning(decodeJob: BaseDecoder?)

    fun decoderPause(decodeJob: BaseDecoder?)

    fun decodeOneFrame(decodeJob: BaseDecoder?, frame: Frame)

    fun decoderFinish(decodeJob: BaseDecoder?)

    fun decoderDestroy(decodeJob: BaseDecoder?)

    fun decoderError(decodeJob: BaseDecoder?, msg: String)
}