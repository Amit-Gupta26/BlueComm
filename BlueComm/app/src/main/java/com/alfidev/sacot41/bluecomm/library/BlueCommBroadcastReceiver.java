package com.alfidev.sacot41.bluecomm.library;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;

/**
 * Created by Samuel on 2015-04-22.
 */
public class BlueCommBroadcastReceiver extends BroadcastReceiver {

    public interface BlueCommBroadCastDeviceListener {
        public void onRSSI(String macAddress, short rssi);
        public void onConnection(String macAddress);
        public void onDisconnection(String macAddress);
    }
    private ArrayList<BlueCommBroadCastDeviceListener> clientArray = new ArrayList<>();
    public void registerToBroadCast(BlueCommBroadCastDeviceListener deviceListener) { clientArray.add(deviceListener); }

    public interface BlueCommBroadCastStateListener {
        public void onStateOff();
        public void onStateTurningOff();
        public void onStateOn();
        public void onStateTurningOn();
    }
    private  BlueCommBroadCastStateListener stateListener;

    public interface BlueCommBroadCastServiceListener {
        public void onDiscovery(BlueCommDevice device);
        public void onFinishDiscovery();
    }
    private BlueCommBroadCastServiceListener serviceListener;

    public BlueCommBroadcastReceiver(BlueCommBroadCastStateListener inStateListener, BlueCommBroadCastServiceListener inServiceListener) {
        this.stateListener = inStateListener;
        this.serviceListener = inServiceListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        final String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR);

            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    if (stateListener != null) stateListener.onStateOff();
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    if (stateListener != null) stateListener.onStateTurningOff();
                    break;
                case BluetoothAdapter.STATE_ON:
                    if (stateListener != null) stateListener.onStateOn();
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    if (stateListener != null) stateListener.onStateTurningOn();
                    break;
            }
        }

        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (action.equals(BluetoothDevice.ACTION_FOUND)) {

            short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);

            if (clientArray != null && clientArray.size() > 0) {
                for (BlueCommBroadCastDeviceListener blueCommBroadCastDeviceListener : clientArray) {
                    if (blueCommBroadCastDeviceListener  != null) blueCommBroadCastDeviceListener.onRSSI(device.getAddress(), rssi);
                    else clientArray.remove(blueCommBroadCastDeviceListener);
                }
            }

            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                if (serviceListener != null) serviceListener.onDiscovery(new BlueCommDevice(device));
            }

        } else if(action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)){
            if (clientArray != null && clientArray.size() > 0) {
                for (BlueCommBroadCastDeviceListener blueCommBroadCastDeviceListener : clientArray) {
                    if (blueCommBroadCastDeviceListener != null) blueCommBroadCastDeviceListener.onConnection(device.getAddress());
                    else clientArray.remove(blueCommBroadCastDeviceListener);
                }
            }
        } else if(action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)){
            if (clientArray != null && clientArray.size() > 0) {
                for (BlueCommBroadCastDeviceListener blueCommBroadCastDeviceListener : clientArray) {
                    if (blueCommBroadCastDeviceListener != null) blueCommBroadCastDeviceListener.onDisconnection(device.getAddress());
                    else clientArray.remove(blueCommBroadCastDeviceListener);
                }
            }
        } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
            if (serviceListener != null) serviceListener.onFinishDiscovery();
        }

    }

}
