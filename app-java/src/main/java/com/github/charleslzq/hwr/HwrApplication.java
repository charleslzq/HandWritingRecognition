package com.github.charleslzq.hwr;

import android.app.Application;
import android.util.Log;

import com.github.charleslzq.hwr.hanvon.HanvonApiHolder;
import com.github.charleslzq.hwr.hanvon.HanvonWritingApiAdapter;
import com.github.charleslzq.hwr.hanvon.MultiHanvonRecognizer;
import com.github.charleslzq.hwr.hanvon.SingleHanvonRecognizer;
import com.github.charleslzq.hwr.hicloud.HciCloudRecognizer;
import com.github.charleslzq.hwr.lookup.HanziLookupRecognizer;
import com.github.charleslzq.hwr.view.HWREngine;
import com.github.charleslzq.hwr.view.HandWritingRecognizer;
import com.github.charleslzq.hwr.view.RecognizerBuilder;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class HwrApplication extends Application {
    private static final String TAG = "HwrApplication";
    private HciCloudRecognizer hciCloudRecognizer = null;

    @Override
    public void onCreate() {
        super.onCreate();
        HanvonApiHolder hanvonApiHolder = new HanvonApiHolder("http://api.hanvon.com");
        final HanvonWritingApiAdapter apiAdapter = new HanvonWritingApiAdapter(hanvonApiHolder.getHandWritingApi());
        final String ip = "101.81.231.193";
        final String key = "fa43375b-4756-44f8-aa8a-251c7c8fe870";

        HWREngine.register("hanvon-m", new RecognizerBuilder() {
            @Override
            public HandWritingRecognizer build() {
                return new MultiHanvonRecognizer(apiAdapter, ip, key);
            }
        });
        HWREngine.register("hanvon-s", new RecognizerBuilder() {
            @Override
            public HandWritingRecognizer build() {
                return new SingleHanvonRecognizer(apiAdapter, ip, key);
            }
        });
        HWREngine.register("lookup", new RecognizerBuilder() {
            @Override
            public HandWritingRecognizer build() {
                try {
                    InputStream databaseInput = getAssets().open("mmah.json");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(databaseInput));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    return new HanziLookupRecognizer(sb.toString());
                } catch (Exception e) {
                    Log.e(TAG, "Error init lookup engine", e);
                    return null;
                }
            }
        });
        HWREngine.register("hcicloud", new RecognizerBuilder() {
            @Override
            public HandWritingRecognizer build() {
                hciCloudRecognizer = new HciCloudRecognizer(
                        HwrApplication.this,
                        "43c5a7be629fc2e7f0c2d792a04c367d",
                        "545d5463"
                );
                return hciCloudRecognizer;
            }
        });
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (hciCloudRecognizer != null) {
            hciCloudRecognizer.release();
        }
    }

}
