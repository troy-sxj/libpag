package com.mika.template.pag

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import com.mika.template.R
import com.mika.template.maker.pag.PAGFileReader
import com.mika.template.maker.pag.model.PAGSceneInfo
import com.mika.template.maker.pag.player.MultiScenePlayer
import org.libpag.PAGFile

/**
 * Author: shangxiaojian
 * Date: 2022/1/19 1:42 下午
 */
class PAGPlayerActivity : AppCompatActivity() {

    private lateinit var mSurfaceView: SurfaceView
    private lateinit var multiScenePlayer: MultiScenePlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pag_player)

        mSurfaceView = findViewById(R.id.surfaceView)
        mSurfaceView.holder.addCallback(object: SurfaceHolder.Callback{
            override fun surfaceCreated(holder: SurfaceHolder?) {

            }

            override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
                holder?.surface?.let { multiScenePlayer.setSurface(it) }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {
                multiScenePlayer.release()
            }
        })
        multiScenePlayer = MultiScenePlayer(this)
    }

    fun loadPAG(view: View) {
        PAGFileReader(this,
            object : PAGFileReader.PAGFileLoaderCallback {
                override fun onPAGLoaded(pageFile: PAGFile?, sceneInfo: PAGSceneInfo) {
                    showAndChoose(pageFile, sceneInfo)
                }

                override fun onPAGLoadFailed(errCode: Int, errMsg: String?) {

                }

            }).openAssertFile("resources/md5/yanzhixiu.pag")
    }

    private fun showAndChoose(pageFile: PAGFile?, sceneInfo: PAGSceneInfo) {
        sceneInfo.textScenes?.let{
            it[0].modifiedText = "买"
            it[1].modifiedText = "3C"
            it[2].modifiedText = "来京东"
            it[3].modifiedText = "就对了"
            it[4].modifiedText = "JD"
            it[5].modifiedText = "in"
            it[6].modifiedText = "All"
        }
        multiScenePlayer.setDataSource(sceneInfo, pageFile!!)
        multiScenePlayer.start()
    }
}