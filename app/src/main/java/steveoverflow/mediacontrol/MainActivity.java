package steveoverflow.mediacontrol;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;


public class MainActivity extends Activity {
    public static final String PREFS_NAME = "mediaControlPrefs";
    public static final String SAVED_PLAYER_NAME = "PLAYER_NAME";
    public static final String SAVED_REMOTE_NAME = "REMOTE_NAME";

    private static final Map<String, String> players = new TreeMap<String, String>(){{
        put("","");
        put("doubleTwist","com.doubleTwist.androidPlayer");
        put("Smart Audiobook Player","ak.alizandro.smartaudiobookplayer");
    }};

    private BluetoothAdapter    btAdapter = null;
    private BluetoothSocket     socket = null;
    private BluetoothDevice     clock = null;
    private InputStream         btStream = null;

    private static String remoteDeviceName;
    private static String playerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String savedPlayerName = settings.getString(SAVED_PLAYER_NAME, "");
        String savedRemoteName = settings.getString(SAVED_REMOTE_NAME, "");

        final ToggleButton pause = (ToggleButton) findViewById(R.id.connect);
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean on = ((ToggleButton) v).isChecked();

                if(on){
                    connectBluetooth();
                }else{
                    disconnectBluetooth();
                }
            }
        });

        setupBluetooth();

        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        String[] devices = new String[pairedDevices.size()+1];
        devices[0]="";
        int i = 1;
        for(BluetoothDevice device: pairedDevices){
            devices[i] = device.getName();
            i++;
        }

        // set up the spinner to select the bluetooth device to connect too
        Spinner remoteSpinner = (Spinner) findViewById(R.id.remoteSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, devices);
        remoteSpinner.setAdapter(adapter);

        remoteSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                MainActivity.this.setRemoteDeviceName((String) parent.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        remoteSpinner.setSelection(adapter.getPosition(savedRemoteName));

        Spinner playerSpinner = (Spinner) findViewById(R.id.playerSpinner);
        final String[] playerList = new String[players.keySet().size()];
        i=0;
        for(String player: players.keySet()){
            playerList[i]=player;
            i++;
        }
        ArrayAdapter<String> playerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, playerList);
        playerSpinner.setAdapter(playerAdapter);

        playerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String pn = playerList[position];
                MainActivity.this.setPlayerName(players.get(pn));
                setPreference(SAVED_PLAYER_NAME, pn);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        playerSpinner.setSelection(playerAdapter.getPosition(savedPlayerName));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private static void sendMediaButton(Context context, int keyCode) {
        if(playerName==null||playerName.trim().equalsIgnoreCase("")){
            return;
        }

        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
        intent.setPackage(playerName);
        context.sendBroadcast(intent, null);

        keyEvent = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
        intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
        intent.setPackage(playerName);
        context.sendBroadcast(intent, null);
    }

    private void setupBluetooth(){
        btAdapter = BluetoothAdapter.getDefaultAdapter();;

        if(!btAdapter.isEnabled()){
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
    }

    private void connectBluetooth(){
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        if(this.getRemoteDeviceName()==null||this.getRemoteDeviceName().trim().equalsIgnoreCase("")){
            Toast.makeText(this, "No Bluetooth device selected.", Toast.LENGTH_LONG).show();
            ToggleButton bluetoothToggle = (ToggleButton) findViewById(R.id.connect);
            bluetoothToggle.setChecked(false);
            return;
        }

        if(pairedDevices.size()>0){
            for(BluetoothDevice device: pairedDevices){
                if(device.getName().equals(remoteDeviceName)){
                    clock = device;
                    break;
                }
            }
        }

        BluetoothSocket socket;

        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID

        try {
            socket = clock.createRfcommSocketToServiceRecord(uuid);
            socket.connect();
            btStream = socket.getInputStream();
            setPreference(SAVED_REMOTE_NAME, remoteDeviceName);

        } catch (IOException e) {
            e.printStackTrace();
        }

        BluetoothMonitor btm = new BluetoothMonitor(btStream, this);
        btm.start();
    }

    private void disconnectBluetooth(){
        if(btStream != null){
            try{
                btStream.close();
            }catch(Exception e){}
            btStream = null;
        }

        if(socket != null){
            try{
                socket.close();
            }catch(Exception e){}
            socket = null;
        }

        ToggleButton bluetoothToggle = (ToggleButton) findViewById(R.id.connect);
        bluetoothToggle.setChecked(false);
    }

    private void setPreference(String prefName, String value){
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(prefName, value);
        editor.commit();
    }

    public static void sendKeyCode(int event, Context context){
        sendMediaButton(context, event);
    }

    public synchronized String getRemoteDeviceName(){
        return this.remoteDeviceName;
    }

    public synchronized void setRemoteDeviceName(String remoteDeviceName){
        this.remoteDeviceName = remoteDeviceName;
    }

    public synchronized String getPlayerName(){
        return this.playerName;
    }

    public synchronized void setPlayerName(String playerName){
        this.playerName = playerName;
    }
}
