package com.alfidev.sacot41.bluecomm.library;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Created by Samuel on 2015-04-15.
 */
public class BlueCommDevice implements Serializable {

    public enum BlueCommDeviceStatus {
        REACHABLE,
        CONNECTION,
        CONNECTED,
        DISCONNECTED,
        UNKNOWN,
    }

    //transient
    //http://blog.paumard.org/cours/java/chap10-entrees-sorties-serialization.html
    protected transient BluetoothDevice mDevice;
    protected String mMacAddress;
    protected BlueCommDeviceStatus mStatus;
    protected UUID mUuid;
    private transient BlueCommConnectionThread mConnectionThread;
    private transient BlueCommConnectedThread mConnectedThread;

    public String getMacAddress() { return this.mMacAddress; }
    public String getName() { return this.mDevice.getName(); }
    public BlueCommDeviceStatus getStatus() { return this.mStatus; }
    public UUID getUUID() { return this.mUuid; }
    public int pairState() { return this.mDevice.getBondState(); }

    BlueCommDevice(String inMacAddress) { this.mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(inMacAddress); }
    BlueCommDevice(BluetoothDevice inDevice) { this.mDevice = inDevice; }

    /**
     * Listener passed on connection
     */
    public interface onConnectListener {
        public void connection(boolean success);
    }

    /**
     * Listener for device receive information
     */
    public interface onReceiveListener {
        public void receive(String msg);
    }
    private onReceiveListener receiveListener;
    public void setOnReceiveListener(onReceiveListener listener) { this.receiveListener = listener; }

    /**
     * listener called when device is not reachable
     */
    public interface onLostConnectionListener {
        public void lostConnection();
    }
    private onLostConnectionListener lostConnectionListener;
    public void setOnLostConnectionListener(onLostConnectionListener listener) { this.lostConnectionListener = listener; }

    /**
     * donne la puissance du signal
     * ne peut etre utilise que si le device distant est deconnecter
     */
    public interface  onCatchRSSIListener {
        public void catchRSSI(short rssi);
    }
    private onCatchRSSIListener catchRSSIListener;
    public void setOnCatchRSSIListener(onCatchRSSIListener listener) { this.catchRSSIListener = listener; }

    /**
     * Listener on pair state change
     */
    public interface onPairStateChange {
        public void onPaired(int lastState);
        public void onPairing(int lastState);
        public void onUnPair(int lastState);
    }
    private onPairStateChange pairChangeListener;
    public void setOnPairStateChangeListener(onPairStateChange listener) { this.pairChangeListener = listener; }

    /**
     * Register to BroadCast
     */
    void registerToBroadCast(BlueCommBroadcastReceiver broadcastReceiver){
        broadcastReceiver.registerToBroadCast(new BlueCommBroadcastReceiver.BlueCommBroadCastDeviceListener() {
            @Override
            public void onRSSI(String macAddress, short rssi) {
                if (mMacAddress.equals(macAddress)) catchRSSIListener.catchRSSI(rssi);
            }

            @Override
            public void onConnection(String macAddress) {

            }

            @Override
            public void onDisconnection(String macAddress) {
                if (mMacAddress.equals(macAddress)){
                    mStatus = BlueCommDeviceStatus.DISCONNECTED;
                    if (lostConnectionListener != null) lostConnectionListener.lostConnection();
                }
            }

            @Override
            public void onBondStateChange(String macAddress, int currentBondState, int passBondState) {
                if (mMacAddress.equals(macAddress)){
                    if (pairChangeListener != null) {
                        if (currentBondState == BluetoothDevice.BOND_BONDED) pairChangeListener.onPaired(passBondState);
                        else if (currentBondState == BluetoothDevice.BOND_BONDING) pairChangeListener.onPairing(passBondState);
                        else if (currentBondState == BluetoothDevice.BOND_NONE) pairChangeListener.onUnPair(passBondState);
                    }
                }
            }
        });
    }

    /**
     *
     * Pair device
     *
     */
    public void pairDevice() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method m = mDevice.getClass().getMethod("createBond", (Class[]) null);
        m.invoke(mDevice, (Object[]) null);
    }

    private void unpairDevice(BluetoothDevice device) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method m = mDevice.getClass() .getMethod("removeBond", (Class[]) null);
        m.invoke(mDevice, (Object[]) null);
    }

    /**
     * Connect
     *
     */
    public void connect(onConnectListener connectListener) {
        if (this.mStatus == BlueCommDeviceStatus.DISCONNECTED){
            if(this.mConnectionThread == null){
                makeConnectionThread(connectListener);
            } else if(mConnectedThread == null){
                makeConnectedThread(connectListener);
            } else {
                if (connectListener != null) connectListener.connection(true);
            }
        }
    }

    private void makeConnectionThread(final onConnectListener connectListener) {

        if (mConnectionThread != null) {
            mConnectionThread.cancel();
            mConnectionThread = null;
        }

        this.mStatus = BlueCommDeviceStatus.CONNECTION;
        this.mConnectionThread = new BlueCommConnectionThread(this, this.mDevice);
        this.mConnectionThread.setOnConnectionListener(new BlueCommConnectionThread.onConnectionListener() {

            @Override
            public void onGetUUID(UUID inUuid) {
                mUuid = inUuid;
            }

            @Override
            public void onConnection(boolean success) {
                if (!success) {
                    disconnect();
                    if (connectListener != null) connectListener.connection(false);
                }
                else makeConnectedThread(connectListener);
            }

        });
        this.mConnectionThread.start();
        mStatus = BlueCommDeviceStatus.CONNECTION;
    }

    private void makeConnectedThread(final onConnectListener connectListener) {
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        this.mStatus = BlueCommDeviceStatus.CONNECTED;
        mConnectedThread = new BlueCommConnectedThread(mConnectionThread.getSocket());
        mConnectedThread.setOnLostConnectionListener(new BlueCommConnectedThread.onLostConnectionListener() {
            @Override
            public void onLostConnection() {
                disconnect();
                if (connectListener != null) connectListener.connection(false);
            }
        });
        mConnectedThread.setOnReceiveMessageListener(new BlueCommConnectedThread.onReceiveMessageListener() {
            @Override
            public void onReceiveMessage(String msg) {
                if (receiveListener != null) receiveListener.receive(msg);
            }
        });
        mConnectedThread.start();
    }

    public void disconnect() {
        if(mConnectionThread != null){
            mConnectionThread.cancel();
            mConnectionThread = null;
        }
        if(mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mStatus = BlueCommDeviceStatus.DISCONNECTED;
    }

    /**
     * Send data
     *
     * @param msg
     */
    public void send(String msg) {
        send(msg.getBytes());
    }

    public void send(byte[] rawData) {
        if(mConnectedThread != null){
            mConnectedThread.write(rawData);
        }
    }

    /**
     *
     * Close threat
     * @throws Throwable
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        disconnect();
    }
}
