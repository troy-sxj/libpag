package com.mika.template.media.decoder

import android.media.MediaCodec
import java.nio.ByteBuffer

/**
 * Author: shangxiaojian
 * Date: 2022/1/18 5:44 下午
 */
class Frame {

    var buffer: ByteBuffer?=null

    var bufferInfo = MediaCodec.BufferInfo()
        private set

    fun setBufferInfo(info: MediaCodec.BufferInfo) {
        bufferInfo.set(info.offset, info.size, info.presentationTimeUs, info.flags)
    }
}