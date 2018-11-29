class CsrBt {
    static MESH_ADDRESS_MIN = 0x8001;
    static MESH_ADDRESS_MAX = 0x8FFF;
    static GROUP_ADDRESS_MIN = 0x0000;
    static GROUP_ADDRESS_MAX = 0x7FFF;
    static HUE_MIN = 0;
    static HUE_MAX = 360;
    static SATURATION_MIN = 0;
    static SATURATION_MAX = 100;
    static BRIGHTNESS_MIN = 2;
    static BRIGHTNESS_MAX = 100;
    static COLOR_TEMP_MIN = 1;
    static COLOR_TEMP_MAX = 100;
    static NODE_STATUS_OFF = 0;
    static NODE_STATUS_ON = 1;
    static NODE_STATUS_OFFLINE = 2;
    static DELAY_MS_AFTER_UPDATE_MESH_COMPLETED = 1;

    static passthroughMode = undefined; // send data on serial port to controll bluetooth node
    static lastBrightness = 2;
    static isClaiming = false;

    static doInit() {}

    static doDestroy() {}

    static addListener(eventName, handler) {}

    static removeListener(eventName, handler) {}

    static notModeAutoConnectMesh() {
        return true;
    }

    static setNetworkPassPhrase({
        passPhrase
    }) {}

    // 自动重连
    static autoConnect({
        userMeshName,
        userMeshPwd,
        otaMac
    }) {}

    // 自动刷新 Notify
    static autoRefreshNotify({
        repeatCount,
        Interval
    }) {}

    static idleMode({
        disconnect
    }) {}

    static startScan({
        meshName,
        outOfMeshName,
        timeoutSeconds,
        isSingleNode,
    }) {}

    static changeBriTmpPwr({
        meshAddress,
        brightness,
        colorTemp,
        power,
        dhmKey,
        type,
        name,
        macAddress,
    }) {}

    static changePower({
        meshAddress,
        value
    }) {}

    static changeBrightness({
        meshAddress,
        value
    }) {}

    static changeColorTemp({
        meshAddress,
        value
    }) {}

    static changeColor({
        meshAddress,
        value
    }) {}

    static changeScene({
        meshAddress,
        scene,
        hue = 0,
        saturation = 0,
        value,
        colorIds = [1, 2, 3, 4, 5],
        type,
    }) {}

    static getTypeFromUuid = uuid => parseInt(uuid.slice(4, 8), 16);

    static configNode({
        node,
        cfg,
        isToClaim,
    }) {}

    static getTotalOfGroupIndex({
        meshAddress,
    }) {}

    static setNodeGroupAddr({
        meshAddress,
        groupIndex,
        groupAddress,
    }) {}
}

module.exports = CsrBt;
