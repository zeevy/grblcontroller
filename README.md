# Grbl Controller
### Compact android mobile application for GRBL powered CNC machine.

![Axis Control](https://raw.githubusercontent.com/zeevy/grblcontroller/master/doc/screenshots/JoggingTab.png "Axis Controll Panel") ![File Streaming](https://raw.githubusercontent.com/zeevy/grblcontroller/master/doc/screenshots/FileSenderTab.png "File Streaming Panel")

#### Features:
- Supports both Bluetooth and USB connections
- GRBL 1.1 real time feed, spindle and rapid overrides support.
- Simple and powerful jogging control.
- Uses character counting streaming protocol.
- Real time machine status reporting (Position, feed, spindle speed, buffer state. Buffer status report needs to enabled using the setting $10=2).
- Supports Sending G-Code files directly from mobile phone. (Supported extensions are .gcode, .nc, .ngc and .tap. G-Code files can be placed anywhere in the phone or external storage).
- Supports short text commands.
- Supports Probing (G38.3) with auto adjust Z-Axis.
- Manual tool change with G43.1
- 4 Highly Configurable Custom Buttons which supports multi line commands (Supports both short click and long click).
- Application can work in background mode, by utilizing the less resources, there by consuming less power.

#### Notes:
- For android versions Marshmallow or above, use your OS permission manager and grant "Read External Storage" permission in order to get file streaming working.
- G-Code files can be placed anywhere in the phone or external storage, but they must end with one of the following extensions .gcode or .nc or .tap or .ngc.
- If you are connecting Bluetooth module first time to your machine, then make sure you have changed the baud rate of the BT module to 115200. (Default baud rate of the GRBL 1.1v firmware is 115200 as 8-bits, no parity, and 1-stop bit).
- HC-05 Bluetooth module setup http://www.buildlog.net/blog/2017/10/using-the-hc-05-bluetooth-module/
- HC-06 Bluetooth module setup https://github.com/zeevy/grblcontroller/wiki/Bluetooth-Setup-HC-06

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="90"/>](https://play.google.com/store/apps/details?id=in.co.gorest.grblcontroller "Download from play store")

### Limitations:
- No trimming of decimal places
- Does not remove unsupported Gcodes
- No expansion of Canned Drill cycles or M06 Tool Change

#### Known Bugs:
- Some times jog stop button will not work effectively, need to press the jog stop button two times.

#### Special thanks to

- Will Winder https://github.com/winder/Universal-G-Code-Sender
- Joan Zapata https://github.com/JoanZapata/android-iconify
- Markus Junginger https://github.com/greenrobot/EventBus
- Felipe Herranz https://github.com/felHR85/UsbSerial
- nbsp-team https://github.com/nbsp-team/MaterialFilePicker
- Chuang Guangquan https://github.com/warkiz/IndicatorSeekBar
