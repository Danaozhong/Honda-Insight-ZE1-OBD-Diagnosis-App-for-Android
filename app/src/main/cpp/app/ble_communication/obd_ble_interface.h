//
// Created by Clemens on 04.03.2018.
//

#ifndef ANDROID_OBD_BLE_INTERFACE_H
#define ANDROID_OBD_BLE_INTERFACE_H

#include <string>
#include <vector>
#include <chrono>
#include <mutex>
#include <condition_variable>

#include "midware/threads/cyclic_thread.h"
#include "driver/ble/ble_interface.h"
#include "app/ble_communication/ble_request_queue.hpp"
#include "app/shared/obd_ble_shared.hpp"
#include "app/obd_data_repository.h"

class GenericOBDDataClient
{
public:
    GenericOBDDataClient();

    virtual ~GenericOBDDataClient();

    virtual void startup() = 0;
    virtual void shutdown() = 0;

    //virtual void on_client_connect(const std::string &client_id);
    //virtual void on_client_disconnect(const std::string &client_id);

    virtual int get_protocol_version_major() const;
    virtual int get_protocol_version_minor() const;

    /* Generates a hash out of the data table to compare it with the hash of the other client */
    virtual std::string get_obd_data_hash() const;


   // virtual void set_transmission_request(OBDDataRequestType type) = 0;
    //virtual void clear_transmission_request(OBDDataRequestType type) = 0;

    //std::shared_ptr<Application::DiagnosisReader> get_diagnosis_reader();
    /*
     * Init
     * Version Number
     * Encryption Key
     * Request CyclicTransmission[Object]
     * RequestSingeTransmission[Object]
     */
    /* The current communication state of the cliebt */
    OBDConnectionState connection_state;

    bool is_connected() const;
protected:
    /* Reference to the diagnosis reader used for interfacing the OBD diagnosis tool */
    /* Prevent this class from being copied to prevent slicing */
    GenericOBDDataClient(const GenericOBDDataClient&);
    //operator==(const GenericOBDDataClient&);

    std::atomic<bool> m_bo_is_connected;
};

class BLEOBDDataClient;


class OBDDataCyclicReceiverThread : public Thread
{
public:
    OBDDataCyclicReceiverThread(const std::string &name, BLEOBDDataClient &ble_obd_server, OBDDataRepository &obd_data_repository);
    virtual void run();
private:
    BLEOBDDataClient &ble_obd_data_client;
    OBDDataRepository &data_repository;


};

class OBDClientThread : public Thread
{
public:
    OBDClientThread(const std::string &name, BLEOBDDataClient &ble_obd_server);
    virtual void run();
private:
    BLEOBDDataClient &ble_obd_client;
};



class BLEOBDDataClient : public GenericOBDDataClient, public BLESendRequestQueue
{
public:
    friend OBDClientThread;
    BLEOBDDataClient();

    virtual void startup();
    virtual void shutdown();

    //virtual void set_transmission_request(OBDDataRequestType type);
    //virtual void clear_transmission_request(OBDDataRequestType type);


    void start_communication_threads();
    void join_communication_threads();

    virtual std::vector<unsigned char> get_client_public_encryption_key() const;

    virtual int set_server_public_encryption_key(const std::vector<unsigned char> &encryption_key);

        int initialize_client_connection();

    BLEClientInterface ble_interface;

    /* Automatically sends encrypted or unencrypted, depending on connection state */
    int server_send_secured(int characteristic_id, const std::vector<unsigned char> &buffer);

    int server_send_command(int characteristic_id, const std::vector<unsigned char> command,
                            std::vector<unsigned char> *value = NULL);

    int server_receive_command(int characteristic_id, const std::vector<unsigned char> command,
                               std::vector<unsigned char> *value = NULL,
                               const std::chrono::milliseconds timeout = std::chrono::milliseconds(
                                       2000));

    int server_receive_any_command(int characteristic_id, std::vector<unsigned char> &command,
                                   std::vector<unsigned char> *value = NULL,
                                   const std::chrono::milliseconds timeout = std::chrono::milliseconds(
                                           2000));

    int server_receive_data(int characteristic_id, std::vector<unsigned char> &data,
                            const std::chrono::milliseconds timeout);

    void set_obd_data_transmission_mode(OBDDataTransmissionMode en_new_transmission_mode);
    OBDDataTransmissionMode get_obd_data_transmission_mode() const;

private:
    /* Receive function for OBD commands */
    //std::vector<unsigned char> characteristic_receive_command_received_data;
    //Todo this should be arrays */
    bool characteristic_receive_command_data_received_complete;


    std::vector<unsigned char> received_data_characteristic_id[10];
    bool data_received_complete[10];
    std::mutex characteristic_receive_command_data_receive_mutex[10];
    std::condition_variable characteristic_receive_data_cv[10];

    void receive_from_characteristic(int characteristic_id, const std::vector<unsigned char> &data);


    bool server_is_running;



    /* Collection of all BLE communication threads that are currently active */
    //ThreadRepository transmission_thread_repository;
    //bool active_requests[DATA_REQUEST_NUM_OF_ITEMS];

    std::shared_ptr<OBDClientThread> thread_ble_obd_client_thread;
    //std::shared_ptr<CyclicThread> thread_obd_data_transmission;
    //std::shared_ptr<CyclicThread> thread_obd_error_codes_transmission;

    OBDDataTransmissionMode m_en_obd_data_transmission_mode;

private: // Todo remove



};

#endif //ANDROID_OBD_BLE_INTERFACE_H
