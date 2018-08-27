package com.github.charleslzq.hwr

import android.app.Application
import com.github.charleslzq.hwr.hanvon.HanvonApiHolder
import com.github.charleslzq.hwr.hanvon.HanvonWritingApiAdapter
import com.github.charleslzq.hwr.hanvon.MultiHanvonRecognizer
import com.github.charleslzq.hwr.hanvon.SingleHanvonRecognizer
import com.github.charleslzq.hwr.hicloud.HciCloudRecognizer
import com.github.charleslzq.hwr.lookup.HanziLookupRecognizer
import com.github.charleslzq.hwr.view.HWREngine

class HwrApplication : Application() {

    private var hciCloudRecognizer: HciCloudRecognizer? = null

    override fun onCreate() {
        super.onCreate()
        val hanvonApi = HanvonApiHolder("http://api.hanvon.com")
        val ip = "101.81.231.193"
        val key = "fa43375b-4756-44f8-aa8a-251c7c8fe870"
        HWREngine.register("hanvon-m") {
            MultiHanvonRecognizer(
                    HanvonWritingApiAdapter(hanvonApi.handWritingApi),
                    ip,
                    key
            )
        }
        HWREngine.register("hanvon-s") {
            SingleHanvonRecognizer(
                    HanvonWritingApiAdapter(hanvonApi.handWritingApi),
                    ip,
                    key
            )
        }
        HWREngine.register("lookup") {
            assets.open("mmah.json").bufferedReader().useLines {
                HanziLookupRecognizer(it.joinToString(""))
            }
        }
        HWREngine.register("hcicloud") {
            HciCloudRecognizer(
                    this,
                    "43c5a7be629fc2e7f0c2d792a04c367d",
                    "545d5463"
            ).also {
                hciCloudRecognizer = it
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        hciCloudRecognizer?.release()
    }
}