package com.alfidev.sacot41.library;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Set;

/**
 * Created by Samuel on 2015-04-15.
 */
public class BlueComm {

    public static final String TAG = "bluecomm";
    private static final String INTERNAL_SAVE_BLUECOMMDEVICE_LIST = "KEY_BLUETOOTH_DEVICE";

    public enum BlueCommStatus {
        UNKNOWN,
        TURNING_OPEN,
        OPEN,
        TURNING_CLOSE,
        CLOSE
    }

    private static BlueComm instance;
    private BlueComm() {  }
    public static BlueComm getInstance() {
        if (instance == null) instance = new BlueComm();
        return instance;
    }

    private BlueCommStatus bluetoothStatus = BlueCommStatus.UNKNOWN;
    private Context mContext;

    private Class<?> deviceClass = BlueCommDevice.class;
    public void setDeviceClass(Class<?> inClass) {this.deviceClass = inClass; }

    private ArrayList<BlueCommDevice> mDevices = new ArrayList<BlueCommDevice>();

    private BlueCommBroadcastReceiver.BlueCommBroadCastServiceListener serviceListener = new BlueCommBroadcastReceiver.BlueCommBroadCastServiceListener() {
        @Override
        public void onDiscovery(BluetoothDevice device) {
            BlueCommDevice bluecommDevice = deviceFactory(device);

        }

        @Override
        public void onFinishDiscovery() {

        }
    };

    private BlueCommBroadcastReceiver.BlueCommBroadCastStateListener stateListener = new BlueCommBroadcastReceiver.BlueCommBroadCastStateListener() {
        @Override
        public void onStateOff() {
            bluetoothStatus = BlueCommStatus.CLOSE;
        }

        @Override
        public void onStateTurningOff() {
            bluetoothStatus = BlueCommStatus.TURNING_CLOSE;
        }

        @Override
        public void onStateOn() {
            bluetoothStatus = BlueCommStatus.OPEN;
        }

        @Override
        public void onStateTurningOn() {
            bluetoothStatus = BlueCommStatus.TURNING_OPEN;
        }
    };

    private BlueCommBroadcastReceiver mReceiver = new BlueCommBroadcastReceiver(stateListener, serviceListener);

    /**
     * Dont forget to ask user before open bluetooth
     * @return
     */

    public boolean isBluetoothAvailable() { return BluetoothAdapter.getDefaultAdapter() != null;}
    public boolean isBluetoothOpen() { return BluetoothAdapter.getDefaultAdapter().isEnabled(); }
    public void openBluetooth()      { if(!BluetoothAdapter.getDefaultAdapter().isEnabled())  BluetoothAdapter.getDefaultAdapter().enable(); }
    public void closeBluetooth()     { if(BluetoothAdapter.getDefaultAdapter().isEnabled())  BluetoothAdapter.getDefaultAdapter().disable(); }

    public void onStart(Context inContext) {
        mContext = inContext;

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        inContext.registerReceiver(mReceiver, filter);
        inContext.registerReceiver(mReceiver, filter1);
        inContext.registerReceiver(mReceiver, filter2);
        inContext.registerReceiver(mReceiver, filter3);
    }
    public void onStop() {
        mContext.unregisterReceiver(mReceiver);
        for (BlueCommDevice blueCommDevice : getSavedDevice()) {
            blueCommDevice.disconnect();
        }
    }

    /**
     * Return all reachable and unPaired device
     */
    public interface GetUnPairedDeviceCallBack {
        public void unPairedDevice(ArrayList<BlueCommDevice> list);
    }
    public void getUnPairedDevice(GetUnPairedDeviceCallBack callback) {

    }

    /**
     * Return all paired device, reachable or not
     */
    public ArrayList<BlueCommDevice> getPairedDevice() {
        if(BluetoothAdapter.getDefaultAdapter() != null){
            Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
            ArrayList<BlueCommDevice> outList = new ArrayList<BlueCommDevice>();
            if (pairedDevices.size() > 0) {

                for (BluetoothDevice device : pairedDevices) {
                    BlueCommDevice findDevice = findDevice(device.getAddress());
                    if (findDevice != null) outList.add(findDevice);
                    else outList.add(deviceFactory(device));
                }

            }
            return outList;
        }
        return new ArrayList<BlueCommDevice>();
    }

    /**
     * Return device paired and see by this device
     */
    public interface GetReachableDeviceCallBack {
        public void reachableDevice(ArrayList<BlueCommDevice> list);
    }
    public void getReachableDevice(GetReachableDeviceCallBack callBack) {
        BluetoothAdapter.getDefaultAdapter().startDiscovery();
    }

    public void cancelDiscovery() { BluetoothAdapter.getDefaultAdapter().cancelDiscovery(); }

    public void saveDevice(BlueCommDevice device) throws IOException {
        ArrayList<BlueCommDevice> deviceList = getSavedDevice();
        if (deviceList == null) deviceList = new ArrayList<BlueCommDevice>();
        deviceList.add(device);
        InternalStorage.writeObject(mContext, INTERNAL_SAVE_BLUECOMMDEVICE_LIST, deviceList);
    }
    public void forgetDevice(BlueCommDevice device) throws IOException {
        ArrayList<BlueCommDevice> deviceList = getSavedDevice();
        if (deviceList == null) return;
        for (BlueCommDevice blueCommDevice : deviceList) {
            if (blueCommDevice.getMacAddress().equals(device.getMacAddress())) deviceList.remove(blueCommDevice);
        }
        InternalStorage.writeObject(mContext, INTERNAL_SAVE_BLUECOMMDEVICE_LIST, deviceList);
    }
    public ArrayList<BlueCommDevice> getSavedDevice() {
        ArrayList<BlueCommDevice> deviceList = null;
        try {
            ArrayList<BlueCommDevice> intermediateList = (ArrayList<BlueCommDevice>) InternalStorage.readObject(mContext, INTERNAL_SAVE_BLUECOMMDEVICE_LIST);
            if (intermediateList.size() > 0 ) {
                deviceList = new ArrayList<BlueCommDevice>();
                for (BlueCommDevice blueCommDevice : intermediateList) {
                    BlueCommDevice findDevice = findDevice(blueCommDevice.getMacAddress());
                    if (findDevice != null) deviceList.add(findDevice);
                    else deviceList.add(deviceFactory(blueCommDevice));
                }
            }

        } catch (IOException e) {
            Log.e("alarm", e.getMessage());
        } catch (ClassNotFoundException e) {
            Log.e("alarm", e.getMessage());
        }
        return deviceList;
    }

    /**
     *
     * this method ensure that the device is registred
     * @param bluetoothDevice
     * @return
     */
    public BlueCommDevice deviceFactory(BluetoothDevice bluetoothDevice) {
        if (!BlueCommDevice.class.isAssignableFrom(this.deviceClass)) throw new IllegalArgumentException("device class is not a extension of BlueCommDevice");

        BlueCommDevice device = findDevice(bluetoothDevice.getAddress());
        if (device != null) return device;

        try {
            device  = (BlueCommDevice) this.deviceClass.getConstructor(BluetoothDevice.class).newInstance(bluetoothDevice);
            mDevices.add(device);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        return device;
    }

    public BlueCommDevice deviceFactory(String macAddress) {
        if (!BlueCommDevice.class.isAssignableFrom(this.deviceClass)) throw new IllegalArgumentException("device class is not a extension of BlueCommDevice");

        BlueCommDevice device = findDevice(macAddress);
        if (device != null) return device;

        try {
            device  = (BlueCommDevice) this.deviceClass.getConstructor(String.class).newInstance(macAddress);
            mDevices.add(device);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        return device;
    }

   public BlueCommDevice deviceFactory(BlueCommDevice inDevice)  {

       if (!BlueCommDevice.class.isAssignableFrom(this.deviceClass)) throw new IllegalArgumentException("device class is not a extension of BlueCommDevice");

       BlueCommDevice device = findDevice(inDevice.getMacAddress());
       if (device != null) return device;

       mDevices.add(device);
       return device;
    }

    private BlueCommDevice findDevice(String macAddres) {
        for (BlueCommDevice blueCommDevice : mDevices) {
            if(blueCommDevice.getMacAddress().equals(macAddres)) return blueCommDevice;
            else if (blueCommDevice == null) mDevices.remove(blueCommDevice);
        }
        return null;
    }
}
