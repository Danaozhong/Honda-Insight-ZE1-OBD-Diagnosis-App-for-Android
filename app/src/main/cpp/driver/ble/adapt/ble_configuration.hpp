
#ifndef _BLE_CONFIGURATION_HPP_
#define _BLE_CONFIGURATION_HPP_

#include <string>
#include <vector>


extern const std::string SERVICE_UUID;// UART service UUID

class BLEInterfaceCharacteristic;
typedef std::vector<BLEInterfaceCharacteristic> BLECharacteristicList;
extern BLECharacteristicList ble_interface_characteristics;


/** BLE characteristic names */
extern const unsigned int BLE_CHARACTERISTIC_ID_RECEIVE_COMMANDS;
extern const unsigned int BLE_CHARACTERISTIC_ID_SEND_COMMANDS;
extern const unsigned int BLE_CHARACTERISTIC_ID_SEND_OBD_DATA;
extern const unsigned int BLE_CHARACTERISTIC_ID_SEND_OBD_ERROR_CODES;

#define BLE_UUID_LENGTH (40u)
#define BLE_MAX_PACKET_SIZE (20u)

extern const unsigned char BLE_PACKET_BEGIN;
extern const unsigned char BLE_PACKET_MIDDLE;
extern const unsigned char BLE_PACKET_END;

extern const std::string BLE_DEVICE_SSID;


#endif

