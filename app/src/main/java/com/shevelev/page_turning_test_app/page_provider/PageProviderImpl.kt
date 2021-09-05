package com.shevelev.page_turning_test_app.page_provider

import android.content.Context
import android.graphics.*
import android.util.Log
import com.shevelev.page_turning_lib.page_curling.CurlPage
import com.shevelev.page_turning_lib.page_curling.PageProvider
import com.shevelev.page_turning_lib.page_curling.PageSide
import java.io.IOException

/**
 * Provide textures for pages and update pages
 */
internal class PageProviderImpl(context: Context) : PageProvider {

    private val repository: BitmapRepository
    override val pageCount: Int
        get() = repository.pageCount

    /**
     * Create bitmap for drawing
     * @param width
     * @param height
     * @param index
     * @return
     */
    @Throws(IOException::class)
    private fun loadBitmap(width: Int, height: Int, index: Int): Bitmap {
        val bitmap = repository.getByIndex(index, width, height)
        val b = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565) // For memory saving
        b.eraseColor(-0x1)
        val c = Canvas(b)
        val margin = 7
        val border = 3 // Thin frame around image
        val r = Rect(margin, margin, width - margin, height - margin) // Image's frame
        var imageWidth = r.width() - border * 2 // Scale image with saving proportions
        var imageHeight = imageWidth * bitmap!!.height / bitmap.width
        if (imageHeight > r.height() - border * 2) { // Inscribe image in draw
            imageHeight = r.height() - border * 2
            imageWidth = imageHeight * bitmap.width / bitmap.height
        }
        r.left += (r.width() - imageWidth) / 2 - border
        r.right = r.left + imageWidth + border + border // Place image's rect on center
        r.top += (r.height() - imageHeight) / 2 - border
        r.bottom = r.top + imageHeight + border + border
        val p = Paint()
        p.color = -0x3f3f40 // Draw violet frame around image
        c.drawRect(r, p)
        r.left += border
        r.right -= border
        r.top += border
        r.bottom -= border
        //        d.setBounds(r);
//        d.draw(c);
        c.drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), r, p)
        return b
    }

    /**
     * Set bitmap for page - front and back (may be texture or solid color)
     * @param page
     * @param width
     * @param height
     * @param index
     */
    override fun updatePage(page: CurlPage, width: Int, height: Int, index: Int) {
        var bmp: Bitmap? = null
        try {
            bmp = loadBitmap(width, height, index)
        } catch (e: Exception) {
            Log.e("CV", "exception", e)
        }
        page.setTexture(bmp, PageSide.Front)
        page.setTexture(bmp, PageSide.Back)
        page.setColor(Color.argb(50, 255, 255, 255), PageSide.Back)
    }

    init {
        repository = BitmapRepositoryImpl(context)
    }
}
