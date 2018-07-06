const {
    NativeModules,
    DeviceEventEmitter,
} = require('react-native');
const NativeModule = NativeModules.CsrBt;

var tinycolor = require("tinycolor2");

class CsrBt {
    static MESH_ADDRESS_MIN = 0x8001;
    static MESH_ADDRESS_MAX = 0x8FFF;
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

    static changePower({
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
                            color: {
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
        value,
        type,
    }) {
        NativeModule.changeColor(meshAddress, value);
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
        return bytes.map(byte => this.padHexString(byte.toString(16))).toString().replace(/,/g, '');
    }

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
}

module.exports = CsrBt;
