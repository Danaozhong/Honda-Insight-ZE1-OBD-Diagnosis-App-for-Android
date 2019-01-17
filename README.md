HONDA INSIGHT ZE1 OBDII SMARTPHONE EXTENSION
============================================

This is a Android app which receives OBDII vehicle data, transmitted from a ESP32 using BLE (Bluetooth Low Energy). Being a hybrid vehicle, having the possibility to display data from the ECU or the Battery Management System in a live view is important.

So far current products provide outdated dot matrix displays only. This app is intended to change that by providing an intuitive visual interface using color-coded bar diagrams.

How does it work?
-----------------

In order to make it run, you neeed an ESP32 controller, and flash the software from https://github.com/Danaozhong/sw.tool.esp32.insight_ze1_obd2_ble.

Build this repository using Android Studio and use the app.

Info
----

Development is so far still in its very early stages. The missing part so far is the interface of the ESP32 to the OBDIIC&C device, which is still under development. 

Software Details
----------------
The main components are written in C++, while the HMI uses Java. Data between the HMI and the core part is exchanged using SWIG.
