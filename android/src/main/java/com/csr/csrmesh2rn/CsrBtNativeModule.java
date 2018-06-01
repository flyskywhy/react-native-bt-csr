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
import com.csr.csrmesh2.MeshConstants;
import com.csr.csrmesh2.MeshService;
import com.csr.csrmesh2.LightModelApi;
import com.csr.csrmesh2.PowerModelApi;
import com.csr.csrmesh2.PowerState;
import com.csr.csrmesh2.TimeModelApi;

import static com.csr.csrmesh2rn.CsrBtPackage.TAG;
import com.csr.csrmesh2rn.utils.Hex;

public class CsrBtNativeModule extends ReactContextBaseJavaModule implements ActivityEventListener, LifecycleEventListener {

    // Debugging
    private static final boolean D = true;

    private static final int ACCESS_COARSE_LOCATION_RESULT_CODE = 4;
    private static final int BLUETOOTH_RESULT_CODE = 5;
    private static final int STORAGE_RESULT_CODE = 6;

    private static int MIN_DEVICE_ID = 0x8000;

    // Event names
    public static final String BT_ENABLED = "bluetoothEnabled";
    public static final String BT_DISABLED = "bluetoothDisabled";
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
    protected int mReqId = -1;
    protected String mDhmKey;

    // Patch
    private String mPatchConfigNodeOldName;

    // Promises
    private Promise mConfigNodePromise;

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

        checkLocation();

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
        registerBluetoothStateReceiver();
    }

    @ReactMethod
    public void doDestroy() {
        if (isInited) {
            mHandler.removeCallbacksAndMessages(null);
            isInited = false;
        }

        if (isServiceStarted) {
            isServiceStarted = false;
            if (mService != null) {
                mContext.unbindService(mServiceConnection);
                mService = null;
            }
        }

        // TODO: unregisterReceiver(bluetoothStateReceiver);
    }

    @ReactMethod
    protected void doResume() {
        //检查是否支持蓝牙设备
        // if (!LeBluetooth.getInstance().isSupport(mContext)) {
        //     Toast.makeText(mContext, "ble not support", Toast.LENGTH_SHORT).show();
        //     return;
        // }

        // if (!LeBluetooth.getInstance().isEnabled()) {
        //     AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //     builder.setMessage("开启蓝牙，体验智能灯!");
        //     builder.setNeutralButton("cancel", new DialogInterface.OnClickListener() {
        //         @Override
        //         public void onClick(DialogInterface dialog, int which) {
        //             finish();
        //         }
        //     });
        //     builder.setNegativeButton("enable", new DialogInterface.OnClickListener() {
        //         @Override
        //         public void onClick(DialogInterface dialog, int which) {
        //             LeBluetooth.getInstance().enable(getApplicationContext());
        //         }
        //     });
        //     builder.show();
        // }

        // DeviceInfo deviceInfo = this.mApplication.getConnectDevice();

        // if (deviceInfo != null) {
        //     this.connectMeshAddress = this.mApplication.getConnectDevice().meshAddress & 0xFF;
        // }

        // Broadcasting time to the mesh network whenever we resume the app.
        // MeshLibraryManager.MeshChannel channel = MeshLibraryManager.getInstance().getChannel();
        if (mService != null) {
            final int MS_IN_15_MINS = 15 * 60 * 1000;
            final byte utcOffset = (byte)(TimeZone.getDefault().getOffset(Calendar.getInstance().getTimeInMillis()) / MS_IN_15_MINS);
// Log.d(TAG, "TimeInMillis" + Calendar.getInstance().getTimeInMillis());
// Log.d(TAG, "utcOffset" + utcOffset);
//             TimeModelApi.broadcastTime(Calendar.getInstance().getTimeInMillis(), utcOffset, true);
        }

        checkPermissions();
        checkAvailability();

        Log.d(TAG, "onResume");
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

    public void checkLocation() {
        if (Build.VERSION.SDK_INT >=23) {
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
            if (gps_enabled == false || network_enabled == false) {
                // Show our settings alert and let the use turn on the GPS/Location
                // showBTStatusDialog(false);
            }


        }
    }

    @ReactMethod
    public void notModeAutoConnectMesh(Promise promise) {
promise.resolve(true);
// promise.resolve(mService.isAutoConnectEnabled());
        // promise.resolve(TelinkLightService.Instance().getMode() != LightAdapter.MODE_AUTO_CONNECT_MESH);
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
    private void autoConnect(String userMeshName, String userMeshPwd, String otaMac) {
        // LeAutoConnectParameters connectParams = Parameters.createAutoConnectParameters();
        // connectParams.setMeshName(userMeshName);
        // connectParams.setPassword(userMeshPwd);
        // connectParams.autoEnableNotification(true);

        // if (TextUtils.isEmpty(otaMac))  {
        //     mTelinkApplication.saveLog("Action: AutoConnect:NULL");
        // } else {    // 之前是否有在做 MeshOTA 操作，是则继续
        //     connectParams.setConnectMac(otaMac);
        //     mTelinkApplication.saveLog("Action: AutoConnect:" + otaMac);
        // }

        // TelinkLightService.Instance().autoConnect(connectParams);
        if (isConnected()) {
            sendEvent(DEVICE_STATUS_LOGIN);
        } else {
Log.d(TAG, "prepare autoConnect");
//         if (Build.VERSION.SDK_INT >= 21) {
// Log.d(TAG, "ScanSettings.SCAN_MODE_LOW_LATENCY");
//             mService.setBluetoothBearerEnabled(ScanSettings.SCAN_MODE_LOW_LATENCY);
//         }
//         else {
            sendEvent(DEVICE_STATUS_LOGOUT);
            if (mService.getActiveBearer() == MeshService.Bearer.BLUETOOTH) {
                connectBluetooth();
            } else {
                mService.setBluetoothBearerEnabled();
            }
        // }
        }
    }

    /**
     * Set bluetooth as bearer and connect to a bridge
     */
    private void connectBluetooth() {
Log.d(TAG, "start autoConnect");
mService.setTTL((byte)100);
Log.d(TAG, "xxxxxgetTTL" + mService.getTTL());
// mService.setControllerAddress(32769);
        mService.setMeshListeningMode(true, false);
        mService.startAutoConnect(1);
        //mService.setContinuousLeScanEnabled(true);
    }

    @ReactMethod
    private void autoRefreshNotify(int repeatCount, int Interval) {
        // LeRefreshNotifyParameters refreshNotifyParams = Parameters.createRefreshNotifyParameters();
        // refreshNotifyParams.setRefreshRepeatCount(repeatCount);
        // refreshNotifyParams.setRefreshInterval(Interval);

        // TelinkLightService.Instance().autoRefreshNotify(refreshNotifyParams);
    }

    @ReactMethod
    private void idleMode(boolean disconnect) {
        // TelinkLightService.Instance().idleMode(disconnect);
    }

    @ReactMethod
    private void startScan() {
        // LeScanParameters params = LeScanParameters.create();
        // params.setMeshName(meshName);
        // params.setOutOfMeshName(outOfMeshName);
        // params.setTimeoutSeconds(timeoutSeconds);
        // params.setScanMode(isSingleNode);
        // TelinkLightService.Instance().startScan(params);
Log.d(TAG, "startScan");
mService.setDeviceDiscoveryFilterEnabled(true);
    }

    @ReactMethod
    private void stopScan() {
Log.d(TAG, "stopScan");
        mService.setDeviceDiscoveryFilterEnabled(false);
    }

    @ReactMethod
    private void changePower(int meshAddress, int value) {
        if (value >= 0 && value < PowerState.values().length) {
Log.d(TAG, "xxxxxxmeshAddress: " + meshAddress);
Log.d(TAG, "xxxxxxvalue: " + value);
Log.d(TAG, "xxxxxxPowerState: " + PowerState.values()[value]);
// LightModelApi.getState(meshAddress);
// ConfigModelApi.getInfo(meshAddress, DeviceInfo.MODEL_LOW);
            PowerModelApi.setState(meshAddress, PowerState.values()[value], true);
            // PowerModelApi.toggleState(meshAddress, true);
        }
    }

    @ReactMethod
    private void changeBrightness(int meshAddress, int value) {
    }

    @ReactMethod
    private void changeColorTemp(int meshAddress, int value) {
    }

    @ReactMethod
    private void changeColor(int meshAddress, int value) {
        // byte red = (byte) (value >> 16 & 0xFF);
        // byte green = (byte) (value >> 8 & 0xFF);
        // byte blue = (byte) (value & 0xFF);

        // byte opcode = (byte) 0xE2;
        // byte[] params = new byte[]{0x04, red, green, blue};

        // TelinkLightService.Instance().sendCommandNoResponse(opcode, meshAddress, params);
    }

    @ReactMethod
    private void configNode(ReadableMap node, boolean isToClaim, Promise promise) {
        mConfigNodePromise = promise;
        if (isToClaim) {
            mService.associateDevice(Integer.parseInt(node.getString("macAddress")), 0, false, node.getInt("meshAddress"));
        } else {
            mConfigNodePromise.resolve(true);
            mConfigNodePromise = null;
            mService.resetDevice(node.getInt("meshAddress"), Hex.decodeHex(node.getString("dhmKey").toCharArray()));
        }
    }

    private void onUpdateMeshCompleted(Bundle data) {
        if (D) Log.d(TAG, "onUpdateMeshCompleted");
showBundleData(data);

        int deviceId = data.getInt(MeshConstants.EXTRA_DEVICE_ID);
        mDhmKey = Hex.encodeHexStr(data.getByteArray(MeshConstants.EXTRA_RESET_KEY));
        WritableArray array = Arguments.createArray();
        mReqId = ConfigModelApi.getInfo(deviceId, DeviceInfo.APPEARANCE);
    }

    private void onUpdateMeshFailure() {
        if (D) Log.d(TAG, "onUpdateMeshFailure");
        if (mConfigNodePromise != null) {
            mConfigNodePromise.reject(new Exception("onUpdateMeshFailure"));
        }
        mConfigNodePromise = null;
    }

    private void configDeviceInfo(Bundle data) {
showBundleData(data);
        int deviceId = data.getInt(MeshConstants.EXTRA_DEVICE_ID);
        DeviceInfo type = DeviceInfo.values()[data.getInt(MeshConstants.EXTRA_DEVICE_INFO_TYPE)];
        if (type == DeviceInfo.APPEARANCE) {
            if (mConfigNodePromise != null) {
                WritableMap params = Arguments.createMap();
                params.putString("name", getNameByAppearance((int) data.getLong(MeshConstants.EXTRA_DEVICE_INFORMATION)) + " " + (deviceId - MIN_DEVICE_ID));
                params.putString("dhmKey", mDhmKey);
                mConfigNodePromise.resolve(params);
            }
            mConfigNodePromise = null;
        }
    }

    private String getNameByAppearance(int appearance) {
        if (appearance == MeshConstants.LIGHT_APPEARANCE) {
            return "Light";
        }
        else if (appearance == MeshConstants.HEATER_APPEARANCE) {
            return "Heater";
        }
        else if (appearance == MeshConstants.SENSOR_APPEARANCE) {
            return "Sensor";
        }
        else if (appearance == MeshConstants.CONTROLLER_APPEARANCE) {
            return "Controller";
        }
        else {
            return "Unknown";
        }
    }

    // private void onNError(final DeviceEvent event) {
    //     // TelinkLightService.Instance().idleMode(true);
    //     // TelinkLog.d("DeviceScanningActivity#onNError");
    //     sendEvent(DEVICE_STATUS_ERROR_N);
    // }

    // private void onDeviceStatusChanged(DeviceEvent event) {
    //     DeviceInfo deviceInfo = event.getArgs();

    //     switch (deviceInfo.status) {
    //         case LightAdapter.STATUS_LOGIN:
    //             // mHandler.postDelayed(new Runnable() {
    //             //     @Override
    //             //     public void run() {
    //             //         TelinkLightService.Instance().sendCommandNoResponse((byte) 0xE4, 0xFFFF, new byte[]{});
    //             //     }
    //             // }, 3 * 1000);

    //             // WritableMap params = Arguments.createMap();
    //             // params.putInt("connectMeshAddress", mTelinkApplication.getConnectDevice().meshAddress);
    //             sendEvent(DEVICE_STATUS_LOGIN, params);
    //             break;
    //         case LightAdapter.STATUS_CONNECTING:
    //             break;
    //         case LightAdapter.STATUS_LOGOUT:
    //             sendEvent(DEVICE_STATUS_LOGOUT);
    //             break;
    //         case LightAdapter.STATUS_UPDATE_MESH_COMPLETED:
    //             onUpdateMeshCompleted();
    //             break;
    //         case LightAdapter.STATUS_UPDATE_MESH_FAILURE:
    //             onUpdateMeshFailure(deviceInfo);
    //             break;
    //         case LightAdapter.STATUS_ERROR_N:
    //             onNError(event);
    //         default:
    //             break;
    //     }
    // }

    /**
     * 处理{@link NotificationEvent#ONLINE_STATUS}事件
     */
    // private synchronized void onOnlineStatusNotify(NotificationEvent event) {
        // TelinkLog.i("MainActivity#onOnlineStatusNotify#Thread ID : " + Thread.currentThread().getId());
        // List<OnlineStatusNotificationParser.DeviceNotificationInfo> notificationInfoList;
        // //noinspection unchecked
        // notificationInfoList = (List<OnlineStatusNotificationParser.DeviceNotificationInfo>) event.parse();

        // if (notificationInfoList == null || notificationInfoList.size() <= 0)
        //     return;

        // WritableArray params = Arguments.createArray();
        // for (OnlineStatusNotificationParser.DeviceNotificationInfo notificationInfo : notificationInfoList) {
        //     WritableMap map = Arguments.createMap();
        //     map.putInt("meshAddress", notificationInfo.meshAddress);
        //     map.putInt("brightness", notificationInfo.brightness);
        //     map.putInt("status", notificationInfo.connectionStatus.getValue());
        //     params.pushMap(map);
        // }
        // sendEvent(NOTIFICATION_ONLINE_STATUS, params);
    // }

    // AlertDialog.Builder mTimeoutBuilder;

    // private void onMeshOffline(MeshEvent event) {
    //     onUpdateMeshFailure();
    //     sendEvent(MESH_OFFLINE);
    // }

    // private void onNotificationEvent(NotificationEvent event) {
        // if (!foreground) return;
        // // 解析版本信息
        // byte[] data = event.getArgs().params;
        // if (data[0] == NotificationEvent.DATA_GET_MESH_OTA_PROGRESS) {
        //     TelinkLog.w("mesh ota progress: " + data[1]);
        //     int progress = (int) data[1];
        //     if (progress != 100) {
        //         startActivity(new Intent(this, OTAUpdateActivity.class)
        //                 .putExtra(OTAUpdateActivity.INTENT_KEY_CONTINUE_MESH_OTA, OTAUpdateActivity.CONTINUE_BY_REPORT)
        //                 .putExtra("progress", progress));
        //     }
        // }
    // }

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
        showBundleData(data);

        ParcelUuid uuid = data.getParcelable(MeshConstants.EXTRA_UUID);
        int uuidHash = data.getInt(MeshConstants.EXTRA_UUIDHASH_31);
        int rssi = data.getInt(MeshConstants.EXTRA_RSSI);
        int ttl = data.getInt(MeshConstants.EXTRA_TTL);
        WritableMap params = Arguments.createMap();
        params.putString("macAddress", uuidHash + "");
        // params.putString("deviceName", deviceInfo.deviceName);
        // params.putString("meshName", deviceInfo.meshName);
        // params.putInt("meshAddress", deviceInfo.meshAddress);
        // params.putInt("meshUUID", uuid.toString());
        // params.putInt("productUUID", deviceInfo.productUUID);
        // params.putInt("status", deviceInfo.status);
        sendEvent(LE_SCAN, params);
    }

    // private void onMeshEventUpdateCompleted(MeshEvent event) {
    // }

    // private void onMeshEventError(MeshEvent event) {
    // }


    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((MeshService.LocalBinder) rawBinder).getService();
            if (mService != null) {
                mService.setHandler(mMeshHandler);
                mService.setLeScanCallback(mScanCallBack);
                sendEvent(SERVICE_CONNECTED);

/* Set the Bluetooth bearer. This will start the stack, but
   we don't connect until we receive MESSAGE_LE_BEARER_READY.*/
// enableBluetooth();
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
            sendEvent(SERVICE_DISCONNECTED);
        }
    };

    private static final String BRIDGE_ADDRESS = "00:00:00:00:23:29";
    private BluetoothAdapter.LeScanCallback mScanCallBack = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (mService != null)

/*
                if (device.getAddress().equalsIgnoreCase(BRIDGE_ADDRESS)) {
                    Log.d(TAG, "Connecting to bridge: " + BRIDGE_ADDRESS);
                    mService.connectBridge(device);
                    mService.setContinuousLeScanEnabled(false);
                }*/
                // if (device.getAddress().equalsIgnoreCase(BRIDGE_ADDRESS)) {
                //     Log.d(TAG, "Connecting to bridge: " + BRIDGE_ADDRESS);
                //     mService.connectBridge(device);
                // }
                if (mService.processMeshAdvert(device, scanRecord, rssi)) {
Log.d(TAG, "to processMeshAdvert");
Log.d(TAG, "device.getAddress: " + device.getAddress());
Log.d(TAG, "scanRecord[0]: " + scanRecord[0]);
Log.d(TAG, "rssi: " + rssi);
                    // Notify about the new device scanned.
                    // {
                    //     Bundle data = new Bundle();
                    //     data.putParcelable(MeshConstants.EXTRA_DEVICE, device);
                    //     data.putInt(MeshConstants.EXTRA_RSSI, rssi);
                    //     App.bus.post(new MeshSystemEvent(MeshSystemEvent.SystemEvent.DEVICE_SCANNED, data));
                    // }
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
                case MeshConstants.MESSAGE_CONFIG_DEVICE_INFO: {
                    Log.d(TAG, "MESSAGE_CONFIG_DEVICE_INFO");
                    mParent.get().configDeviceInfo(data);
                    break;
                }
                case MeshConstants.MESSAGE_DEVICE_DISCOVERED:
                    Log.d(TAG, "MESSAGE_DEVICE_DISCOVERED");
                    mParent.get().onLeScan(data);
                    break;
                case MeshConstants.MESSAGE_TIMEOUT:
                    Log.d(TAG, "MESSAGE_TIMEOUT");
                    showBundleData(data);
                    break;
                // case MeshConstants.MESSAGE_ASSOCIATING_DEVICE:
                //     Log.d(TAG, "MESSAGE_ASSOCIATING_DEVICE");
                //     showBundleData(data);
                //     break;
                case MeshConstants.MESSAGE_LOCAL_DEVICE_ASSOCIATED:
                    Log.d(TAG, "MESSAGE_LOCAL_DEVICE_ASSOCIATED");
                    showBundleData(data);
                    break;
                case MeshConstants.MESSAGE_LOCAL_ASSOCIATION_FAILED:
                    Log.d(TAG, "MESSAGE_LOCAL_ASSOCIATION_FAILED");
                    showBundleData(data);
                    break;
                case MeshConstants.MESSAGE_ASSOCIATION_PROGRESS:
                    Log.d(TAG, "MESSAGE_ASSOCIATION_PROGRESS");
                    showBundleData(data);
                    break;
                case MeshConstants.MESSAGE_DEVICE_ASSOCIATED:
                    Log.d(TAG, "MESSAGE_DEVICE_ASSOCIATED");
                    mParent.get().onUpdateMeshCompleted(data);
                    break;
                case MeshConstants.MESSAGE_RECEIVE_BLOCK_DATA: {  /* SkyLine_1 */
                    Log.e(TAG, "MESSAGE_RECEIVE_BLOCK_DATA" + data);
                    // SendDataToServer.addSendDataToServer(data);
                    // App.bus.post(new MeshResponseEvent(MeshResponseEvent.ResponseEvent.DATA_RECEIVE_BLOCK, data));
                    break;
                }
                default:
                    break;
            }
        }

                // case MeshService.MESSAGE_LE_CONNECTED: {
                //     parentActivity.mConnectedDevices.add(msg.getData().getString(MeshService.EXTRA_DEVICE_ADDRESS));
                //     if (!parentActivity.mConnected) {
                //         parentActivity.onConnected();
                //     }
                //     break;
                // }
    }

    /**
     * 事件处理方法
     *
     * @param event
     */
    // @Override
    // public void performed(Event<String> event) {
    //     switch (event.getType()) {
    //         case NotificationEvent.ONLINE_STATUS:
    //             this.onOnlineStatusNotify((NotificationEvent) event);
    //             break;
    //         case DeviceEvent.STATUS_CHANGED:
    //             this.onDeviceStatusChanged((DeviceEvent) event);
    //             break;
    //         case ServiceEvent.SERVICE_CONNECTED:
    //             this.onServiceConnected((ServiceEvent) event);
    //             break;
    //         case ServiceEvent.SERVICE_DISCONNECTED:
    //             this.onServiceDisconnected((ServiceEvent) event);
    //             break;
    //         case NotificationEvent.GET_DEVICE_STATE:
    //             onNotificationEvent((NotificationEvent) event);
    //             break;
    //         case LeScanEvent.LE_SCAN:
    //             onLeScan((LeScanEvent) event);
    //             break;
    //         case LeScanEvent.LE_SCAN_TIMEOUT:
    //             sendEvent(LE_SCAN_TIMEOUT);
    //             break;
    //         case LeScanEvent.LE_SCAN_COMPLETED:
    //             sendEvent(LE_SCAN_COMPLETED);
    //             break;
    //         case MeshEvent.OFFLINE:
    //             this.onMeshOffline((MeshEvent) event);
    //             break;
    //         case MeshEvent.UPDATE_COMPLETED:
    //             onMeshEventUpdateCompleted((MeshEvent) event);
    //             break;
    //         case MeshEvent.ERROR:
    //             onMeshEventError((MeshEvent) event);
    //             break;
    //     }
    // }

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

    /**
     * Register receiver for bluetooth state change
     */
    private void registerBluetoothStateReceiver() {
        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
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
                            // TelinkLightService.Instance().idleMode(true);
                            // autoConnect();
                            sendEvent(BT_ENABLED);
                            break;
                    }
                }
            }
        };

        mReactContext.registerReceiver(bluetoothStateReceiver, intentFilter);
    }
}
