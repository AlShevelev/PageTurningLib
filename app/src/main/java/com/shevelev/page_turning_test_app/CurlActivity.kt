package com.shevelev.page_turning_test_app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Size
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shevelev.page_turning_lib.page_curling.CurlView
import com.shevelev.page_turning_lib.page_curling.CurlViewEventsHandler
import com.shevelev.page_turning_lib.page_curling.textures_manager.PageLoadingEventsHandler
import com.shevelev.page_turning_lib.user_actions_managing.Area
import com.shevelev.page_turning_lib.user_actions_managing.Point

class CurlActivity : AppCompatActivity() {
    private var curlView: CurlView? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_curl)

        curlView = (findViewById<View>(R.id.curl) as? CurlView)?.also {
            it.setBitmapProvider(RawResourcesBitmapProvider(this))
            it.initCurrentPageIndex(intent.getIntExtra(START_PAGE, 0))
            it.setBackgroundColor(Color.WHITE)
            it.setHotAreas(listOf(Area(0, Point(0, 0), Size(100, 100))))

            it.setExternalEventsHandler(object: CurlViewEventsHandler {
                override fun onPageChanged(newPageIndex: Int) {
                    // do nothing so far
                }

                override fun onHotAreaPressed(areaId: Int) {
                    it.setCurrentPageIndex(2)
                    Toast.makeText(this@CurlActivity, "We've moved to the page with index 2", Toast.LENGTH_SHORT).show()
                }
            })

            it.setOnPageLoadingListener(object: PageLoadingEventsHandler {
                override fun onLoadingStarted() {
                    findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE
                }

                override fun onLoadingCompleted() {
                    findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                }

                override fun onLoadingError() {
                    Toast.makeText(this@CurlActivity, R.string.generalError, Toast.LENGTH_SHORT).show()
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
        private const val START_PAGE = "START_PAGE"

        fun start(parentActivity: Activity, startPage: Int) {
            val intent = Intent(parentActivity, CurlActivity::class.java).putExtra(START_PAGE, startPage)
            parentActivity.startActivity(intent)
        }
    }
}
