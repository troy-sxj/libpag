package com.mika.template.pag

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.ViewGroup
import android.widget.FrameLayout
import com.mika.template.R
import org.libpag.PAGFile
import org.libpag.PAGView

/**
 * Author: shangxiaojian
 * Date: 2022/1/18 11:20 上午
 */
class PAGFileTestActivity : AppCompatActivity() {

    private lateinit var mContainer: FrameLayout
    private lateinit var mPAGView: PAGView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pag_file)

        mContainer = findViewById(R.id.container)

        mContainer.postDelayed({
            initPAGView()
            loadPAGFile()
        }, 500)
    }

    private fun initPAGView() {
        mPAGView = PAGView(this)
        mPAGView.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        mContainer.addView(mPAGView)
    }

    private fun loadPAGFile() {
        val pagFile = PAGFile.Load(assets, "resources/timestretch/repeat.pag")
        mPAGView.composition = pagFile

        mPAGView.setRepeatCount(-1)
        mPAGView.play()
    }
}