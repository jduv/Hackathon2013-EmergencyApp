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
import android.widget.ImageButton;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.content.Context;
import android.widget.ImageView;
import android.widget.LinearLayout;

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

        // pass ip into sdk initializer if you want to control it
        this.initializer = new SdkInitializer();
        final PHHueSDK sdk = initializer.getConnectedSdk();
        final EmergencyStrober strober = new EmergencyStrober(sdk);
        final ThreadWrapper wrapper = new ThreadWrapper();

        final MediaPlayer player = MediaPlayer.create(this, R.raw.help);

        final ImageButton medicalButton = (ImageButton) findViewById(R.id.medical_button);
        final ImageButton fireButton = (ImageButton) findViewById(R.id.fire_button);
        final ImageButton emergencyButton = (ImageButton) findViewById(R.id.emergency_button);
        final ImageButton clearButton = (ImageButton) findViewById(R.id.clear_button);

        final ImageView app_logo = (ImageView)findViewById(R.id.app_logo);

        final LinearLayout buttonHolder = (LinearLayout)findViewById(R.id.button_holder);
        final LinearLayout dialogHolder = (LinearLayout)findViewById(R.id.dialog_holder);

        buttonHolder.setVisibility(View.VISIBLE);
        dialogHolder.setVisibility(View.GONE);

        medicalButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                if(!blinky && !wrapper.isThreadAlive()) {
                    blinky = true;
                    Thread t = new Thread(strober);
                    wrapper.setWrapped(t);
                    t.start();

                    AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 20, 0);
                    player.start();
                }

                buttonHolder.setVisibility(View.GONE);
                dialogHolder.setVisibility(View.VISIBLE);
            }
        });


        clearButton.setOnClickListener(new View.OnClickListener() {
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

                buttonHolder.setVisibility(View.VISIBLE);
                dialogHolder.setVisibility(View.GONE);
            }
        });

        app_logo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                clearLights(sdk, BLUE);
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

        public boolean isThreadAlive() {
            return this.wrapped != null && this.wrapped.isAlive();
        }
    }
}