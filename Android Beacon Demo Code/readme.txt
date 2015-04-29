{\rtf1\ansi\ansicpg1252\cocoartf1265\cocoasubrtf210
{\fonttbl\f0\fswiss\fcharset0 Helvetica;}
{\colortbl;\red255\green255\blue255;}
\margl1440\margr1440\vieww10800\viewh8400\viewkind0
\pard\tx566\tx1133\tx1700\tx2267\tx2834\tx3401\tx3968\tx4535\tx5102\tx5669\tx6236\tx6803\pardirnatural

\f0\fs24 \cf0 README\
\
This project contains three examples of code to scan for Wimoto Climates and parse their ad data for temperature, and light measurements. \
\
*****MainActivity*****\
Pressing scan on the UI will cause the device to scan. The parsed data will appear in the LogCat.\
\
\
*****BeaconLollipopActivity & BeaconKitKatActivity*****\
After the program begins, it constantly scans for Climates and parses their ad data for temperature and light values. The temperature and light values will show up on the UI as well as in LogCat. Make sure to run the appropriate version of this program for your device as Lollipop brought along new API functions for BLE. \
}