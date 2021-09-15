package com.shevelev.page_turning_lib.page_curling.textures_manager

import android.graphics.Bitmap

/**
 * Created by Syleiman on 26.11.2015.
 */
interface BitmapRepository {
    /**
     * @param index index of page
     * @return
     */
    fun getByIndex(index: Int, viewAreaWidth: Int, viewAreaHeight: Int): Bitmap?

    val pageCount: Int
}