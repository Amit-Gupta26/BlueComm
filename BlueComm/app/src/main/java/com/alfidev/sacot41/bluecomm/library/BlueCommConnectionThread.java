package com.alfidev.sacot41.bluecomm.library;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Created by Samuel on 2015-04-15.
 */
public class BlueCommConnectionThread extends Thread {

    private final BlueCommDevice mmBlueCommDevice;
    private final BluetoothDevice mmDevice;
    private BluetoothSocket mmSocket;
    private ParcelUuid[] mmUUIDAvailableList;
    private int mmUUIDIterator = 0;

    public interface onConnectionListener {
        public void onGetUUID(UUID inUuid);
        public void onConnection(boolean success);
    }
    private onConnectionListener connectionListener;
    public void setOnConnectionListener(onConnectionListener listener) { this.connectionListener = listener; }

    public BluetoothSocket getSocket() { return this.mmSocket; }

    public BlueCommConnectionThread(BlueCommDevice blueCommDevice, BluetoothDevice device) {

        mmBlueCommDevice = blueCommDevice;
        mmDevice = device;
        mmUUIDAvailableList = mmDevice.getUuids();

        try {
            mmSocket = createBluetoothSocket();
        } catch (IOException e) {
            Log.d(BlueComm.TAG, "get socket fail : " + e);
            e.printStackTrace();
        }
    }

    public BluetoothSocket createBluetoothSocket() throws IOException {

        if (mmBlueCommDevice.getUUID() == null) return null;
        if (mmDevice == null) return null;

        if(Build.VERSION.SDK_INT >= 10){
            try {
                final Method m = mmDevice.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                return (BluetoothSocket) m.invoke(mmDevice, mmBlueCommDevice.getUUID());
            } catch (Exception e) {
                Log.e(BlueComm.TAG, "Could not create Insecure RFComm Connection", e);
            }
        }
        return  mmDevice.createRfcommSocketToServiceRecord(mmBlueCommDevice.getUUID());
    }

    public void run() {
        setName("ConnectThread");
        BlueComm.getInstance().cancelDiscovery();

        try {
            mmSocket.connect();
        } catch (IOException connectException) {
            try {
                mmSocket.close();
            } catch (IOException closeException) { Log.d(BlueComm.TAG,"connect exception 2 : " + closeException);}

            if(mmUUIDIterator < mmUUIDAvailableList.length){
                try {
                    connectionListener.onGetUUID(mmUUIDAvailableList[mmUUIDIterator].getUuid());
                    mmSocket = createBluetoothSocket();
                    mmUUIDIterator = mmUUIDIterator + 1;
                    this.run();
                } catch (IOException e) {
                    Log.d(BlueComm.TAG,"get socket fail : " + e);
                    e.printStackTrace();
                }
            } else {
                Log.d(BlueComm.TAG, "cant create socket : " );
                if (connectionListener != null) connectionListener.onConnection(false);
            }

            return;

        }

        if(connectionListener != null) connectionListener.onConnection(true);
    }

    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            mmSocket.close();
            if (connectionListener != null) connectionListener.onConnection(false);
        } catch (IOException e) { }
    }

}
