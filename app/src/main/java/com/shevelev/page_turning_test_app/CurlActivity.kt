package com.shevelev.page_turning_test_app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.shevelev.comics_viewer.ui.activities.view_comics.CurlView
import com.shevelev.page_turning_test_app.page_provider.PageProviderImpl

class CurlActivity : AppCompatActivity() {
    private var curlView: CurlView? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_curl)
//        val currentPageIndex = DalFacade.Comics.getComicsById(comicsId)!!.lastViewedPageIndex
        curlView = findViewById<View>(R.id.curl) as CurlView
        curlView!!.setPageProvider(PageProviderImpl(this))
        curlView!!.initCurrentPageIndex(0)
        curlView!!.setBackgroundColor(-0xdfd7d0)

        curlView!!.setCallbackHandlers(
            { pageIndex: Int -> onPageChanged(pageIndex) },
            { onShowMenu() }
        )

        // This is something somewhat experimental. Before uncommenting next
// line, please see method comments in CurlView.
// curlView.setEnableTouchPressure(true);

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
    }

    public override fun onPause() {
        super.onPause()
        curlView!!.onPause()
    }

    public override fun onResume() {
        super.onResume()
        curlView!!.onResume()
    }

    private fun onPageChanged(currentPageIndex: Int) {
    }

    private fun onShowMenu() {
    }

    companion object {
        fun start(parentActivity: Activity) {
            val intent = Intent(parentActivity, CurlActivity::class.java)
            parentActivity.startActivity(intent)
        }
    }
}
