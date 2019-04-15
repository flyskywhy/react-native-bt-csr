class CsrBt {
    static MESH_ADDRESS_MIN = 0x8001;
    // static MESH_ADDRESS_MAX = 0x8FFF;
    static MESH_ADDRESS_MAX = 0x8000 + 20; // 1100 字节的二维码最多分享 20 个蓝牙设备
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
    static RELAY_TIMES_MAX = 16;
    static DELAY_MS_AFTER_UPDATE_MESH_COMPLETED = 1;
    static DELAY_MS_COMMAND = 320;
    static ALARM_CREATE = 0;
    static ALARM_REMOVE = 1;
    static ALARM_UPDATE = 2;
    static ALARM_ENABLE = 3;
    static ALARM_DISABLE = 4;
    static ALARM_ACTION_TURN_OFF = 0;
    static ALARM_ACTION_TURN_ON = 1;
    static ALARM_ACTION_SCENE = 2;
    static ALARM_TYPE_DAY = 0;
    static ALARM_TYPE_WEEK = 1;

    static passthroughMode = undefined; // send data on serial port to controll bluetooth node
    static lastBrightness = 2;
    static isClaiming = false;

    static doInit() {}

    static doDestroy() {}

    static addListener(eventName, handler) {}

    static removeListener(eventName, handler) {}

    static enableBluetooth() {}

    static enableSystemLocation() {}

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

    static remind({
        meshAddress,
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

    static setTime({
        meshAddress,
        year,
        month,
        day,
        hour,
        minute,
        second = 0,
    }) {}

    static getTime({
        meshAddress,
        relayTimes,
    }) {}

    static setAlarm({
        meshAddress,
        crud,
        alarmId,
        status,
        action,
        type,
        month = 1,
        dayOrweek,
        hour,
        minute,
        second = 0,
        sceneId = 0,
    }) {}

    static getAlarm({
        meshAddress,
        relayTimes,
        alarmId,
    }) {}
}

module.exports = CsrBt;
