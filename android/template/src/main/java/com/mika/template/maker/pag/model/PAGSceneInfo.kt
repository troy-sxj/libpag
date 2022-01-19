package com.mika.template.maker.pag.model

import java.io.Serializable

/**
 * Author: shangxiaojian
 * Date: 2022/1/19 4:23 下午
 */
data class PAGSceneInfo(
    var textScenes: List<PAGTextScene>? = null,
    var imgScenes: List<PAGImageScene>? = null,
    var videoScenes: List<PAGVideoScene>? = null
) : Serializable {

}