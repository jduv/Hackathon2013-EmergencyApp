package com.slalom.dangerapp.views;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;
import com.slalom.dangerapp.R;
import com.slalom.dangerapp.hue.EmergencyStrober;
import com.slalom.dangerapp.hue.SdkInitializer;

import java.util.ArrayList;
import java.util.List;

public class HomeView extends Activity {
    private boolean blinky = false;
    private SdkInitializer initializer;
    private static final int BLUE = 46920;
    private static final int GREEN = 25500;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.home);

        this.initializer = new SdkInitializer(getApplicationContext());
        final PHHueSDK sdk = initializer.getConnectedSdk();
        final EmergencyStrober strober = new EmergencyStrober(sdk);
        final ThreadWrapper wrapper = new ThreadWrapper();

        final Button initLights = (Button) findViewById(R.id.init_lights);
        initLights.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                clearLights(sdk, BLUE);
            }
        });

        final Button blinkyButton = (Button) findViewById(R.id.blinkyon);
        blinkyButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                if(!blinky) {
                    blinky = true;
                    Thread t = new Thread(strober);
                    wrapper.setWrapped(t);
                    t.start();
                }
            }
        });

        final Button noBlinkyButton = (Button) findViewById(R.id.blinkyoff);
        noBlinkyButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    if(blinky && wrapper.getWrapped() != null) {
                        blinky = false;
                        strober.terminate();
                        wrapper.getWrapped().join();
                        clearLights(sdk, GREEN);
                    }
                } catch (InterruptedException ie) {
                    // Nothing to do really.
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        this.initializer.disconnectSdk();
        super.onDestroy();
    }

    private void clearLights(PHHueSDK sdk, int color) {
        PHBridge bridge = sdk.getSelectedBridge();
        List<PHLight> lights = bridge == null ?
                new ArrayList<PHLight>() :
                bridge.getResourceCache().getAllLights();
        if(bridge != null && lights.size() > 0) {
            // strobe lights
            for(PHLight light : lights) {
                PHLightState state = new PHLightState();
                state.setOn(true);
                state.setHue(color);
                state.setBrightness(200);
                state.setSaturation(254);
                bridge.updateLightState(light, state);
            }
        }
    }

    private class ThreadWrapper {
        private Thread wrapped;

        public Thread getWrapped() {
            return this.wrapped;
        }

        public void setWrapped(Thread toWrap) {
            this.wrapped = toWrap;
        }
    }
}