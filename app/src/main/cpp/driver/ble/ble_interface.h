//
// Created by Clemens on 04.03.2018.
//

#ifndef ANDROID_BLE_INTERFACE_H
#define ANDROID_BLE_INTERFACE_H

#include <jni.h>

#include "midware/events/event_handler.h"

/* Own header files */
#include "driver/ble/adapt/ble_configuration.hpp"
#include "driver/ble/platform_specific/ble_platform_specific.h"

typedef std::vector<unsigned char> BLETransmitBuffer;

//int send_ble_data(std::string uuid, const std::vector<unsigned char> &data);
int send_ble_data(std::string uuid, const unsigned char *data, size_t data_buffer_size);

int receive_ble_data(std::string uuid_str, const std::vector<unsigned char> &data);

class MyCallbacks
{
public:
    void receive_callback(const std::vector<unsigned char> &data);

    boost::signals2::signal<std::vector<unsigned char>> SIG_RECEIVED_DATA;
private:
    std::vector<unsigned char> current_message;
};


/* COMMON Interface class to be implemented platform specific */
class BLEInterfaceCharacteristicBase
{
public:
    virtual int send(const std::vector<unsigned char> &buffer) = 0;

    virtual int get_index() const = 0;

    virtual const char* get_uuid() const = 0;

    /* Signal to be called if data has been received */
    virtual boost::signals2::signal<std::vector<unsigned char>>* get_received_data_signal() const = 0;
};


/* ADAPT */
class BLEInterfaceCharacteristic : public BLEInterfaceCharacteristicBase
{
public:
    BLEInterfaceCharacteristic(unsigned int index, const char uuid[40], int property, MyCallbacks* callback = NULL);

    virtual int send(const std::vector<unsigned char> &buffer);

    virtual int get_index() const;

    virtual const char* get_uuid() const;

    virtual boost::signals2::signal<std::vector<unsigned char>>* get_received_data_signal() const;

private:
    unsigned int index;
    char uuid[BLE_UUID_LENGTH];
    int property;
    MyCallbacks* rx_callback;
    //BLECharacteristic *p_characteristic;

    friend int receive_ble_data(std::string uuid_str, const std::vector<unsigned char> &data);
};


    /* TODO Derive from client / server common base class */
class BLEClientInterface
{
public:
    void init_interface();
    void send_interface(int index, const std::vector<unsigned char> &data);

    bool is_connected() const;

    void disconnect() const;

    BLEInterfaceCharacteristic* get_characteristic(int id);
private:
    //MyServerCallbacks *server_callback;

    std::string interface_public_string;
};

#endif //ANDROID_BLE_INTERFACE_H

