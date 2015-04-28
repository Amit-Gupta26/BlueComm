package com.alfidev.sacot41.bluecomm.library;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Samuel on 2015-04-15.
 */
public class BlueCommConnectedThread extends Thread {

    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    public interface onReceiveMessageListener {
        public void onReceiveMessage(String msg);
    }
    private onReceiveMessageListener receiveMessageListener;
    public void setOnReceiveMessageListener(onReceiveMessageListener listener) { this.receiveMessageListener = listener; }

    public interface onLostConnectionListener {
        public void onLostConnection();
    }
    private onLostConnectionListener lostConnectionListener;
    public void setOnLostConnectionListener(onLostConnectionListener listener) { this.lostConnectionListener = listener; }

    public BlueCommConnectedThread(BluetoothSocket socket) {
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { Log.d(BlueComm.TAG, "connected exception : " + e);}

        mmInStream = tmpIn;
        mmOutStream = tmpOut;

    }

    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;

        while (true) {
            try {

                bytes = mmInStream.read(buffer);
                Log.d(BlueComm.TAG,"recept buffer : " + buffer);
                String msg = "";
                for (int i = 0; i < bytes; i++) {
                    msg = msg + String.valueOf(buffer[i]);
                }

                if(receiveMessageListener != null) receiveMessageListener.onReceiveMessage(msg);

            } catch (IOException e) {
                //connection lost
                Log.d(BlueComm.TAG,"connnected eception 2 : " + e);
                if(lostConnectionListener != null) lostConnectionListener.onLostConnection();
                break;
            }
        }

    }

    /* Call this from the main activity to send data to the remote device */
    public boolean write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
            return true;
        } catch (IOException e) { Log.d(BlueComm.TAG,"connected write exception : " +e);}
        return false;
    }

    /* Call this from the main activity to shutdown the connection */
    public boolean cancel() {
        try {
            mmSocket.close();
            return true;
        } catch (IOException e) {Log.d(BlueComm.TAG,"connected cancel "); }
        return false;
    }
}
