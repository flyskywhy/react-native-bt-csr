const {
    NativeModules,
    DeviceEventEmitter,
} = require('react-native');
const NativeModule = NativeModules.CsrBt;

class CsrBt {
    static MESH_ADDRESS_MIN = 0x8001;
    static MESH_ADDRESS_MAX = 0x8FFF;
    static BRIGHTNESS_MIN = 1;
    static BRIGHTNESS_MAX = 127;
    static COLOR_TEMP_MIN = 1;
    static COLOR_TEMP_MAX = 127;
    static NODE_STATUS_OFF = 0;
    static NODE_STATUS_ON = 1;
    static NODE_STATUS_OFFLINE = 2;
    static DELAY_MS_AFTER_UPDATE_MESH_COMPLETED = 1;

    static doInit() {
        NativeModule.doInit();
    }

    static doDestroy() {
        NativeModule.doDestroy();
    }

    static addListener(eventName, handler) {
        DeviceEventEmitter.addListener(eventName, handler);
    }

    static removeListener(eventName, handler) {
        DeviceEventEmitter.removeListener(eventName, handler);
    }

    static notModeAutoConnectMesh() {
        return NativeModule.notModeAutoConnectMesh();
    }

    static setNetworkPassPhrase({
        passPhrase
    }) {
        return NativeModule.setNetworkPassPhrase(passPhrase);
    }

    // 自动重连
    static autoConnect({
        userMeshName,
        userMeshPwd,
        otaMac
    }) {
        return NativeModule.autoConnect(userMeshName, userMeshPwd, otaMac);
    }

    // 自动刷新 Notify
    static autoRefreshNotify({
        repeatCount,
        Interval
    }) {
        return NativeModule.autoRefreshNotify(repeatCount, Interval);
    }

    static idleMode({
        disconnect
    }) {
        return NativeModule.idleMode(disconnect);
    }

    static startScan({
        meshName,
        outOfMeshName,
        timeoutSeconds,
        isSingleNode,
    }) {
        NativeModule.startScan();
        setTimeout(() => {
            NativeModule.stopScan();
        }, timeoutSeconds * 1000);
    }

    static changePower({
        meshAddress,
        value
    }) {
        NativeModule.changePower(meshAddress, value);
    }

    static changeBrightness({
        meshAddress,
        value
    }) {
        NativeModule.changeBrightness(meshAddress, value);
    }

    static changeColorTemp({
        meshAddress,
        value
    }) {
        NativeModule.changeColorTemp(meshAddress, value);
    }

    static changeColor({
        meshAddress,
        value
    }) {
        NativeModule.changeColor(meshAddress, value);
    }

    static configNode({
        node,
        cfg,
    }) {
        return NativeModule.configNode(node, cfg);
    }
}

module.exports = CsrBt;
