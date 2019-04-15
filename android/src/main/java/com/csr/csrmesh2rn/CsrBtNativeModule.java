package com.csr.csrmesh2rn;

import javax.annotation.Nullable;

import java.util.List;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanSettings;
import android.content.ComponentName;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.csr.csrmesh2.ConfigModelApi;
import com.csr.csrmesh2.DeviceInfo;
import com.csr.csrmesh2.GroupModelApi;
import com.csr.csrmesh2.MeshConstants;
import com.csr.csrmesh2.MeshService;
import com.csr.csrmesh2.DataModelApi;
import com.csr.csrmesh2.LightModelApi;
import com.csr.csrmesh2.PowerModelApi;
import com.csr.csrmesh2.PowerState;
import com.csr.csrmesh2.TimeModelApi;

import static com.csr.csrmesh2rn.CsrBtPackage.TAG;

public class CsrBtNativeModule extends ReactContextBaseJavaModule implements ActivityEventListener, LifecycleEventListener {

    // Debugging
    private static final boolean D = true;

    private static final int REQUEST_CODE_LOCATION_SETTINGS = 2;
    private static final int ACCESS_COARSE_LOCATION_RESULT_CODE = 4;
    private static final int BLUETOOTH_RESULT_CODE = 5;
    private static final int STORAGE_RESULT_CODE = 6;

    private static int MIN_DEVICE_ID = 0x8000;

    // Event names
    public static final String BT_ENABLED = "bluetoothEnabled";
    public static final String BT_DISABLED = "bluetoothDisabled";
    public static final String SYSTEM_LOCATION_ENABLED = "systemLocationEnabled";
    public static final String SYSTEM_LOCATION_DISABLED = "systemLocationDisabled";
    public static final String SERVICE_CONNECTED = "serviceConnected";
    public static final String SERVICE_DISCONNECTED = "serviceDisconnected";
    public static final String NOTIFICATION_ONLINE_STATUS = "notificationOnlineStatus";
    public static final String NOTIFICATION_GET_DEVICE_STATE = "notificationGetDeviceState";
    public static final String DEVICE_STATUS_CONNECTING = "deviceStatusConnecting";
    public static final String DEVICE_STATUS_CONNECTED = "deviceStatusConnected";
    public static final String DEVICE_STATUS_LOGINING = "deviceStatusLogining";
    public static final String DEVICE_STATUS_LOGIN = "deviceStatusLogin";
    public static final String DEVICE_STATUS_LOGOUT = "deviceStatusLogout";
    public static final String DEVICE_STATUS_ERROR_N = "deviceStatusErrorAndroidN";
    public static final String DEVICE_STATUS_UPDATE_MESH_COMPLETED = "deviceStatusUpdateMeshCompleted";
    public static final String DEVICE_STATUS_UPDATING_MESH = "deviceStatusUpdatingMesh";
    public static final String DEVICE_STATUS_UPDATE_MESH_FAILURE = "deviceStatusUpdateMeshFailure";
    public static final String DEVICE_STATUS_UPDATE_ALL_MESH_COMPLETED = "deviceStatusUpdateAllMeshCompleted";
    public static final String DEVICE_STATUS_GET_LTK_COMPLETED = "deviceStatusGetLtkCompleted";
    public static final String DEVICE_STATUS_GET_LTK_FAILURE = "deviceStatusGetLtkFailure";
    public static final String DEVICE_STATUS_MESH_OFFLINE = "deviceStatusMeshOffline";
    public static final String DEVICE_STATUS_MESH_SCAN_COMPLETED = "deviceStatusMeshScanCompleted";
    public static final String DEVICE_STATUS_MESH_SCAN_TIMEOUT = "deviceStatusMeshScanTimeout";
    public static final String DEVICE_STATUS_OTA_COMPLETED = "deviceStatusOtaCompleted";
    public static final String DEVICE_STATUS_OTA_FAILURE = "deviceStatusOtaFailure";
    public static final String DEVICE_STATUS_OTA_PROGRESS = "deviceStatusOtaProgress";
    public static final String DEVICE_STATUS_GET_FIRMWARE_COMPLETED = "deviceStatusGetFirmwareCompleted";
    public static final String DEVICE_STATUS_GET_FIRMWARE_FAILURE = "deviceStatusGetFirmwareFailure";
    public static final String DEVICE_STATUS_DELETE_COMPLETED = "deviceStatusDeleteCompleted";
    public static final String DEVICE_STATUS_DELETE_FAILURE = "deviceStatusDeleteFailure";
    public static final String LE_SCAN = "leScan";
    public static final String LE_SCAN_COMPLETED = "leScanCompleted";
    public static final String LE_SCAN_TIMEOUT = "leScanTimeout";
    public static final String MESH_OFFLINE = "meshOffline";

    // Members
    private static CsrBtNativeModule mThis;
    private MeshService mService;
    private BluetoothAdapter mBluetoothAdapter;
    private ReactApplicationContext mReactContext;
    protected Context mContext;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    protected boolean isInited = false;
    protected boolean isServiceStarted = false;
    protected boolean isChannelReady = false;
    protected String mConfigNodeResetMacAddress;

    // Promises
    private Promise mConfigNodePromise;
    private Promise mGetNumberOfModelGroupIds;
    private Promise mSetModelGroupIdPromise;

    final BroadcastReceiver mBluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        if (D) Log.d(TAG, "Bluetooth was disabled");
                        sendEvent(BT_DISABLED);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        if (D) Log.d(TAG, "Bluetooth was enabled");
                        sendEvent(BT_ENABLED);
                        break;
                }
            }
        }
    };

    public CsrBtNativeModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mThis = this;
        mReactContext = reactContext;
        mContext = mReactContext.getApplicationContext();
    }

    @Override
    public String getName() {
        return "CsrBt";
    }

    public static CsrBtNativeModule getInstance() {
        return mThis;
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
        if (D) Log.d(TAG, "On activity result request: " + requestCode + ", result: " + resultCode);
        // if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
        //     if (resultCode == Activity.RESULT_OK) {
        //         if (D) Log.d(TAG, "User enabled Bluetooth");
        //         if (mEnabledPromise != null) {
        //             mEnabledPromise.resolve(true);
        //         }
        //     } else {
        //         if (D) Log.d(TAG, "User did *NOT* enable Bluetooth");
        //         if (mEnabledPromise != null) {
        //             mEnabledPromise.reject(new Exception("User did not enable Bluetooth"));
        //         }
        //     }
        //     mEnabledPromise = null;
        // }

        // if (requestCode == REQUEST_PAIR_DEVICE) {
        //     if (resultCode == Activity.RESULT_OK) {
        //         if (D) Log.d(TAG, "Pairing ok");
        //     } else {
        //         if (D) Log.d(TAG, "Pairing failed");
        //     }
        // }

        if (requestCode == REQUEST_CODE_LOCATION_SETTINGS) {
            checkSystemLocation();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (D) Log.d(TAG, "On new intent");
    }


    @Override
    public void onHostResume() {
        if (D) Log.d(TAG, "Host resume");
        if (isInited) {
            this.doResume();
        }
    }

    @Override
    public void onHostPause() {
        if (D) Log.d(TAG, "Host pause");
    }

    @Override
    public void onHostDestroy() {
        if (D) Log.d(TAG, "Host destroy");
        // APP 切到后台时也会调用此处，导致切回前台 Resume 时无法再正常使用本组件，因此使不在此处调用 doDestroy
        // if (isInited) {
        //     this.doDestroy();
        // }
    }

    @Override
    public void onCatalystInstanceDestroy() {
        if (D) Log.d(TAG, "Catalyst instance destroyed");
        super.onCatalystInstanceDestroy();
        if (isInited) {
            this.doDestroy();
        }
    }

    @ReactMethod
    public void doInit() {
        if (!isInited) {
            isInited = true;
        }

        if (isServiceStarted || mService != null)
            return;

        isServiceStarted = true;
        Intent bindIntent = new Intent(mContext, MeshService.class);
        mContext.bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            sendEvent(BT_ENABLED);
        } else {
            sendEvent(BT_DISABLED);
        }

        mReactContext.addActivityEventListener(this);
        mReactContext.addLifecycleEventListener(this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mReactContext.registerReceiver(mBluetoothStateReceiver, intentFilter);
    }

    @ReactMethod
    public void doDestroy() {
        if (isInited) {
            mHandler.removeCallbacksAndMessages(null);
            mReactContext.unregisterReceiver(mBluetoothStateReceiver);
            isInited = false;
        }

        if (isServiceStarted) {
            isServiceStarted = false;
            if (mService != null) {
                mContext.unbindService(mServiceConnection);
                mService = null;
            }
        }
    }

    @ReactMethod
    public void doResume() {
        Log.d(TAG, "onResume");
        // Broadcasting time to the mesh network whenever we resume the app.
        // if (mService != null) {
        //     final int MS_IN_15_MINS = 15 * 60 * 1000;
        //     final byte utcOffset = (byte)(TimeZone.getDefault().getOffset(Calendar.getInstance().getTimeInMillis()) / MS_IN_15_MINS);
        //     TimeModelApi.broadcastTime(Calendar.getInstance().getTimeInMillis(), utcOffset, true);
        // }

        checkPermissions();
        checkAvailability();
        checkSystemLocation();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getCurrentActivity(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getCurrentActivity(),
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        ACCESS_COARSE_LOCATION_RESULT_CODE);
            }
            else if (ContextCompat.checkSelfPermission(getCurrentActivity(),
                    Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getCurrentActivity(),
                        new String[]{Manifest.permission.BLUETOOTH},
                        BLUETOOTH_RESULT_CODE);
            }
            else if (ContextCompat.checkSelfPermission(getCurrentActivity(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getCurrentActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        STORAGE_RESULT_CODE);
            }
            else {
                Log.d(TAG, "checkPermissions ok");
            }
        }
    }

    //---- BT and BLE
    @TargetApi(18)
    private void checkAvailability() {
        if(Build.VERSION.SDK_INT < 18) {
            Log.d(TAG, "Bluetooth LE not supported by this device");
        } else if(!mContext.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le")) {
            Log.d(TAG, "Bluetooth LE not supported by this device");
        } else {
            if(((BluetoothManager)mContext.getSystemService(mContext.BLUETOOTH_SERVICE)).getAdapter().isEnabled())
                Log.d(TAG, "Bluetooth is enabled");
            else
                Log.d(TAG, "Bluetooth is not enabled!");
        }
    }

    public boolean isLocationEnable() {
        LocationManager lm = null;
        boolean gps_enabled = false;
        boolean network_enabled = false;

        lm = (LocationManager) getCurrentActivity().getSystemService(mContext.LOCATION_SERVICE);
        // exceptions will be thrown if provider is not permitted.
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            Log.d(TAG, "gps_enabled: " + gps_enabled);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            Log.d(TAG, "network_enabled:" + network_enabled);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return gps_enabled || network_enabled;
    }

    private void checkSystemLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (isLocationEnable()) {
                sendEvent(SYSTEM_LOCATION_ENABLED);
            } else {
                sendEvent(SYSTEM_LOCATION_DISABLED);
            }
        }
    }

    @ReactMethod
    public void enableBluetooth() {
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
    }

    @ReactMethod
    public void enableSystemLocation() {
        Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        getCurrentActivity().startActivityForResult(locationIntent, REQUEST_CODE_LOCATION_SETTINGS);
    }

    @ReactMethod
    public void notModeAutoConnectMesh(Promise promise) {
        promise.resolve(true);
    }

    @ReactMethod
    public void setNetworkPassPhrase(String passPhrase) {
        if (mService != null) {
            mService.setNetworkPassPhrase(passPhrase);
        }
    }

    private boolean isConnected()
    {
        return isChannelReady && (mService.getActiveBearer() == MeshService.Bearer.BLUETOOTH);
    }

    @ReactMethod
    public void autoConnect(String userMeshName, String userMeshPwd, String otaMac) {
        if (isConnected()) {
            sendEvent(DEVICE_STATUS_LOGIN);
        } else {
            // Log.d(TAG, "prepare autoConnect");

            sendEvent(DEVICE_STATUS_LOGOUT);
            if (mService.getActiveBearer() == MeshService.Bearer.BLUETOOTH) {
                connectBluetooth();
            } else {
                /* Set the Bluetooth bearer. This will start the stack, but
                   we don't connect until we receive MESSAGE_LE_BEARER_READY.*/
                // if (Build.VERSION.SDK_INT >= 21) {
                //     Log.d(TAG, "ScanSettings.SCAN_MODE_LOW_LATENCY");
                //     mService.setBluetoothBearerEnabled(ScanSettings.SCAN_MODE_LOW_LATENCY);
                // }
                // else {
                    mService.setBluetoothBearerEnabled();
                // }
            }
        }
    }

    /**
     * Set bluetooth as bearer and connect to a bridge
     */
    private void connectBluetooth() {
        // Log.d(TAG, "start autoConnect");
        // mService.setTTL((byte)100);
        // mService.setControllerAddress(32769);
        mService.setMeshListeningMode(true, false);
        mService.startAutoConnect(1);
        //mService.setContinuousLeScanEnabled(true);
    }

    @ReactMethod
    public void idleMode(boolean disconnect) {
    }

    @ReactMethod
    public void startScan() {
        Log.d(TAG, "startScan");
        mService.setDeviceDiscoveryFilterEnabled(true);
    }

    @ReactMethod
    public void stopScan() {
        Log.d(TAG, "stopScan");
        mService.setDeviceDiscoveryFilterEnabled(false);
    }

    public static byte[] readableArray2ByteArray(ReadableArray arr) {
        int size = arr.size();
        byte[] byteArr = new byte[size];
        for(int i = 0; i < arr.size(); i++) {
            byteArr[i] = (byte)arr.getInt(i);
        }

        return byteArr;
    }

    @ReactMethod
    public void sendData(int meshAddress, ReadableArray value) {
        DataModelApi.sendData(meshAddress, readableArray2ByteArray(value), false);
        // 可以发送小于等于 10 字节的数据在串口上显示出来，不过发送 10 字节以上或者说 ack 为 true
        // 但是没有提前 setContinuousScanEnabled() 的会 MeshConstants.MESSAGE_TIMEOUT ，只
        // 不过这种方式貌似是为了手机之间通过 蓝牙 mesh 网络传输大量数据用的，因此这里就不实现了
        // DataModelApi.sendData(meshAddress, value.getBytes(), true);
    }

    @ReactMethod
    public void changePower(int meshAddress, int value) {
        if (value >= 0 && value < PowerState.values().length) {
            PowerModelApi.setState(meshAddress, PowerState.values()[value], true);
        }
    }

    @ReactMethod
    public void changeBrightness(int meshAddress, int value) {
        LightModelApi.setLevel(meshAddress, value, false);
    }

    @ReactMethod
    public void changeColorTemp(int meshAddress, int value) {
        int duration = 65535;   // TODO: calculate duration from value;
        LightModelApi.setColorTemperature(meshAddress, value, duration);
    }

    @ReactMethod
    public void changeColor(int meshAddress, int value) {
        int duration = 65535;   // TODO: calculate duration from value;
        LightModelApi.setRgb(meshAddress, value, duration, false);
    }

    @ReactMethod
    public void configNode(ReadableMap node, boolean isToClaim, Promise promise) {
        stopScan();
        mConfigNodePromise = promise;
        int deviceHash = MeshService.getDeviceHash31FromUuid(UUID.fromString(node.getString("macAddress")));

        if (isToClaim) {
            mService.associateDevice(deviceHash, 0, false, node.getInt("meshAddress"));
        } else {
            mConfigNodeResetMacAddress = node.getString("macAddress");
            mService.resetDevice(node.getInt("meshAddress"), readableArray2ByteArray(node.getArray("dhmKey")));
            startScan();
        }
    }

    private void onUpdateMeshCompleted(Bundle data) {
        if (D) Log.d(TAG, "onUpdateMeshCompleted");
        // showBundleData(data);

        if (mConfigNodePromise != null) {
            WritableMap params = Arguments.createMap();
            WritableArray array = Arguments.createArray();
            byte[] dhmKey = data.getByteArray(MeshConstants.EXTRA_RESET_KEY);
            for (int i = 0; i < dhmKey.length; i++) {
                array.pushInt(dhmKey[i] &  0xFF);
            }

            params.putArray("dhmKey", array);
            mConfigNodePromise.resolve(params);
        }
        mConfigNodePromise = null;
    }

    private void onUpdateMeshFailure() {
        if (D) Log.d(TAG, "onUpdateMeshFailure");
        if (mConfigNodePromise != null) {
            mConfigNodePromise.reject(new Exception("onUpdateMeshFailure"));
        }
        mConfigNodePromise = null;
    }

    @ReactMethod
    public void getNumberOfModelGroupIds(int meshAddress, int modelNo, Promise promise) {
        mGetNumberOfModelGroupIds = promise;
        GroupModelApi.getNumberOfModelGroupIds(meshAddress, modelNo);
    }

    private void onGetNumberOfModelGroupIdsCompleted(Bundle data) {
        if (D) Log.d(TAG, "onGetNumberOfModelGroupIdsCompleted");
        // showBundleData(data);

        if (mGetNumberOfModelGroupIds != null) {
            WritableMap params = Arguments.createMap();
            params.putInt("modelNo", data.getInt(MeshConstants.EXTRA_MODEL_NO));
            params.putInt("numGroupIds", data.getInt(MeshConstants.EXTRA_NUM_GROUP_IDS));
            params.putInt("meshAddress", data.getInt(MeshConstants.EXTRA_DEVICE_ID));
            mGetNumberOfModelGroupIds.resolve(params);
        }
        mGetNumberOfModelGroupIds = null;
    }

    @ReactMethod
    public void setModelGroupId(int meshAddress, int modelNo, int groupIndex, int instance, int groupAddress, Promise promise) {
        // stopScan();
        mSetModelGroupIdPromise = promise;
        GroupModelApi.setModelGroupId(meshAddress, modelNo, groupIndex, instance, groupAddress);
    }

    private void onUpdateGroupCompleted(Bundle data) {
        if (D) Log.d(TAG, "onUpdateGroupCompleted");
        // showBundleData(data);

        if (mSetModelGroupIdPromise != null) {
            WritableMap params = Arguments.createMap();
            params.putInt("meshAddress", data.getInt(MeshConstants.EXTRA_DEVICE_ID));
            params.putInt("modelNo", data.getInt(MeshConstants.EXTRA_MODEL_NO));
            params.putInt("groupIndex", data.getInt(MeshConstants.EXTRA_GROUP_INDEX));
            params.putInt("groupAddress", data.getInt(MeshConstants.EXTRA_GROUP_ID));
            mSetModelGroupIdPromise.resolve(params);
        }
        mSetModelGroupIdPromise = null;
    }

    private void onUpdateGroupFailure() {
        if (D) Log.d(TAG, "onUpdateGroupFailure");
        if (mSetModelGroupIdPromise != null) {
            mSetModelGroupIdPromise.reject(new Exception("onUpdateGroupFailure"));
        }
        mSetModelGroupIdPromise = null;
    }

    public static void showBundleData(Bundle bundle) {
        if (bundle == null) {
            return;
        }
        String string = "Bundle{";
        Log.d(TAG, "Bundle{");
        for (String key : bundle.keySet()) {
            string += " " + key + " => " + bundle.get(key) + ";";
            Log.d(TAG, " " + key + " => " + bundle.get(key) + ";");
        }
        string += " }Bundle";
        Log.d(TAG, " }Bundle");
    }

    private void onLeScan(Bundle data) {
        // showBundleData(data);

        ParcelUuid uuid = data.getParcelable(MeshConstants.EXTRA_UUID);
        String macAddress = uuid.getUuid().toString();
        WritableMap params = Arguments.createMap();
        if (mConfigNodePromise != null && mConfigNodeResetMacAddress.equals(macAddress)) {
            stopScan();
            mConfigNodeResetMacAddress = "";
            mConfigNodePromise.resolve(params);
            mConfigNodePromise = null;
        } else {
            params.putString("macAddress", macAddress);
            // params.putString("deviceName", deviceInfo.deviceName);
            // params.putString("meshName", deviceInfo.meshName);
            // params.putInt("meshAddress", deviceInfo.meshAddress);
            // params.putInt("meshUUID", uuid.toString());
            // params.putInt("productUUID", deviceInfo.productUUID);
            // params.putInt("status", deviceInfo.status);
            sendEvent(LE_SCAN, params);
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((MeshService.LocalBinder) rawBinder).getService();
            if (mService != null) {
                mService.setHandler(mMeshHandler);
                mService.setLeScanCallback(mScanCallBack);
                sendEvent(SERVICE_CONNECTED);
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
            sendEvent(SERVICE_DISCONNECTED);
        }
    };

    // private static final String BRIDGE_ADDRESS = "00:00:00:00:23:29";
    private BluetoothAdapter.LeScanCallback mScanCallBack = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (mService != null)
                // if (device.getAddress().equalsIgnoreCase(BRIDGE_ADDRESS)) {
                //     Log.d(TAG, "Connecting to bridge: " + BRIDGE_ADDRESS);
                //     mService.connectBridge(device);
                //     mService.setContinuousLeScanEnabled(false);
                // }
                if (mService.processMeshAdvert(device, scanRecord, rssi)) {
                    // Log.d(TAG, "to processMeshAdvert");
                    // Log.d(TAG, "device.getAddress: " + device.getAddress());
                    // Log.d(TAG, "scanRecord[0]: " + scanRecord[0]);
                    // Log.d(TAG, "rssi: " + rssi);
                }
        }
    };

    /**
     * Handle messages from mesh service.
     */
    private final Handler mMeshHandler = new MeshHandler(this);

    private static class MeshHandler extends Handler {
        private final WeakReference<CsrBtNativeModule> mParent;

        public MeshHandler(CsrBtNativeModule module) {
            super(Looper.getMainLooper());
            mParent = new WeakReference<CsrBtNativeModule>(module);
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            Log.d(TAG, "handleMessage: " + msg.what);
            switch (msg.what) {
                case MeshConstants.MESSAGE_LE_BEARER_READY:
                    mParent.get().connectBluetooth();
                    break;
                case MeshConstants.MESSAGE_LE_CONNECTED: {
                    Log.d(TAG, "MeshConstants.MESSAGE_LE_CONNECTED " + data.getString(MeshConstants.EXTRA_DEVICE_ADDRESS));
                    mParent.get().isChannelReady = true;
                    mParent.get().sendEvent(DEVICE_STATUS_LOGIN);
                    break;
                }
                case MeshConstants.MESSAGE_LE_DISCONNECTED: {
                    Log.d(TAG, "Response MESSAGE_LE_DISCONNECTED");
                    mParent.get().isChannelReady = false;
                    mParent.get().sendEvent(DEVICE_STATUS_LOGOUT);
                    break;
                }
                case MeshConstants.MESSAGE_LE_DISCONNECT_COMPLETE: {
                    Log.d(TAG, "Response MESSAGE_LE_DISCONNECT_COMPLETE");
                    mParent.get().isChannelReady = false;
                    mParent.get().sendEvent(DEVICE_STATUS_LOGOUT);
                    break;
                }
                case MeshConstants.MESSAGE_BRIDGE_CONNECT_TIMEOUT: {
                    Log.d(TAG, "Response MESSAGE_BRIDGE_CONNECT_TIMEOUT");
                    mParent.get().isChannelReady = false;
                    mParent.get().sendEvent(DEVICE_STATUS_LOGOUT);
                    break;
                }
                // case MeshConstants.MESSAGE_DEVICE_APPEARANCE:
                //     Log.d(TAG, "MESSAGE_DEVICE_APPEARANCE");
                //     showBundleData(data);
                //     break;
                case MeshConstants.MESSAGE_RESET_DEVICE:
                    Log.d(TAG, "MESSAGE_RESET_DEVICE");
                    /* The application can handle a request to reset here.
                     * It should calculate the signature using ConfigModelApi.computeResetDeviceSignatureWithDeviceHash(long, byte[])
                     * to check the signature is valid.
                     */
                    break;
                // case MeshConstants.MESSAGE_CONFIG_DEVICE_INFO: {
                //     Log.d(TAG, "MESSAGE_CONFIG_DEVICE_INFO");
                //     mParent.get().configDeviceInfo(data);
                //     break;
                // }
                case MeshConstants.MESSAGE_DEVICE_DISCOVERED:
                    Log.d(TAG, "MESSAGE_DEVICE_DISCOVERED");
                    mParent.get().onLeScan(data);
                    break;
                case MeshConstants.MESSAGE_TIMEOUT:
                    Log.d(TAG, "MESSAGE_TIMEOUT");
                    mParent.get().onUpdateGroupFailure();
                    break;
                // case MeshConstants.MESSAGE_ASSOCIATING_DEVICE:
                //     Log.d(TAG, "MESSAGE_ASSOCIATING_DEVICE");
                //     showBundleData(data);
                //     break;
                case MeshConstants.MESSAGE_LOCAL_DEVICE_ASSOCIATED:
                    Log.d(TAG, "MESSAGE_LOCAL_DEVICE_ASSOCIATED");
                    // showBundleData(data);
                    break;
                case MeshConstants.MESSAGE_LOCAL_ASSOCIATION_FAILED:
                    Log.d(TAG, "MESSAGE_LOCAL_ASSOCIATION_FAILED");
                    // showBundleData(data);
                    break;
                case MeshConstants.MESSAGE_ASSOCIATION_PROGRESS:
                    Log.d(TAG, "MESSAGE_ASSOCIATION_PROGRESS");
                    // showBundleData(data);
                    break;
                case MeshConstants.MESSAGE_DEVICE_ASSOCIATED:
                    Log.d(TAG, "MESSAGE_DEVICE_ASSOCIATED");
                    mParent.get().onUpdateMeshCompleted(data);
                    break;
                case MeshConstants.MESSAGE_ASSOCIATION_FAILED:
                    Log.d(TAG, "MESSAGE_ASSOCIATION_FAILED");
                    mParent.get().onUpdateMeshFailure();
                    break;
                case MeshConstants.MESSAGE_GROUP_NUM_GROUPIDS:
                    Log.d(TAG, "MESSAGE_GROUP_NUM_GROUPIDS");
                    mParent.get().onGetNumberOfModelGroupIdsCompleted(data);
                    break;
                case MeshConstants.MESSAGE_GROUP_MODEL_GROUPID:
                    Log.d(TAG, "MESSAGE_GROUP_MODEL_GROUPID");
                    mParent.get().onUpdateGroupCompleted(data);
                    break;
                case MeshConstants.MESSAGE_RECEIVE_BLOCK_DATA: {
                    Log.e(TAG, "MESSAGE_RECEIVE_BLOCK_DATA" + data);
                    break;
                }
                case MeshConstants.MESSAGE_FIRMWARE_VERSION: {
                    Log.d(TAG, "MESSAGE_FIRMWARE_VERSION");
                    // showBundleData(data);
                    break;
                }
                default:
                    break;
            }
        }
    }

    /*********************/
    /** Private methods **/
    /*********************/

    /**
     * Check if is api level 19 or above
     * @return is above api level 19
     */
    private boolean isKitKatOrAbove () {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    /**
     * Send event to javascript
     * @param eventName Name of the event
     * @param params Additional params
     */
    public void sendEvent(String eventName, @Nullable WritableMap params) {
        if (mReactContext.hasActiveCatalystInstance()) {
            if (D) Log.d(TAG, "Sending event: " + eventName);
            mReactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
        }
    }

    public void sendEvent(String eventName, @Nullable WritableArray params) {
        if (mReactContext.hasActiveCatalystInstance()) {
            if (D) Log.d(TAG, "Sending event: " + eventName);
            mReactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
        }
    }

    public void sendEvent(String eventName) {
        if (mReactContext.hasActiveCatalystInstance()) {
            if (D) Log.d(TAG, "Sending event: " + eventName);
            mReactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, null);
        }
    }
}
