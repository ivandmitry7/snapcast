package de.badaix.snapcast;

import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private static final String TAG = "Main";

    private Button buttonScan;
    private Button buttonStart;
    private Button buttonStop;
    private TextView tvHost;
    private CheckBox cbScreenWakelock;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager mNsdManager = null;
    private String host = "";
    private int port = 1704;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonScan = (Button) findViewById(R.id.buttonScan);
        buttonStart = (Button) findViewById(R.id.buttonStart);
        buttonStop = (Button) findViewById(R.id.buttonStop);
        tvHost = (TextView) findViewById(R.id.tvHost);
        cbScreenWakelock = (CheckBox) findViewById(R.id.cbScreenWakelock);
        cbScreenWakelock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b)
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });

        buttonScan.setOnClickListener(this);
        buttonStart.setOnClickListener(this);
        buttonStop.setOnClickListener(this);
        initializeDiscoveryListener();
    }

    private void start() {
        Intent i = new Intent(this, SnapclientService.class);

        i.putExtra(SnapclientService.EXTRA_HOST, host);
        i.putExtra(SnapclientService.EXTRA_PORT, port);

        startService(i);
    }

    private void stop() {
        stopService(new Intent(this, SnapclientService.class));
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onClick(View view) {
        if (view == buttonStart) {
            Toast.makeText(this, "Start", Toast.LENGTH_SHORT).show();
            start();
        } else if (view == buttonStop) {
            Toast.makeText(this, "Stop", Toast.LENGTH_SHORT).show();
            stop();
        } else if (view == buttonScan) {
            Toast.makeText(this, "Scan", Toast.LENGTH_SHORT).show();
            initializeDiscoveryListener();
        }
    }

    public void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            private void setStatus(final String text) {
                Log.e(TAG, text);
                tvHost.post(new Runnable() {
                    @Override
                    public void run() {
                        tvHost.setText("Host: " + text);
                    }
                });
            }

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
                setStatus("Host: searching for a Snapserver");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                Log.d(TAG, "Service discovery success" + service);
                mNsdManager.resolveService(service, new NsdManager.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {

                    }

                    @Override
                    public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
                        Log.d(TAG, "resolved: " + nsdServiceInfo);
                        host = nsdServiceInfo.getHost().getCanonicalHostName();
                        port = nsdServiceInfo.getPort();
                        setStatus(host + ":" + port);
                        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
                    }
                });
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost" + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                setStatus("Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                setStatus("Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };

        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        mNsdManager.discoverServices("_snapcast._tcp.", NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }
}
