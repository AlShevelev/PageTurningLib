package com.shevelev.comics_viewer.ui.activities.view_comics

/**
 * Provider for feeding 'book' with bitmaps which are used for rendering
 * pages.
 */
interface IPageProvider {
    /**
     * Return number of pages available.
     */
    val pageCount: Int

    /**
     * Called once new bitmaps/textures are needed. Width and height are in
     * pixels telling the size it will be drawn on screen and following them
     * ensures that aspect ratio remains. But it's possible to return bitmap
     * of any size though. You should use provided CurlPage for storing page
     * information for requested page number.<br></br>
     * <br></br>
     * Index is a number between 0 and getBitmapCount() - 1.
     */
    fun updatePage(page: CurlPage, width: Int, height: Int, index: Int)
}