# Grbl Bluetooth Controller for Android Mobile
#### Use bluetooth enabled mobile phone to stream G-code and control your GRBL powered CNC machine.

![Axis Control](https://raw.githubusercontent.com/zeevy/grblcontroller/master/doc/screenshots/resized/Screenshot_20171001-090425.png "Axis Controll Panel") ![File Streaming](https://raw.githubusercontent.com/zeevy/grblcontroller/master/doc/screenshots/resized/Screenshot_20171001-090518.png "File Streaming Panel").

#### Features:
- Supports GRBL 1.1 real time feed, spindle and rapid overrides.
- Simple and powerful jogging control with corner jogging.
- Uses buffered streaming.
- Real time machine status reporting (Position, feed, spindle speed, buffer state. Buffer status report needs to enabled using the setting $10=2).
- Supports Sending G-Code files directly from mobile phone. (Supported extensions are .gcode, .nc and .tap. G-Code files can be placed anywhere in the phone or external storage).
- Supports short text commands (You can send G-Code or GRBL commands from the mobile by typing).
- Supports Probing (G38.3) and auto adjusts Z-Axis.
- Manual tool change with G43.1
- 4 Highly Configurable Custom Buttons which supports multi line commands (Supports both short click and long click).
- Application can work in background mode, by utilizing the less resources there by consuming less power.

#### Notes:
- For android versions Marshmallow or above, use your OS permission manager and grant "Read External Storage" permission in order to get file streaming working.
- G-Code files can be placed anywhere in the phone or external storage, but they must end with one of the following extensions .gcode or .nc or .tap.
- If you are connecting Bluetooth module first time to your machine, then make sure you have changed the baud rate of the BT module to 115200. (Default baud rate of the GRBL 1.1v firmware is 115200 as 8-N-1 (8-bits, no parity, and 1-stop bit)).
- For more information on changing baud rate, pass code of HC-05 Bluetooth module you may visit this link http://www.buildlog.net/blog/2017/10/using-the-hc-05-bluetooth-module/.

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="90"/>](https://play.google.com/store/apps/details?id=in.co.gorest.grblcontroller "Download from play store").

#### Special thanks to

1. winder https://github.com/winder/Universal-G-Code-Sender (for most of the core logic and code)
2. michael-rapp  https://github.com/michael-rapp/AndroidMaterialPreferences
3. woxingxiao https://github.com/woxingxiao/BubbleSeekBar
4. greenrobot https://github.com/greenrobot/EventBus
5. DroidNinja https://github.com/DroidNinja/Android-FilePicker
6. JoanZapata https://github.com/JoanZapata/android-iconify
