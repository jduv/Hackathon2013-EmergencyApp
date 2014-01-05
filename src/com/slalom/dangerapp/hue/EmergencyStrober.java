package com.slalom.dangerapp.hue;

import android.util.Log;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

import java.util.ArrayList;
import java.util.List;

public class EmergencyStrober implements Runnable {
    private static final int MAX_HUE=65535;
    private static final int BLINKY_INTERVAL=1500;
    private static final int WAITING_FOR_BRIDGE = 2000;
    private static final int RED = 0;
    private volatile boolean running = false;
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
                    LightNode head = this.convertToCircularLinkedList(lights);
                    this.initEmergencyState(bridge, head);
                    Thread.sleep(BLINKY_INTERVAL);
                    while(running){
                        // strobe lights
                        updateEmergencyStates(bridge, head);
                        Thread.sleep(BLINKY_INTERVAL);
                    }
                } else {
                    Thread.sleep(WAITING_FOR_BRIDGE);
                }
            }
        } catch (InterruptedException exc) {
            if(!this.running) {
                this.terminate();
                Log.w("JDUV", "Terminated due to thread exception");
            }
        }
    }

    private void initEmergencyState(PHBridge bridge, LightNode head) {
        LightNode current = head;
        do {
            PHLightState state = new PHLightState();
            state.setHue(RED);

            if(current == head)
            {
                state.setOn(false);
            }

            bridge.updateLightState(current.getLight(), state);
            current = current.getNext();

        } while(current != head);
    }

    private void updateEmergencyStates(PHBridge bridge, LightNode head) {
        LightNode current = head;
        LightNode previous = null;

        do {
            previous = current == null ? null : current.getPrevious();

            if(previous != null && !previous.getLight().getLastKnownLightState().isOn()) {
                PHLightState currentState = new PHLightState();
                PHLightState previousState = new PHLightState();
                previousState.setOn(true);
                currentState.setOn(false);
                current.setState(currentState);
                previous.setState(previousState);
            }

            current = current.getNext();

        } while(current != head);

        this.commitStateChanges(bridge, head);
        Log.w("JDUV", "Done updating state");
    }

    private void commitStateChanges(PHBridge bridge, LightNode head) {
        LightNode current = head;
        do {
            if(current.getState() != null) {
                bridge.updateLightState(current.getLight(), current.getState());
                current.setState(null);
            }
            current = current.getNext();
        } while (current != head);
        Log.w("JDUV", "Committed state changes");
    }

    private LightNode convertToCircularLinkedList(List<PHLight> lights) {
        LightNode head = new LightNode(lights.get(0));
        LightNode secondLight = new LightNode(lights.get(2));
        LightNode thirdLight = new LightNode(lights.get(1));

        head.setNext(secondLight);
        head.setPrevious(thirdLight);

        secondLight.setNext(thirdLight);
        secondLight.setPrevious(head);

        thirdLight.setNext(head);
        thirdLight.setPrevious(secondLight);

        return head;
    }

    private class LightNode {
        private LightNode next;
        private LightNode previous;
        private PHLight light;
        private PHLightState state;

        public LightNode(PHLight light) {
            this.light = light;
        }

        public LightNode getNext() {
            return this.next;
        }

        public void setNext(LightNode next) {
            this.next = next;
        }

        public LightNode getPrevious() {
            return this.previous;
        }

        public void setPrevious(LightNode previous) {
            this.previous = previous;
        }

        public PHLight getLight() {
            return this.light;
        }

        public PHLightState getState() {
            return this.state;
        }

        public void setState(PHLightState toSet) {
            this.state = toSet;
        }
    }
}
