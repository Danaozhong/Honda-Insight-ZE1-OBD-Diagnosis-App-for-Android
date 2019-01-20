//
// Created by Clemens on 04.03.2018.
//


#include <midware/data_pool/data_pool.hpp>
#include "obd_ble_interface.h"
#include "driver/ble/ble_interface.h"
#include "app/shared/obd_ble_shared.hpp"
#include "midware/threads/cyclic_thread.h"
#include "midware/trace/trace.h"
#include "app/diagnosis_reader_application.h"

GenericOBDDataClient::GenericOBDDataClient()
{}

GenericOBDDataClient::~GenericOBDDataClient()
{}

bool GenericOBDDataClient::is_connected() const
{
    return this->m_bo_is_connected;
}

int GenericOBDDataClient::get_protocol_version_major() const
{
    return 1;
}

int GenericOBDDataClient::get_protocol_version_minor() const
{
    return 0;
}

/* Generates a hash out of the data table to compare it with the hash of the other client */
std::string GenericOBDDataClient::get_obd_data_hash() const
{
    return "AABBCC";
}



OBDDataCyclicReceiverThread::OBDDataCyclicReceiverThread(const std::string &name, BLEOBDDataClient &ble_obd_client, OBDDataRepository &obd_data_repository)
    : Thread(name), ble_obd_data_client(ble_obd_client), data_repository(obd_data_repository)
{
}

void OBDDataCyclicReceiverThread::run()
{
    //DEBUG_PRINTF("Worker Thread for OBD Data started!");
    while(true)
    {
        std::vector<unsigned char> obd_data = {};
        DEBUG_PRINTF("OBD data thread: try to receive");
        if (ble_obd_data_client.server_receive_data(BLE_CHARACTERISTIC_ID_SEND_OBD_DATA,
                                                       obd_data, std::chrono::seconds(1)) == 0)
        {
            DEBUG_PRINTF("OBD data thread: rec successful");
            if (obd_data.size() <= 6)
            {
                DEBUG_PRINTF("Received OBD data packet is empty, actual size is: " + helper::to_string(obd_data.size()));
                continue;
            }

            /* Decode OBD data */
            uint16_t u16_packet_length = *(reinterpret_cast<uint16_t*>(&obd_data[4]));


            /* Start parsing at the sixth byte */
            auto read_itr = obd_data.begin() + 6;
            while(read_itr != obd_data.end())
            {
                char identifier = (char)(*read_itr);
                auto base_obd_data = data_repository.get_obd_data_by_identifier(identifier);

                if (nullptr == base_obd_data)
                {
                    /* Error occured, could not find the OBD data with this identifier */
                    DEBUG_PRINTF("Error occured, identifier " + helper::to_string(static_cast<int>(identifier)) + " not found in OBD Data!");
                    break;
                }
                const size_t data_size = base_obd_data->get_value_size();

                if (read_itr - obd_data.begin() + 1 + data_size <= obd_data.size())
                {
                    // read the data
                    read_itr++;

                    if (base_obd_data->get_value().type == OBD_VALUE_NUMERIC)
                    {
                        float value_f = *(reinterpret_cast<float *>(&(*read_itr)));
                        auto numeric_obd_data = std::dynamic_pointer_cast<NumericOBDData>(
                                base_obd_data);
                        numeric_obd_data->set_value(value_f);
                    }

                    // Goto next data field
                    read_itr += data_size;
                }
                else
                {
                    break;
                }

            }

            /* Updata data pool */
            data_repository.update_shared_data();
            /* Send signal to HMI */
            //std::string data_to_send = "Reply from C++";
            //std::vector<unsigned char> data_bytes(data_to_send.begin(), data_to_send.end());
            //send_ble_data("6E400002-B5A3-F393-E0A9-E50E24DCCA2E", data_bytes);

            //DEBUG_PRINTF("RECEIVED OBD Data via different interface, length is: " + std::string(obd_data.begin(), obd_data.end()));
        }
        else
        {
            DEBUG_PRINTF("Did not receive any OBD Data!");
        }
    }
}

OBDClientThread::OBDClientThread(const std::string &name, BLEOBDDataClient &ble_obd_client)
        : Thread(name), ble_obd_client(ble_obd_client)
{
}


void OBDClientThread::run()
{
    ble_obd_client.connection_state = CONNECTION_STATE_DISCONNECTED;


    ble_obd_client.startup();

    /* Wait a while until BLE is established */
    std::this_thread::sleep_for(std::chrono::milliseconds(2000));

    /* Try to connect to an interface */
    while(true)
    {
        if (0 == ble_obd_client.initialize_client_connection())
        {
            /* Connection established */
            this->ble_obd_client.m_bo_is_connected = true;

            std::vector<unsigned char> elements_of_interest;

            for (int i = 0; i != DataPoolAccessor::get_main_view_elements_of_interest_count(); ++i)
            {
                elements_of_interest.push_back(DataPoolAccessor::get_main_view_elements_of_interest_array_element(i).u32_element);
            }

            ble_obd_client.send_command(BLECommand::BLE_COMMAND_SET_INTERESTING_OBD_DATA, &elements_of_interest);
            /* create worker threads */
            OBDDataCyclicReceiverThread obd_data_cyclic_receiver_thread("OBDDataCyclicReceiverThread", this->ble_obd_client, App::get_app().get_data_repository());
            obd_data_cyclic_receiver_thread.start();

            auto last_rx_timestamp = std::chrono::system_clock::now();
            auto last_tx_timestamp = std::chrono::system_clock::now();

            while(true == this->ble_obd_client.m_bo_is_connected)
            {
                /* Wait for commands / or keep alives */
                BLETransmitBuffer received_command;
                BLETransmitBuffer received_value;
                if(0 == ble_obd_client.server_receive_any_command(BLE_CHARACTERISTIC_ID_SEND_COMMANDS,
                                                                  received_command, &received_value, std::chrono::milliseconds(100)))
                {
                    DEBUG_PRINTF("Received command signal!");

                    last_rx_timestamp = std::chrono::system_clock::now();
                    /* TODO Check packet content */
                }

                {
                    std::lock_guard<std::mutex> lock_guard(this->ble_obd_client.m_ble_requeust_queue_mutex);
                    while (this->ble_obd_client.m_ble_request_queue.size() > 0)
                    {
                        auto current_request = this->ble_obd_client.m_ble_request_queue.front();
                        ble_obd_client.m_ble_request_queue.pop_front();

                        int ret_val = -1;
                        if (0 != ble_obd_client.server_send_command(BLE_CHARACTERISTIC_ID_RECEIVE_COMMANDS,
                                                                    current_request->send_command, &current_request->send_value))
                        {
                            /* Sending the signal failed, continue with next queue item */
                            continue;
                        }
                        current_request->SIG_BLE_REQUEST_SENT(0);

                        /* Check if a reply is requested */
                        if (current_request->request_type == BLE_REQUEST_SEND_AND_RECEIVE)
                        {
                            if(0 == ble_obd_client.server_receive_any_command(BLE_CHARACTERISTIC_ID_SEND_COMMANDS,
                                                                              current_request->received_command, &current_request->received_value, current_request->timeout))
                            {
                                /* Successfully received an answer via BLE, signalize to client */
                                current_request->SIG_BLE_REQUEST_REPLY_RECEIVED(0);

                                /* Update timestamp that we have received something */
                                last_rx_timestamp = std::chrono::system_clock::now();
                            }
                        }
                    }
                }

                /* Ensure we did not run into timeout */
                if (std::chrono::system_clock::now() - last_rx_timestamp > std::chrono::seconds(10))
                {
                    /*Nothing received within 10 seconds, abort! */
                    DEBUG_PRINTF("Nothing received within 10 seconds!");
                    //this->ble_obd_client.m_bo_is_connected = false;
                }

                /* Check if it necessary to send heartbeat */
                if (std::chrono::system_clock::now() - last_tx_timestamp > std::chrono::seconds(5))
                {
                    /* send a heartbeat and reset the counter */
                    std::vector<unsigned char> heartbeat_data = { 'V', 'B', 'A' };
                    if (0 != ble_obd_client.server_send_command(
                            BLE_CHARACTERISTIC_ID_RECEIVE_COMMANDS,
                            BLECommand::BLE_COMMAND_CLIENT_KEEP_ALIVE, &heartbeat_data))
                    {
                        /* Sending heartbeat failed, close connection. */
                        DEBUG_PRINTF("Error: Sending heartbeat failed, exiting!");
                        //this->ble_obd_client.m_bo_is_connected = false;
                    }
                    last_tx_timestamp = std::chrono::system_clock::now();
                }
            }
            obd_data_cyclic_receiver_thread.join();
            // TODO Terminate worker threads....
        }

    }
    /* Never terminates */
}



BLEOBDDataClient::BLEOBDDataClient()
    : GenericOBDDataClient(), server_is_running(false)
{
    thread_ble_obd_client_thread = std::shared_ptr<OBDClientThread>(new OBDClientThread("OBDBLEClient", *this));
    thread_ble_obd_client_thread->start();
}

void BLEOBDDataClient::startup()
{
    DEBUG_PRINTF("Startup of BLE OBD Server called!");
    if (this->server_is_running == true)
    {
        return;
    }
    this->server_is_running = true;

    /* Create the cyclic threads, but do not launch them yet */
    //assert(this->thread_obd_data_transmission == NULL);
    //assert(this->thread_obd_error_codes_transmission == NULL);

    /* Currently only OBD data transmission thread is supported */
    //this->thread_obd_data_transmission = std::shared_ptr<OBDDataBLETransmissionThread>(new OBDDataBLETransmissionThread(this));

    /* Initialize the BLE hardware */
    this->ble_interface.init_interface();

    /* Register for the Rx callback of the OBD BLE transmission, use a lambda to encapsulate class interface and connection ID */
    auto ble_characteristic_receive_commands_rx = std::bind(&BLEOBDDataClient::receive_from_characteristic, this, BLE_CHARACTERISTIC_ID_SEND_COMMANDS, std::placeholders::_1);
    auto ble_characteristic_receive_obd_data_rx = std::bind(&BLEOBDDataClient::receive_from_characteristic, this, BLE_CHARACTERISTIC_ID_SEND_OBD_DATA, std::placeholders::_1);

    this->ble_interface.get_characteristic(BLE_CHARACTERISTIC_ID_SEND_COMMANDS)->get_received_data_signal()->connect(ble_characteristic_receive_commands_rx);
    this->ble_interface.get_characteristic(BLE_CHARACTERISTIC_ID_SEND_OBD_DATA)->get_received_data_signal()->connect(ble_characteristic_receive_obd_data_rx);

    /* Start the bluetooth manager thread */
    //this->thread_ble_obd_server_thread->start();
}


void BLEOBDDataClient::shutdown()
{
    this->server_is_running = false;
}

std::vector<unsigned char> BLEOBDDataClient::get_client_public_encryption_key() const
{
    return std::vector<unsigned char> { 'A', 'B', 'D'};
}

int BLEOBDDataClient::set_server_public_encryption_key(const std::vector<unsigned char> &encryption_key)
{
    return 0;
}

int BLEOBDDataClient::initialize_client_connection()
{
    this->connection_state = CONNECTION_STATE_INIT;

    DEBUG_PRINTF("Starting client connection...");
    /* Receive encryption key request */

    int iAttempts = 0;
    bool boSuccess = false;
    while (iAttempts < 3)
    {
        iAttempts++;
        if (this->server_receive_command(BLE_CHARACTERISTIC_ID_SEND_COMMANDS,
                                         BLECommand::BLE_COMMAND_REQUEST_ENCRYPTION_KEY, NULL) == 0)
        {
            boSuccess = true;
            break;
        }
        else
        {
            DEBUG_PRINTF(helper::to_string(iAttempts) + " request for Encryption key timeout.");
        }
    }

    if (boSuccess == false)
    {
        // No request for encryption key received
        DEBUG_PRINTF("No request for encryption key received, aborting!");
        this->connection_state = CONNECTION_STATE_DISCONNECTED;
        return -1;
    }

    DEBUG_PRINTF("Send encryption key...");
    /* Send the server our encryption key */
    std::vector<unsigned char> encryption_key = get_client_public_encryption_key();
    if (0 != this->server_send_command(BLE_CHARACTERISTIC_ID_RECEIVE_COMMANDS,
                                       BLECommand::BLE_COMMAND_PUBLISH_ENCRYPTION_KEY,
                                       &encryption_key))
    {
        this->connection_state = CONNECTION_STATE_DISCONNECTED;
        return -1;
    }

    /* Wait for the encryption key of the server */
    DEBUG_PRINTF("Client connected, initializing encryption...");
    this->connection_state = CONNECTION_STATE_INIT_ENCRYPTION;
    std::vector<unsigned char> server_public_encryption_key;
    if (this->server_receive_command(BLE_CHARACTERISTIC_ID_SEND_COMMANDS,
                                     BLECommand::BLE_COMMAND_PUBLISH_ENCRYPTION_KEY,
                                     &server_public_encryption_key) != 0)
    {
        /* Server encryption key not received */
        this->connection_state = CONNECTION_STATE_DISCONNECTED;
        return -1;
    }

    /* Process server encryption key */
    this->set_server_public_encryption_key(server_public_encryption_key);

    /* Encryption establish successful, send handshake */
    if (0 != this->server_send_command(BLE_CHARACTERISTIC_ID_RECEIVE_COMMANDS,
                                       BLECommand::BLE_COMMAND_ENCRYPTION_HANDSHAKE))
    {
        this->connection_state = CONNECTION_STATE_DISCONNECTED;
        return -1;
    }

    this->connection_state = CONNECTION_STATE_DATA_PROTOCOL_SYNC;

    /* Wait for the OBD Data hash request from the server */

    if (0 != this->server_receive_command(BLE_CHARACTERISTIC_ID_SEND_COMMANDS,
                                          BLECommand::BLE_COMMAND_REQUEST_OBD_DATA_HASH))
    {
        this->connection_state = CONNECTION_STATE_DISCONNECTED;
        return -1;
    }

    std::vector<unsigned char> client_obd_data_hash = { 0x01, 0x02, 0x04 };

    /* Wait for secure handshake */
    if (this->server_send_command(BLE_CHARACTERISTIC_ID_RECEIVE_COMMANDS,
                                  BLECommand::BLE_COMMAND_OBD_DATA_HASH, &client_obd_data_hash) != 0)
    {
        DEBUG_PRINTF("Sending the client obd data hash failed!");
        this->connection_state = CONNECTION_STATE_DISCONNECTED;
        return -1;
    }

    /* Successfully managed to read the OBD Data hash */
    /* TODO compare the hashes */

    DEBUG_PRINTF("Connection established!");
    this->connection_state = CONNECTION_STATE_CONNECTED;
    return 0;
}


/* Automatically sends encrypted or unencrypted, depending on connection state */
int BLEOBDDataClient::server_send_secured(int characteristic_id, const std::vector<unsigned char> &buffer)
{
    if (this->connection_state >= CONNECTION_STATE_INIT_ENCRYPTION)
    {
        /* TODO Encrypt data */
    }
    else
    {
        /* Nothing to do, send data non encrypted */
    }
    this->ble_interface.send_interface(characteristic_id, buffer);
    return 0;
}

int BLEOBDDataClient::server_send_command(int characteristic_id, const std::vector<unsigned char> command, std::vector<unsigned char> *value)
{
    std::vector<unsigned char> data = command;
    if (value != nullptr)
    {
        data.insert(data.end(), (*value).begin(), (*value).end());
    }
    return this->server_send_secured(characteristic_id, data);
}


int BLEOBDDataClient::server_receive_command(int characteristic_id, const std::vector<unsigned char> command, std::vector<unsigned char> *value, const std::chrono::milliseconds timeout)
{
    std::vector<unsigned char> actually_received_command;
    std::vector<unsigned char> actually_received_value;

    if(0 == server_receive_any_command(characteristic_id, actually_received_command,
                                       &actually_received_value, timeout))
    {
        DEBUG_PRINTF("Expected command:  " + helper::to_string((char)command[0]) +  ", actual command: " + helper::to_string((char)actually_received_command[0]));
        if (actually_received_command[0] == command[0])
        {
            if (nullptr != value)
            {
                *value = actually_received_value;
            }
            return 0;
        }
    }

    /* Nothing received (timeout), or incorrect command received */
    return -1;
}

int BLEOBDDataClient::server_receive_any_command(int characteristic_id, std::vector<unsigned char> &command, std::vector<unsigned char> *value, const std::chrono::milliseconds timeout)
{
    std::vector<unsigned char> data;
    if(0 == server_receive_data(characteristic_id, data, timeout) && data.size() > 0)
    {
        command = std::vector<unsigned char> { data[0] };
        if (nullptr != value && data.size() > 1)
        {
            *value = std::vector<unsigned char>(data.begin() + 1, data.end());
        }
        return 0;
    }
    return -1;
}

int BLEOBDDataClient::server_receive_data(int characteristic_id, std::vector<unsigned char> &data, const std::chrono::milliseconds timeout)
{
    {
#if 0
        std::unique_lock<std::mutex> lck(
                characteristic_receive_command_data_receive_mutex[characteristic_id]);

        /* Wait to receive data */

        if (characteristic_receive_data_cv[characteristic_id].wait_for(lck, timeout,
                                                                       [this, characteristic_id]() -> bool {
                                                                           return data_received_complete[characteristic_id] ==
                                                                                  true;
                                                                       }))

#endif

        const auto ts_begin = std::chrono::system_clock::now();
        while(data_received_complete[characteristic_id] == false)
        {
            TaskHelper::sleep_for(std::chrono::milliseconds(50));
            auto ts_now = std::chrono::system_clock::now();
            if (ts_now - ts_begin > timeout)
            {
                break;
            }
        }

        if(true == data_received_complete[characteristic_id])
        {
            data_received_complete[characteristic_id] = false;
            data = received_data_characteristic_id[characteristic_id];

            // after we wait, we own the lock.
            //lck.unlock();
            return 0;
        } else
        {
            /* Data not received (timeout). */
            return -1;
        }
    }
}

void BLEOBDDataClient::set_obd_data_transmission_mode(OBDDataTransmissionMode en_new_transmission_mode)
{
    if (this->m_en_obd_data_transmission_mode == en_new_transmission_mode)
    {
        return;
    }

    this->m_en_obd_data_transmission_mode = en_new_transmission_mode;

    if (this->m_en_obd_data_transmission_mode == TRANSMISSION_MODE_ALL_DATA_SLOW)
    {
        BLETransmitBuffer value = { static_cast<unsigned char>(this->m_en_obd_data_transmission_mode) };
        this->send_command(BLECommand::BLE_COMMAND_CHANGE_OBD_TRANSMISSION_MODE, &value);
    }
}

OBDDataTransmissionMode BLEOBDDataClient::get_obd_data_transmission_mode() const
{
    return this->m_en_obd_data_transmission_mode;
}

void BLEOBDDataClient::receive_from_characteristic(int characteristic_id, const std::vector<unsigned char> &data)
{
    if (data.size() == 0)
    {
        /* Invalid data, quit */
        return;
    }
    if (data_received_complete[characteristic_id] == true)
    {
        DEBUG_PRINTF("Still got data");
        return;
    }

    if (characteristic_id == BLE_CHARACTERISTIC_ID_SEND_OBD_DATA)
    {
        DEBUG_PRINTF("Received something via the OBD interface, will acquire mutex no!");
    }
    {
        /* Signalise waiting thread that data has been received */
        std::lock_guard<std::mutex> lck(
                characteristic_receive_command_data_receive_mutex[characteristic_id]);

        // TODO try to use std::move here
        this->received_data_characteristic_id[characteristic_id] = data;
        data_received_complete[characteristic_id] = true;
    }
    characteristic_receive_data_cv[characteristic_id].notify_all();
}