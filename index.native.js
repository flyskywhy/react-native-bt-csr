const {
    NativeModules,
    DeviceEventEmitter,
} = require('react-native');
const NativeModule = NativeModules.CsrBt;

var tinycolor = require("tinycolor2");

class CsrBt {
    static MESH_ADDRESS_MIN = 0x8001;
    static MESH_ADDRESS_MAX = 0x8FFF;
    static GROUP_ADDRESS_MIN = 0x0000;
    static GROUP_ADDRESS_MAX = 0x7FFF;
    static GROUP_ADDRESS_MASK = 0x7FFF;
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
    static gamma = [  // gamma 2.4 ，normal color ，据说较暗时颜色经 gamma 校正后会比较准
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,                                 // 0
        0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2,                                 // 16
        2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4,                                 // 32
        5, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7, 8, 8, 8, 9, 9,                                 // 48
        9, 10, 10, 10, 11, 11, 11, 12, 12, 13, 13, 14, 14, 14, 15, 15,                  // 64
        16, 16, 17, 17, 18, 18, 19, 19, 20, 20, 21, 22, 22, 23, 23, 24,                 // 80
        24, 25, 26, 26, 27, 28, 28, 29, 30, 30, 31, 32, 32, 33, 34, 35,                 // 96
        35, 36, 37, 38, 39, 39, 40, 41, 42, 43, 43, 44, 45, 46, 47, 48,                 // 112
        49, 50, 51, 52, 53, 53, 54, 55, 56, 57, 58, 59, 60, 62, 63, 64,                 // 128
        65, 66, 67, 68, 69, 70, 71, 73, 74, 75, 76, 77, 78, 80, 81, 82,                 // 144
        83, 85, 86, 87, 88, 90, 91, 92, 94, 95, 96, 98, 99, 100, 102, 103,              // 160
        105, 106, 108, 109, 111, 112, 114, 115, 117, 118, 120, 121, 123, 124, 126, 127, // 176
        129, 131, 132, 134, 136, 137, 139, 141, 142, 144, 146, 148, 149, 151, 153, 155, // 192
        156, 158, 160, 162, 164, 166, 167, 169, 171, 173, 175, 177, 179, 181, 183, 185, // 208
        187, 189, 191, 193, 195, 197, 199, 201, 203, 205, 207, 210, 212, 214, 216, 218, // 224
        220, 223, 225, 227, 229, 232, 234, 236, 239, 241, 243, 246, 248, 250, 253, 255  // 240
    ];
    // static gamma = [  // gamma 2.8 ，vivid color ，据说较明亮时颜色经 gamma 校正后会比较准
    //     0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,                                 // 0
    //     1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,                                 // 16
    //     1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3,                                 // 32
    //     3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 6,                                 // 48
    //     6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 8, 9, 9, 9, 10, 10,                               // 64
    //     10, 11, 11, 12, 12, 12, 13, 13, 13, 14, 14, 15, 15, 16, 16, 17,                 // 80
    //     17, 18, 18, 19, 19, 20, 20, 21, 21, 22, 22, 23, 24, 24, 25, 25,                 // 96
    //     26, 27, 27, 28, 29, 29, 30, 31, 31, 32, 33, 34, 34, 35, 36, 37,                 // 112
    //     38, 38, 39, 40, 41, 42, 43, 43, 44, 45, 46, 47, 48, 49, 50, 51,                 // 128
    //     52, 53, 54, 55, 56, 57, 58, 59, 60, 62, 63, 64, 65, 66, 67, 68,                 // 144
    //     70, 71, 72, 73, 75, 76, 77, 78, 80, 81, 82, 84, 85, 87, 88, 89,                 // 160
    //     91, 92, 94, 95, 97, 98, 100, 101, 103, 104, 106, 108, 109, 111, 112, 114,       // 176
    //     116, 117, 119, 121, 123, 124, 126, 128, 130, 131, 133, 135, 137, 139, 141, 143, // 192
    //     145, 147, 149, 151, 153, 155, 157, 159, 161, 163, 165, 167, 169, 171, 173, 176, // 208
    //     178, 180, 182, 185, 187, 189, 192, 194, 196, 199, 201, 203, 206, 208, 211, 213, // 224
    //     216, 218, 221, 223, 226, 228, 231, 234, 236, 239, 242, 244, 247, 250, 253, 255  // 240
    // ];
    static whiteBalance = { // cooler
        r: 1,
        g: 0.6,
        b: 0.24,
    };
    // static whiteBalance = { // warmer
    //     r: 1,
    //     g: 0.5,
    //     b: 0.18,
    // };
    static lastBrightness = 2;
    static isClaiming = false;

    static otaFileVersionOffset = 4;    // 把二进制固件作为一个字节数组看待的话，描述着版本号的第一个字节的数组地址
    static otaFileVersionLength = 2;    // 二进制固件中描述版本号用了几个字节

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

    static enableBluetooth() {
        NativeModule.enableBluetooth();
    }

    static enableSystemLocation() {
        NativeModule.enableSystemLocation();
    }

    static notModeAutoConnectMesh() {
        return NativeModule.notModeAutoConnectMesh();
    }

    static setNetworkPassPhrase({
        passPhrase
    }) {
        return NativeModule.setNetworkPassPhrase(passPhrase);
    }

    static autoConnect({
        userMeshName,
        userMeshPwd,
        otaMac
    }) {
        return NativeModule.autoConnect(userMeshName, userMeshPwd, otaMac);
    }

    static autoRefreshNotify({
        repeatCount,
        Interval
    }) {}

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

    static sendData({
        meshAddress,
        value,
    }) {
        NativeModule.sendData(meshAddress, value);
    }

    static maxTo255(value, max) {
        return parseInt(value * 255 / max, 10);
    }

    static changeBriTmpPwr({
        meshAddress,
        brightness,
        colorTemp,
        power,
        dhmKey,
        type,
        name,
        macAddress,
    }) {
        for (let mode in this.passthroughMode) {
            if (this.passthroughMode[mode].includes(type)) {
                if (mode === 'sllc') {
                    let data = 'st00';
                    data += this.padHexString(this.maxTo255(brightness, this.BRIGHTNESS_MAX).toString(16));
                    data += this.padHexString(this.maxTo255(colorTemp, this.COLOR_TEMP_MAX).toString(16));
                    data += power === 1 ? 'op' : 'cl';
                    this.sendData({
                        meshAddress,
                        value: [].map.call(data, str => str.charCodeAt(0)),
                    });
                }

                break;
            }
        }
    }

    static remind({
        meshAddress,
    }) {}

    static isOnline(status) {
        return (status & 0x03) !== this.NODE_STATUS_OFFLINE;
    }

    static isOn(status) {
        return (status & 0x03) === this.NODE_STATUS_ON;
    }

    static changePower({
        meshAddress,
        value,
        type,
        immediate = false,
    }) {
        let changed = false;

        if (this.passthroughMode) {
            for (let mode in this.passthroughMode) {
                if (this.passthroughMode[mode].includes(type)) {
                    if (mode === 'oe') {
                        let data = [0];
                        data.push(0);
                        data.push(32);
                        data.push(value === 1 ? 1 : 0);
                        data.push(4);
                        data.push(0);
                        data.push(0);
                        data.push(0);
                        data.push(0);
                        data.push(0);

                        this.sendData({
                            meshAddress: meshAddress,
                            value: data,
                        });

                        changed = true;
                    } else if (mode === 'sllc') {
                        let data = 'setpwr00';
                        data += value === 1 ? 'op' : 'cl';

                        this.sendData({
                            meshAddress,
                            value: [].map.call(data, str => str.charCodeAt(0)),
                        });

                        changed = true;
                    }

                    break;
                }
            }
        }

        if (!changed) {
            NativeModule.changePower(meshAddress, value);
        }
    }

    static coolWarm({
        warm,
        brightness,
    }) {
        let warmRatio = warm / this.COLOR_TEMP_MAX;
        let cool4ArrayOfByte = 255 * (1 - warmRatio);
        let warm4ArrayOfByte = 255 - cool4ArrayOfByte;
        let brightnessRatio = brightness / this.BRIGHTNESS_MAX;
        cool4ArrayOfByte *= brightnessRatio;
        warm4ArrayOfByte *= brightnessRatio;

        return {
            cool: cool4ArrayOfByte,
            warm: warm4ArrayOfByte,
            warmRatio,
            brightnessRatio,
        }
    }

    static hsbtb({
        color,
        coolWarm,
        lastBrightnessRatio,
    }) {
        if (!color && !coolWarm) {
            return [];
        }

        let d1 = color ? color.brightnessRatio : coolWarm.brightnessRatio;
        let d2 = lastBrightnessRatio >= 0.0 ? Math.abs(lastBrightnessRatio - d1) : 1.0;
        if (Math.abs(d1) < 1.0E-5) {
            d1 = 0.0;
        }
        let d3 = d2 > 0.6 ? 0.6 : d2;
        let d4 = (0.6 - d3) / 0.6;
        let d5 = d1 === 0.0 ? 4.0 : 4.0 + 12.0 * d4 * Math.pow(1.0 - d1, 3.0);
        let arrayOfByte = [0];
        arrayOfByte.push(19);
        arrayOfByte.push(d5);
        if (color) {
            let c = tinycolor.fromRatio({
                h: color.hueRatio,
                s: color.saturationRatio,
                v: color.brightnessRatio,
            }).toRgb();
            arrayOfByte.push(c.r);
            arrayOfByte.push(c.g);
            arrayOfByte.push(c.b);
        } else {
            arrayOfByte.push(0);
            arrayOfByte.push(0);
            arrayOfByte.push(0);
        }
        if (coolWarm) {
            arrayOfByte.push(parseInt(coolWarm.cool, 10));
            arrayOfByte.push(parseInt(coolWarm.warm, 10));
        } else {
            arrayOfByte.push(0);
            arrayOfByte.push(0);
        }
        arrayOfByte.push(0);

        return arrayOfByte;
    }

    static changeBrightness({
        meshAddress,
        hue = 0,
        saturation = 0,
        value,
        type,
    }) {
        let changed = false;

        if (this.passthroughMode) {
            for (let mode in this.passthroughMode) {
                if (this.passthroughMode[mode].includes(type)) {
                    if (mode === 'oe') {
                        let data = [0];
                        data = data.concat(this.hsbtb({
                            color: hue === -1 ? null : {
                                hueRatio: hue / this.HUE_MAX,
                                saturationRatio: saturation / this.SATURATION_MAX,
                                brightnessRatio: value / this.BRIGHTNESS_MAX,
                            },
                            coolWarm: this.coolWarm({
                                warm: 0,
                                brightness: value,
                            }),
                            lastBrightnessRatio: -1,
                        }));
                        this.lastBrightness = value;

                        this.sendData({
                            meshAddress: meshAddress,
                            value: data,
                        });

                        changed = true;
                    } else if (mode === 'sllc') {
                        let data = 'setbri00';
                        data += this.padHexString(this.maxTo255(value, this.BRIGHTNESS_MAX).toString(16));
                        this.sendData({
                            meshAddress,
                            value: [].map.call(data, str => str.charCodeAt(0)),
                        });

                        changed = true;
                    }

                    break;
                }
            }
        }

        if (!changed) {
            NativeModule.changeBrightness(meshAddress, value);
        }
    }

    static changeColorTemp({
        meshAddress,
        value,
        type,
    }) {
        let changed = false;

        if (this.passthroughMode) {
            for (let mode in this.passthroughMode) {
                if (this.passthroughMode[mode].includes(type)) {
                    if (mode === 'oe') {
                        let data = [0];
                        data = data.concat(this.hsbtb({
                            coolWarm: this.coolWarm({
                                warm: value,
                                brightness: this.lastBrightness,
                            }),
                            lastBrightness: this.lastBrightness / this.BRIGHTNESS_MAX,
                        }));

                        this.sendData({
                            meshAddress: meshAddress,
                            value: data,
                        });

                        changed = true;
                    } else if (mode === 'sllc') {
                        let data = 'settmp00';
                        data += this.padHexString(this.maxTo255(value, this.COLOR_TEMP_MAX).toString(16));
                        this.sendData({
                            meshAddress,
                            value: [].map.call(data, str => str.charCodeAt(0)),
                        });

                        changed = true;
                    }

                    break;
                }
            }
        }

        if (!changed) {
            NativeModule.changeColorTemp(meshAddress, value);
        }
    }

    static changeColor({
        meshAddress,
        hue = 0,
        saturation = 0,
        value,
        type,
    }) {
        let color = tinycolor.fromRatio({
            h: hue / this.HUE_MAX,
            s: saturation / this.SATURATION_MAX,
            v: value / this.BRIGHTNESS_MAX,
        }).toRgb();
        NativeModule.changeColor(meshAddress, color.a << 24 | color.r << 16 | color.g << 8 | color.b);
    }

    static padHexString(string) {
        if (string.length === 1) {
            return '0' + string;
        } else {
            return string;
        }
    }

    static hexString2ByteArray(string) {
        let array = [];
        [].map.call(string, (value, index, str) => {
            if (index % 2 === 0) {
                array.push(parseInt(value + str[index + 1], 16));
            }
        })

        return array;
    }

    static byteArray2HexString(bytes) {
        return bytes.map(byte => this.padHexString((byte & 0xFF).toString(16))).toString().replace(/,/g, '');
    }

    static changeScene({
        meshAddress,
        scene,
        hue = 0,
        saturation = 0,
        value,
        colorIds = [1, 2, 3, 4, 5],
        type,
        immediate = false,
    }) {}

    static getTypeFromUuid = uuid => parseInt(uuid.slice(4, 8), 16);

    static configNode({
        node,
        cfg,
        isToClaim,
    }) {
        return new Promise((resolve, reject) => {
            if (isToClaim) {
                if (this.isClaiming) {
                    reject('Association already in progress. Parallel association disabled');
                    return;
                } else {
                    this.isClaiming = true;
                }
            }

            let newNode = {
                ...node
            };
            if (node.dhmKey) {
                newNode.dhmKey = this.hexString2ByteArray(node.dhmKey);
            }
            NativeModule.configNode(newNode, isToClaim).then(payload => {
                if (isToClaim && this.passthroughMode) {
                    for (let mode in this.passthroughMode) {
                        if (this.passthroughMode[mode].includes(node.type)) {
                            setTimeout(() => {
                                this.sendData({
                                    meshAddress: node.meshAddress,
                                    value: [0], // firmware from www.oecore.com need any sendData in 60 seconds to confirm claim, other vendor's firmware should not react with this 0 data, so be it.
                                })
                            }, 2000);
                            break;
                        }
                    }
                }

                if (isToClaim) {
                    this.isClaiming = false;
                    resolve({
                        ...payload,
                        dhmKey: this.byteArray2HexString(payload.dhmKey),
                    });
                } else {
                    resolve(payload);
                }
            }, err => {
                if (isToClaim) {
                    this.isClaiming = false;
                }
                reject(err);
            })
        });
    }

    static getTotalOfGroupIndex({
        meshAddress,
    }) {
        return NativeModule.getNumberOfModelGroupIds(meshAddress, 0xFF);
    }

    static setNodeGroupAddr({
        meshAddress,
        groupIndex,
        groupAddress,
    }) {
        return NativeModule.setModelGroupId(meshAddress, 0xFF, groupIndex, 0, groupAddress);
    }

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

    static getNodeInfoWithNewType({
        nodeInfo = '',
        newType = 0xA5A5,
    }) {}

    static getFwVerInNodeInfo({
        nodeInfo = '',
    }) {}

    static getNodeInfoWithNewFwVer({
        nodeInfo = '',
        newFwVer = '',
    }) {}

    static getFirmwareVersion({
        meshAddress = 0xFFFF,
        relayTimes = 7,
        immediate = false,
    }) {}

    // 是否是两个发布版本之间的测试版本
    static isTestFw({
        fwVer,
    }) {}

    static getOtaState({
        meshAddress = 0x0000,
        relayTimes = 7,
        immediate = false,
    }) {}

    static setOtaMode({
        meshAddress = 0x0000,
        relayTimes = 7,     // 转发次数
        otaMode = 'gatt',   // OTA 模式， gatt 为单灯升级， mesh 为单灯升级后由单灯自动通过 mesh 网络发送新固件给其它灯
        type = 0xFB00,      // 设备类型（gatt OTA 模式请忽略此字段）
        immediate = false,
    }) {}

    static stopMeshOta({
        meshAddress = 0xFFFF,
        immediate = false,
    }) {}

    static startOta({
        firmware,
    }) {}

    static isValidFirmware(firmware) {
        return firmware[0] === 0x0E &&
            (firmware[1] & 0xFF) === 0x80 &&
            firmware.length > 6;
    }
}

module.exports = CsrBt;
