package com.example.bkozik.butterfliesandhurricanes;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.choosemuse.libmuse.Accelerometer;
import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.Eeg;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseConnectionListener;
import com.choosemuse.libmuse.MuseConnectionPacket;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;
import com.choosemuse.libmuse.MuseDataPacketType;
import com.choosemuse.libmuse.MuseListener;
import com.choosemuse.libmuse.MuseManagerAndroid;

import java.lang.ref.WeakReference;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnClickListener{

    private final String TAG = "Butterflies";
    private MuseManagerAndroid manager;
    private Muse muse;
    private ConnectionListener connectionListener;
    private DataListener dataListener;

    private ArrayAdapter<String> spinnerAdapter;

    private final double[] eegBuffer = new double[6];
    private boolean eegStale;
    private final double[] accelBuffer = new double[3];
    private boolean accelStale;
    private boolean shouldIBeDoingColors = false;

    private final Handler handler = new Handler();
    Intent intent;
    Button clickButton;
    String hex1;
    String hex2;
    String hex3;
    String hex4;
    int hexRed;
    int hexGreen;
    int hexBlue;
    View bigRect;
    Color hexColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        manager = MuseManagerAndroid.getInstance();

       // LogManager.instance().setLogListener(new AndroidLogListener());
        manager.setContext(this);
        //Log.i(TAG, "LibMuse version=" + LibmuseVersion.instance().getString());

        WeakReference<MainActivity> weakActivity =
                new WeakReference<MainActivity>(this);
        // Register a listener to receive connection state changes.
        connectionListener = new ConnectionListener(weakActivity);
        // Register a listener to receive data from a Muse.
        dataListener = new DataListener(weakActivity);
        manager.setMuseListener(new MuseL(weakActivity));
        ensurePermissions();

        initUI();

        handler.post(tickUi);

        /*
        setContentView(R.layout.activity_main);
        TextView textView = (TextView) findViewById(R.id.textView5);
        textView.setText("" + eegBuffer[0]);
         */

        clickButton = (Button) findViewById(R.id.next);
        clickButton.setOnClickListener( new OnClickListener() {

            @Override
            public void onClick(View v) {
                bigRect = findViewById(R.id.bigRect1);
                bigRect.setVisibility(View.VISIBLE);
                shouldIBeDoingColors = true;
            }
        });


        String hex1;
    }

  /*  public void goToColors(View view)
    {
        intent = new Intent(this, colorActivity.class);
        intent.putExtra("EEG Array", eegBuffer);
        startActivity(intent);

    }
*/
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

    public boolean isBluetoothEnabled() {
        return BluetoothAdapter.getDefaultAdapter().isEnabled();
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

    protected void onPause() {
        super.onPause();
        // It is important to call stopListening when the Activity is paused
        // to avoid a resource leak from the LibMuse library.
        manager.stopListening();
    }
    //MuseArtifactPacket museArtifactPacket;
    public void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {
        //museArtifactPacket = p;
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

    public void museListChanged() {
        final List<Muse> list = manager.getMuses();
        spinnerAdapter.clear();
        for (Muse m : list) {
            spinnerAdapter.add(m.getName() + " - " + m.getMacAddress());
        }
    }


    //Todo: we need an initUI method, but with our stuff, not theirs
    private void initUI() {
        setContentView(R.layout.activity_main);
        Button refreshButton = (Button) findViewById(R.id.refresh);
        refreshButton.setOnClickListener(this);
        Button connectButton = (Button) findViewById(R.id.connect);
        connectButton.setOnClickListener(this);
        Button disconnectButton = (Button) findViewById(R.id.disconnect);
        disconnectButton.setOnClickListener(this);
        Button nextButton = (Button) findViewById(R.id.next);
        nextButton.setOnClickListener(this);


        spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        Spinner musesSpinner = (Spinner) findViewById(R.id.muses_spinner);
        musesSpinner.setAdapter(spinnerAdapter);
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
                if(shouldIBeDoingColors)
                {
                bigRect = findViewById(R.id.bigRect1);
                hex1 = Integer.toHexString((int)eegBuffer[0] * 13653);
                System.out.println("Hex String " + hex1);

                for(int i = hex1.length(); i < 6; i++)
                {
                    hex1 = "0" + hex1;
                }



                if(hex1.length() > 6)
                {
                    hex1 = hex1.substring(0, 6); // reduce hex value if it's too long...
                }
                hex1 = "#" + hex1;

                System.out.println("after cut " + hex1);
                //hexColor = Color.decode();
                /*hexRed = Color.red(Color.parseColor(hex1.substring(0, 2)));
                hexGreen = Color.green(Color.parseColor(hex1.substring(2, 4)));
                hexBlue = Color.blue(Color.parseColor(hex1.substring(4, 5)));
                System.out.println(hexRed);
                System.out.println(hexGreen);
                System.out.println(hexBlue);
*/
                System.out.println(Color.parseColor(hex1));

                bigRect.setBackgroundColor(Color.parseColor(hex1));
                }
            }
            if (accelStale) {
                updateAccel();
            }
           /* if (museArtifactPacket.getBlink()) {
                updateBlink();
            }
            if (museArtifactPacket.getJawClench()) {
                updateJawClench();
            }
            */
            handler.postDelayed(tickUi, 1000 / 3);
        }
    };

    //@Override
    public void onClick(View v) {

        if (v.getId() == R.id.refresh) {
            // The user has pressed the "Refresh" button.
            // Start listening for nearby or paired Muse headbands. We call stopListening
            // first to make sure startListening will clear the list of headbands and start fresh.
            manager.stopListening();
            manager.startListening();

        }

        if (v.getId() == R.id.connect) {

            // The user has pressed the "Connect" button to connect to
            // the headband in the spinner.

            // Listening is an expensive operation, so now that we know
            // which headband the user wants to connect to we can stop
            // listening for other headbands.
            manager.stopListening();

            List<Muse> availableMuses = manager.getMuses();
            Spinner musesSpinner = (Spinner) findViewById(R.id.muses_spinner);

            // Check that we actually have something to connect to.
            if (availableMuses.size() < 1 || musesSpinner.getAdapter().getCount() < 1) {
                Log.w(TAG, "There is nothing to connect to");
            } else {

                // Cache the Muse that the user has selected.
                muse = availableMuses.get(musesSpinner.getSelectedItemPosition());
                // Unregister all prior listeners and register our data listener to
                // receive the MuseDataPacketTypes we are interested in.  If you do
                // not register a listener for a particular data type, you will not
                // receive data packets of that type.
                muse.unregisterAllListeners();
                muse.registerConnectionListener(connectionListener);
                muse.registerDataListener(dataListener, MuseDataPacketType.EEG);
                muse.registerDataListener(dataListener, MuseDataPacketType.ALPHA_RELATIVE);
                muse.registerDataListener(dataListener, MuseDataPacketType.ACCELEROMETER);
                muse.registerDataListener(dataListener, MuseDataPacketType.BATTERY);
                muse.registerDataListener(dataListener, MuseDataPacketType.DRL_REF);
                muse.registerDataListener(dataListener, MuseDataPacketType.QUANTIZATION);

                // Initiate a connection to the headband and stream the data asynchronously.



                muse.runAsynchronously();
            }

        } else if (v.getId() == R.id.disconnect) {

            // The user has pressed the "Disconnect" button.
            // Disconnect from the selected Muse.
            if (muse != null) {
                muse.disconnect();
            }

        /*} else if (v.getId() == R.id.pause) {

            // The user has pressed the "Pause/Resume" button to either pause or
            // resume data transmission.  Toggle the state and pause or resume the
            // transmission on the headband.
            if (muse != null) {
                dataTransmission = !dataTransmission;
                muse.enableDataTransmission(dataTransmission);
            }
         }
        */
        }
    }

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
    /*
    private void updateBlink() {
        TextView blink = (TextView)findViewById(R.id.blink);
        blink.setText("Blink: "+museArtifactPacket.getBlink());
    }

    private void updateJawClench() {
        TextView jawClench = (TextView)findViewById(R.id.jawClench);
        jawClench.setText("Jaw Clench: "+museArtifactPacket.getJawClench());
    }
    */


    class MuseL extends MuseListener {
        final WeakReference<MainActivity> activityRef;

        MuseL(final WeakReference<MainActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void museListChanged() {
            activityRef.get().museListChanged();
        }
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
