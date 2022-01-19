package com.mika.template.media.extractor

import android.media.MediaFormat
import java.nio.ByteBuffer

/**
 * Author: shangxiaojian
 * Date: 2022/1/18 5:39 下午
 */
class VideoExtractor(path:String): IExtractor {

    private val mMediaExtractor = CommonExtractor(path)

    override fun getFormat(): MediaFormat? {
        return mMediaExtractor.getVideoFormat()
    }

    override fun readBuffer(byteBuffer: ByteBuffer): Int {
        return mMediaExtractor.readBuffer(byteBuffer)
    }

    override fun getCurrentTimestamp(): Long {
        return mMediaExtractor.getCurrentTimestamp()
    }

    override fun getSampleFlag(): Int {
        return mMediaExtractor.getSampleFlag()
    }

    override fun seek(pos: Long): Long {
        return mMediaExtractor.seek(pos)
    }

    override fun setStartPos(pos: Long) {
        return mMediaExtractor.setStartPos(pos)
    }

    override fun stop() {
        mMediaExtractor.stop()
    }
}