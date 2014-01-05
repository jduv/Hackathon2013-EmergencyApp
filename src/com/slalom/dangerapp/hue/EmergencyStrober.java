package com.slalom.dangerapp.hue;

import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EmergencyStrober implements Runnable {
    private static final int MAX_HUE=65535;
    private static final int BLINKY_INTERVAL=1000;
    private static final int WAITING_FOR_BRIDGE = 2000;
    private boolean running = false;
    private PHHueSDK sdk;

    public EmergencyStrober(PHHueSDK sdk) {
        this.sdk = sdk;
    }

    public synchronized void terminate() {
        this.running = false;
    }

    public void run() {
        this.running = true;
        try{
            while(running) {
                PHBridge bridge = this.sdk.getSelectedBridge();
                List<PHLight> lights = bridge == null ?
                        new ArrayList<PHLight>() :
                        bridge.getResourceCache().getAllLights();
                if(bridge != null && lights.size() > 0) {
                    while(running){
                        // strobe lights
                        Random rand = new Random();
                        for(PHLight light : lights) {
                            PHLightState state = new PHLightState();
                            state.setHue(rand.nextInt(MAX_HUE));
                            bridge.updateLightState(light, state);
                        }

                        Thread.sleep(BLINKY_INTERVAL);
                    }
                } else {
                    Thread.sleep(WAITING_FOR_BRIDGE);
                }
            }
        } catch (InterruptedException exc) {
            this.terminate();
        }
    }
}
