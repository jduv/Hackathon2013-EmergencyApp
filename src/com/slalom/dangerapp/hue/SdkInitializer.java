package com.slalom.dangerapp.hue;

import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHBridgeSearchManager;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;

import java.util.List;

public class SdkInitializer {
    private final static String DEVICE_NAME = "dangerapp";
    private final static String PREAUTHD_USER = "dangerapp0";
    private final static String BRIDGE_IP = "10.138.116.123";
    private final PHAccessPoint accessPoint = new PHAccessPoint();
    private PHHueSDK sdk;

    public SdkInitializer() {
        this(BRIDGE_IP);
    }

    public SdkInitializer(String bridgeIp) {
        this.accessPoint.setUsername(PREAUTHD_USER);
        //this.accessPoint.setMacAddress(this.getMacAddress());
        this.accessPoint.setIpAddress(bridgeIp);
    }

    public PHHueSDK getConnectedSdk() {
        this.sdk = PHHueSDK.create();
        PHSDKListener listener = new BasicBridgeListener(sdk);
        sdk.getNotificationManager().registerSDKListener(listener);

        // Do UPNP search
        PHBridgeSearchManager sm = (PHBridgeSearchManager) this.sdk.getSDKService(PHHueSDK.SEARCH_BRIDGE);
        sm.search(true, true);

        sdk.connect(this.accessPoint);
        return sdk;
    }

    public void disconnectSdk() {
        if(this.sdk != null) {
            PHBridge bridge = this.sdk.getSelectedBridge();
            if (bridge != null) {

                if (this.sdk.isHeartbeatEnabled(bridge)) {
                    this.sdk.disableHeartbeat(bridge);
                }

                this.sdk.disconnect(bridge);
            }
        }
    }

    private final class BasicBridgeListener implements PHSDKListener {
        private final PHHueSDK sdk;

        public BasicBridgeListener(PHHueSDK sdk) {
            this.sdk = sdk;
        }

        @Override
        public void onCacheUpdated(int i, PHBridge phBridge) {}

        @Override
        public void onBridgeConnected(PHBridge phBridge) {
            this.sdk.setSelectedBridge(phBridge);
            this.sdk.enableHeartbeat(phBridge, PHHueSDK.HB_INTERVAL);
            this.sdk.getLastHeartbeat().put(phBridge.getResourceCache().getBridgeConfiguration()
                    .getIpAddress(), System.currentTimeMillis());
        }

        @Override
        public void onAuthenticationRequired(PHAccessPoint phAccessPoint) {
            this.sdk.startPushlinkAuthentication(accessPoint);
        }

        @Override
        public void onAccessPointsFound(List<PHAccessPoint> phAccessPoints) {
            if (accessPoint != null && phAccessPoints.size() > 0) {
                this.sdk.getAccessPointsFound().clear();
                this.sdk.getAccessPointsFound().addAll(phAccessPoints);
            }
        }

        @Override
        public void onError(int i, String s) {
            if(s != null) {
                // what to do
            }
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void onConnectionResumed(PHBridge phBridge) {
            this.sdk.getLastHeartbeat().put(phBridge.getResourceCache().getBridgeConfiguration().getIpAddress(),
                    System.currentTimeMillis());
            for (int i = 0; i < this.sdk.getDisconnectedAccessPoint().size(); i++) {

                if (this.sdk.getDisconnectedAccessPoint().get(i).getIpAddress().equals(
                        phBridge.getResourceCache().getBridgeConfiguration().getIpAddress())) {
                    this.sdk.getDisconnectedAccessPoint().remove(i);
                }
            }
        }

        @Override
        public void onConnectionLost(PHAccessPoint phAccessPoint) {
            if (!this.sdk.getDisconnectedAccessPoint().contains(accessPoint)) {
                this.sdk.getDisconnectedAccessPoint().add(accessPoint);
            }
        }
    }
}
