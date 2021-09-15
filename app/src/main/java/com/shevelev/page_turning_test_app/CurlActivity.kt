package com.shevelev.page_turning_test_app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.shevelev.page_turning_lib.page_curling.CurlView
import com.shevelev.page_turning_lib.page_curling.CurlViewEventsHandler

class CurlActivity : AppCompatActivity() {
    private var curlView: CurlView? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_curl)
//        val currentPageIndex = DalFacade.Comics.getComicsById(comicsId)!!.lastViewedPageIndex

        curlView = (findViewById<View>(R.id.curl) as? CurlView)?.also {
            it.setBitmapProvider(RawResourcesBitmapProvider(this))
            it.initCurrentPageIndex(0)
            it.setBackgroundColor(Color.WHITE/*-0xdfd7d0*/)

            it.setExternalEventsHandler(object: CurlViewEventsHandler{
                override fun onPageChanged(newPageIndex: Int) {
                    // do nothing so far
                }

                override fun onHotAreaPressed(areaId: Int) {
                    // do nothing so far
                }
            })
        }

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

    companion object {
        fun start(parentActivity: Activity) {
            val intent = Intent(parentActivity, CurlActivity::class.java)
            parentActivity.startActivity(intent)
        }
    }
}
