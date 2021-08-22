package com.shevelev.comics_viewer.ui.activities.view_comics

import android.graphics.RectF
import com.shevelev.comics_viewer.common.structs.SizeF

/**
 * Information about renderer view
 * [viewAreaSize] Size of view area [px]
 */
class RendererViewInfo(val viewRect: RectF, val viewAreaSize: SizeF)