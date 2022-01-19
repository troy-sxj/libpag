package com.mika.template.media.extractor

import android.media.MediaFormat
import java.nio.ByteBuffer

/**
 * Author: shangxiaojian
 * Date: 2022/1/18 5:38 下午
 */
interface IExtractor {

    fun getFormat(): MediaFormat?

    fun readBuffer(byteBuffer: ByteBuffer): Int

    fun getCurrentTimestamp(): Long

    fun getSampleFlag(): Int

    fun seek(pos: Long): Long

    fun setStartPos(pos: Long)

    fun stop()
}