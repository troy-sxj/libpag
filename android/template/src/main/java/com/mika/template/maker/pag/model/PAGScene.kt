package com.mika.template.maker.pag.model

import java.io.Serializable

/**
 * Author: shangxiaojian
 * Date: 2022/1/19 11:35 上午
 */
sealed class PAGScene : Serializable

data class PAGTextScene(val originText: String, var modifiedText: String? = null) : PAGScene()

data class PAGImageScene(val imageIndex: Int, var imgPath: String? = null) : PAGScene()

data class PAGVideoScene(var videoPath: String) : PAGScene()