package com.mika.template.pag

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.SurfaceView
import android.view.View
import com.mika.template.R
import com.mika.template.maker.pag.PAGFileReader
import com.mika.template.maker.pag.model.PAGSceneInfo

/**
 * Author: shangxiaojian
 * Date: 2022/1/19 1:42 下午
 */
class PAGPlayerActivity : AppCompatActivity() {

    private lateinit var mSurfaceView: SurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pag_player)

        mSurfaceView = findViewById(R.id.surfaceView)
    }

    fun loadPAG(view: View) {
        PAGFileReader(this,
            object : PAGFileReader.PAGFileLoaderCallback {
                override fun onPAGLoaded(sceneInfo: PAGSceneInfo) {
                    showAndChoose(sceneInfo)
                }

                override fun onPAGLoadFailed(errCode: Int, errMsg: String?) {

                }

            }).openAssertFile("resources/md5/yanzhixiu.pag")
    }

    private fun showAndChoose(sceneInfo: PAGSceneInfo) {

    }
}