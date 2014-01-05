package com.slalom.dangerapp.views;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.slalom.dangerapp.R;
import com.slalom.dangerapp.hue.EmergencyStrober;
import com.slalom.dangerapp.hue.SdkInitializer;

public class HomeView extends Activity {
    private boolean blinky = false;
    private SdkInitializer initializer;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.home);

        this.initializer = new SdkInitializer(getApplicationContext());
        PHHueSDK sdk = initializer.getConnectedSdk();
        final EmergencyStrober strober = new EmergencyStrober(sdk);
        final Thread stroberThread = new Thread(strober);

        final Button blinkyButton = (Button) findViewById(R.id.blinkyon);
        blinkyButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                if(!blinky) {
                    blinky = true;
                    stroberThread.start();
                }
            }
        });

        final Button noBlinkyButton = (Button) findViewById(R.id.blinkyoff);
        noBlinkyButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    if(blinky) {
                        blinky = false;
                        strober.terminate();
                        stroberThread.join();
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
}