package steveoverflow.mediacontrol;

import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import android.content.Context;
import android.view.KeyEvent;

/**
 * Created by stephentanton on 15-08-18.
 */
public class BluetoothMonitor implements Runnable {
    private BufferedReader btReader;
    private Thread t;
    private MainActivity activity;

    public BluetoothMonitor(InputStream btStream, MainActivity activity){
        this.btReader = new BufferedReader(new InputStreamReader(btStream));
        this.activity=activity;
    }

    @Override
    public void run() {

        try {
            while(true&&!Thread.currentThread().isInterrupted()){
                String input = btReader.readLine();
                Log.i("BTDATA", input);
                if(input.trim().equalsIgnoreCase("play")) {
                    activity.sendKeyCode(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, activity.getBaseContext());
                }else if(input.trim().equalsIgnoreCase("next")){
                    activity.sendKeyCode(KeyEvent.KEYCODE_MEDIA_NEXT, activity.getBaseContext());
                }else if(input.trim().equalsIgnoreCase(("prev"))){
                    activity.sendKeyCode(KeyEvent.KEYCODE_MEDIA_PREVIOUS, activity.getBaseContext());
                }
            }
        }catch(IOException ioe){

        }

    }

    public void start(){
        if(t==null){
            t = new Thread(this);
            t.start();
        }
    }
}
