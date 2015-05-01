README
--------------------------------------------------

This project contains three examples of code to scan for Wimoto Climates and parse their ad data for temperature, 
and light measurements.

*****MainActivity*****
Pressing scan on the UI will cause the Android device to scan for nearby Climtes. The parsed data will appear in
the LogCat.

*****BeaconLollipopActivity & BeaconKitKatActivity*****
After the program begins, it constantly scans for Climates and parses their ad data for temperature and light
values. The temperature and light values will show up on the UI as well as in Logcat. Make sure to run the 
appropriate version of this program for your device as Lollipop brought along new API functions for BLE.
