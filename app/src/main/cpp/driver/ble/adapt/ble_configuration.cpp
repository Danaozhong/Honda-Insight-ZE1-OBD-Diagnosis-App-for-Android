


/* Own header */
#include "driver/ble/ble_interface.h"

const std::string SERVICE_UUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"; // UART service UUID

/* COMMON */
BLECharacteristicList ble_interface_characteristics =
{

    /* Characteristic for receiving commands */
    {BLE_CHARACTERISTIC_ID_RECEIVE_COMMANDS, 	    "6E400002-B5A3-F393-E0A9-E50E24DCCA2E", BLECharacteristic::PROPERTY_WRITE, NULL},

    /* Characteristic for transmitting commands */
    {BLE_CHARACTERISTIC_ID_SEND_COMMANDS, 			"6E400002-B5A3-F393-E0A9-E50E24DC2A2F", BLECharacteristic::PROPERTY_NOTIFY, new MyCallbacks()},

    /* Characteristic for transmitting OBD Data */
    {BLE_CHARACTERISTIC_ID_SEND_OBD_DATA, 			"6E400002-B5A3-F393-E0A9-E50E24DCCA2F", BLECharacteristic::PROPERTY_NOTIFY, new MyCallbacks()},

    /* Characteristic for publishing OBD Error Codes */
    {BLE_CHARACTERISTIC_ID_SEND_OBD_ERROR_CODES, 	"6E400002-B5A3-F393-E0A9-E50E24DCCA2A", BLECharacteristic::PROPERTY_NOTIFY, new MyCallbacks()},
};


/* The characteristic ID to use for transmitting OBD data */
const unsigned int BLE_CHARACTERISTIC_ID_RECEIVE_COMMANDS = 0u;
const unsigned int BLE_CHARACTERISTIC_ID_SEND_COMMANDS = 1u;
const unsigned int BLE_CHARACTERISTIC_ID_SEND_OBD_DATA = 2u;
const unsigned int BLE_CHARACTERISTIC_ID_SEND_OBD_ERROR_CODES = 3u;

const unsigned char BLE_PACKET_BEGIN = 'a';
const unsigned char BLE_PACKET_MIDDLE = 'b';
const unsigned char BLE_PACKET_END = 'c';

const std::string BLE_DEVICE_SSID = "ZE1OBDIIC";
