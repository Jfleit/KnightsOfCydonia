package com.example.bkozik.butterfliesandhurricanes;



import java.lang.ref.WeakReference;

import android.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.choosemuse.libmuse.MuseManagerAndroid;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseConnectionListener;
import com.choosemuse.libmuse.MuseConnectionPacket;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;
import com.choosemuse.libmuse.MuseDataPacketType;
import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.Eeg;
import com.choosemuse.libmuse.Accelerometer;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.bluetooth.BluetoothAdapter;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "Butterflies";
    private MuseManagerAndroid manager;
    private Muse muse;
    private ConnectionListener connectionListener;
    private DataListener dataListener;

    private final double[] eegBuffer = new double[6];
    private boolean eegStale;
    private final double[] accelBuffer = new double[3];
    private boolean accelStale;

    private final Handler handler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        manager = MuseManagerAndroid.getInstance();
       // LogManager.instance().setLogListener(new AndroidLogListener());
        manager.setContext(this);
        manager.startListening();




        //Log.i(TAG, "LibMuse version=" + LibmuseVersion.instance().getString());

        WeakReference<MainActivity> weakActivity =
                new WeakReference<MainActivity>(this);
        // Register a listener to receive connection state changes.
        connectionListener = new ConnectionListener(weakActivity);
        // Register a listener to receive data from a Muse.
        dataListener = new DataListener(weakActivity);
        ensurePermissions();
        setContentView(R.layout.activity_main);
        TextView textView = (TextView) findViewById(R.id.textView5);
        textView.setText("" + eegBuffer[0]);
        initUI();
        handler.post(tickUi);
    }

    private void ensurePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            DialogInterface.OnClickListener buttonListener =
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which){
                    dialog.dismiss();
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
                }
            };
            AlertDialog introDialog = new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("Tap accept")
                    .setPositiveButton("I understand", buttonListener)
                    .create();
            introDialog.show();
        }
    }

    public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {

        final ConnectionState current = p.getCurrentConnectionState();

        // Format a message to show the change of connection state in the UI.
        final String status = p.getPreviousConnectionState() + " -> " + current;

        // Update the UI with the change in connection state.
        handler.post(new Runnable() {
            @Override
            public void run() {

                final TextView statusText = (TextView) findViewById(R.id.con_status);
                statusText.setText(status);


                // If we haven't yet connected to the headband, the version information
                // will be null.  You have to connect to the headband before either the
                // MuseVersion or MuseConfiguration information is known.
            }
        });

        if (current == ConnectionState.DISCONNECTED) {
            this.muse = null;
        }
    }

    public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {

        // valuesSize returns the number of data values contained in the packet.
        final long n = p.valuesSize();
        switch (p.packetType()) {
            case EEG:
                assert(eegBuffer.length >= n);
                getEegChannelValues(p);
                eegStale = true;
                break;
            case ACCELEROMETER:
                assert(accelBuffer.length >= n);
                getAccelValues(p);
                accelStale = true;
                break;
            case BATTERY:
            case DRL_REF:
            case QUANTIZATION:
            default:
                break;
        }
    }

    public void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {
    }
    private void getEegChannelValues(MuseDataPacket p) {
        eegBuffer[0] = p.getEegChannelValue(Eeg.EEG1);
        eegBuffer[1] = p.getEegChannelValue(Eeg.EEG2);
        eegBuffer[2] = p.getEegChannelValue(Eeg.EEG3);
        eegBuffer[3] = p.getEegChannelValue(Eeg.EEG4);
        eegBuffer[4] = p.getEegChannelValue(Eeg.AUX_LEFT);
        eegBuffer[5] = p.getEegChannelValue(Eeg.AUX_RIGHT);
    }


    private void getAccelValues(MuseDataPacket p) {
        accelBuffer[0] = p.getAccelerometerValue(Accelerometer.X);
        accelBuffer[1] = p.getAccelerometerValue(Accelerometer.Y);
        accelBuffer[2] = p.getAccelerometerValue(Accelerometer.Z);
    }


    //Todo: we need an initUI method, but with our stuff, not theirs
    private void initUI() {
        setContentView(R.layout.activity_main);
    }

    /**
     * The runnable that is used to update the UI at 60Hz.
     *
     * We update the UI from this Runnable instead of in packet handlers
     * because packets come in at high frequency -- 220Hz or more for raw EEG
     * -- and it only makes sense to update the UI at about 60fps. The update
     * functions do some string allocation, so this reduces our memory
     * footprint and makes GC pauses less frequent/noticeable.
     */
    private final Runnable tickUi = new Runnable() {
        @Override
        public void run() {
            if (eegStale) {
                updateEeg();
            }
            if (accelStale) {
                updateAccel();
            }
            handler.postDelayed(tickUi, 1000 / 5);
        }
    };

    //Todo change this to our shit
    private void updateAccel() {
        TextView acc_x = (TextView)findViewById(R.id.acc_x);
        TextView acc_y = (TextView)findViewById(R.id.acc_y);
        TextView acc_z = (TextView)findViewById(R.id.acc_z);
        acc_x.setText(String.format("%6.2f", accelBuffer[0]));
        acc_y.setText(String.format("%6.2f", accelBuffer[1]));
        acc_z.setText(String.format("%6.2f", accelBuffer[2]));
    }

    private void updateEeg() {
        TextView tp9 = (TextView)findViewById(R.id.eeg_tp9);
        TextView fp1 = (TextView)findViewById(R.id.eeg_af7);
        TextView fp2 = (TextView)findViewById(R.id.eeg_af8);
        TextView tp10 = (TextView)findViewById(R.id.eeg_tp10);
        tp9.setText(String.format("%6.2f", eegBuffer[0]));
        fp1.setText(String.format("%6.2f", eegBuffer[1]));
        fp2.setText(String.format("%6.2f", eegBuffer[2]));
        tp10.setText(String.format("%6.2f", eegBuffer[3]));
    }



    class ConnectionListener extends MuseConnectionListener {
        final WeakReference<MainActivity> activityRef;

        ConnectionListener(final WeakReference<MainActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
            activityRef.get().receiveMuseConnectionPacket(p, muse);
        }
    }

    class DataListener extends MuseDataListener {
        final WeakReference<MainActivity> activityRef;

        DataListener(final WeakReference<MainActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
            activityRef.get().receiveMuseDataPacket(p, muse);
        }

        @Override
        public void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {
            activityRef.get().receiveMuseArtifactPacket(p, muse);
        }
    }
}
