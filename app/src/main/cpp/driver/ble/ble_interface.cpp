/* System header */
#include <vector>
#include <jni.h>
#include <algorithm>
#include <string>
#include <cstring>

/* Foreign header */


/* Local header */
#include "obd_types/obd_error_code.h"
#include "midware/trace/trace.h"
#include "ble_interface.h"



static JNIEnv* java_env_thread_ble;

static jclass java_jobject_bluetooth_service = nullptr;
static jobject java_jobject_bluetooth_service_instance = nullptr;




void MyCallbacks::receive_callback(const std::vector<unsigned char> &rx_buffer)
{
    //const std::vector<unsigned char> rx_buffer(rxValue.begin(), rxValue.end());

    if (rx_buffer.size() < 2)
    {
        /* Invalid data received! */
        this->current_message.clear();
        TRACE_PRINTF("Invalid message received (less than two bytes)");
        return;
    }

    if (rx_buffer[0] != BLE_PACKET_BEGIN && rx_buffer[0] != BLE_PACKET_MIDDLE && rx_buffer[0] != BLE_PACKET_END)
    {
        /* Invalid data received! */
        this->current_message.clear();
        TRACE_PRINTF("Invalid message received (invalid start byte)");
        return;
    }

    /* If a new message has been received, clear the buffer */
    if (rx_buffer[0] == BLE_PACKET_BEGIN)
    {
        this->current_message.clear();
    }

    /* Reconstruct the mssage */
    this->current_message.insert(this->current_message.end(), rx_buffer.begin() + 1, rx_buffer.end());

    if (rx_buffer[0] == BLE_PACKET_END)
    {
        /* Signalize application */
        this->SIG_RECEIVED_DATA(this->current_message);

        /* Clear the message buffer for subsequent receives */
        this->current_message.clear();
    }
}


BLEInterfaceCharacteristic::BLEInterfaceCharacteristic(unsigned int index, const char uuid[40], int property, MyCallbacks* callback)
        : index(index), rx_callback(callback), property(property), uuid()
{
    strncpy(this->uuid, uuid, 40 - 1);
}

int BLEInterfaceCharacteristic::send(const std::vector<unsigned char> &buffer)
{
    /* Start the first packet with the starting byte */
    unsigned char send_buffer[BLE_MAX_PACKET_SIZE] = { BLE_PACKET_BEGIN, 0 };

    auto send_itr = buffer.begin();
    while(send_itr < buffer.end())
    {
        size_t remaining_buffer_size = buffer.end() - send_itr;
        size_t current_packet_size = std::min(static_cast<size_t>(BLE_MAX_PACKET_SIZE - 1), remaining_buffer_size);

        if (remaining_buffer_size < BLE_MAX_PACKET_SIZE)
        {
            /* Last 20 bytes reached, send the packet end information */
            send_buffer[0] = BLE_PACKET_END;
        }
        memcpy(send_buffer + 1, &(*send_itr), current_packet_size);
        send_itr += current_packet_size;
        send_ble_data(this->get_uuid(), send_buffer, current_packet_size + 1);

        /* Prepare the next packet */
        send_buffer[0] = BLE_PACKET_MIDDLE;
    }

    return 0;
}

int BLEInterfaceCharacteristic::get_index() const
{
    return this->index;
}

const char* BLEInterfaceCharacteristic::get_uuid() const
{
    return this->uuid;
}

boost::signals2::signal<std::vector<unsigned char>>* BLEInterfaceCharacteristic::get_received_data_signal() const
{
    if (nullptr == this->rx_callback)
    {
        return nullptr;
    }
    return &(this->rx_callback->SIG_RECEIVED_DATA);
}

void BLEClientInterface::init_interface()
{
    java_vm->AttachCurrentThread(&java_env_thread_ble, NULL);
    // TODO Stub
}

void BLEClientInterface::send_interface(int index, const std::vector<unsigned char> &data)
{
    if (false == is_connected())
    {
        return;
    }
    auto characteristic = this->get_characteristic(index);
    characteristic->send(data);
}

bool BLEClientInterface::is_connected() const
{
    // TODO stub
    return true;
}

void BLEClientInterface::disconnect() const
{

}

BLEInterfaceCharacteristic* BLEClientInterface::get_characteristic(int id)
{
    for(auto itr = ble_interface_characteristics.begin(); itr != ble_interface_characteristics.end(); ++itr)
    {
        if (itr->get_index() == id)
        {
            //TRACE_PRINTF("Index found, " + helper::to_string(id));
            return &(*itr);
        }
    }
    return nullptr;
}


int send_ble_data(std::string uuid, const unsigned char *data, size_t data_buffer_size)
{
    std::transform(uuid.begin(), uuid.end(), uuid.begin(), ::tolower);



    /* The actual BLE interface is written in Java, forward... */
    /* public int sendBLEBytes(String uuid, final byte data[]) */
    jmethodID method_id = java_env_thread_ble->GetMethodID(java_jobject_bluetooth_service, "sendBLEBytes", "(Ljava/lang/String;[B)I");
    if (method_id == 0)
    {
        return -1;
    }

    jstring jstrUUIDBuf = java_env_thread_ble->NewStringUTF(uuid.c_str());
    //jstring jstrDataBuf = java_env_thread_ble->NewStringUTF(data_str.c_str());

    jbyteArray j_abyteDataBuf = java_env_thread_ble->NewByteArray(data_buffer_size);
    java_env_thread_ble->SetByteArrayRegion(j_abyteDataBuf, 0, data_buffer_size, (jbyte*)data);

    jint android_gui_id_ret_value = java_env_thread_ble->CallIntMethod(
            java_jobject_bluetooth_service_instance, method_id,
            jstrUUIDBuf, j_abyteDataBuf
    );
    java_env_thread_ble->DeleteLocalRef(j_abyteDataBuf);

    return static_cast<int>(android_gui_id_ret_value);
}

int receive_ble_data(std::string uuid_str, const std::vector<unsigned char> &data)
{
    // TODO replace by operators
    std::transform(uuid_str.begin(), uuid_str.end(), uuid_str.begin(), ::tolower);
    for(auto itr = ble_interface_characteristics.begin(); itr != ble_interface_characteristics.end(); ++itr)
    {
        /* TOdo make operators for this */
        std::string uuid_copy(itr->get_uuid());
        std::transform(uuid_copy.begin(), uuid_copy.end(), uuid_copy.begin(), ::tolower);
        if (uuid_str == uuid_copy)
        {
            itr->rx_callback->receive_callback(data);
            return 0;
        }
    }
    return -1;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_texelography_hybridinsight_BluetoothService_android_1on_1create_1bluetooth_1service(JNIEnv *env, jobject obj)
{
    /* Remember the pointers to the object... */
    jclass clsLocal = env->GetObjectClass(obj);
    java_jobject_bluetooth_service = (jclass)env->NewWeakGlobalRef(clsLocal);

    /* .. and the instance */
    java_jobject_bluetooth_service_instance = env->NewWeakGlobalRef(obj);
}


enum BluetoothServiceBroadcastAction
{
    ACTION_GATT_CONNECTED = 0,
    ACTION_GATT_DISCONNECTED = 1,
    ACTION_GATT_SERVICES_DISCOVERED = 2,
    ACTION_DATA_RECEIVED = 3
};
extern "C"
JNIEXPORT void JNICALL
Java_com_texelography_hybridinsight_BluetoothService_android_1on_1broadcast_1receive(JNIEnv *env,
                                                                          jobject obj,

       jint event_type,
       jbyteArray uuid, jint uuid_length, jbyteArray data_bytes_array, jint data_bytes_array_length)
{

    BluetoothServiceBroadcastAction action = static_cast<BluetoothServiceBroadcastAction>(event_type);

    switch (action)
    {
        case ACTION_GATT_CONNECTED:
            break;
        case ACTION_GATT_DISCONNECTED:
            break;
        case ACTION_GATT_SERVICES_DISCOVERED:
            break;

        case ACTION_DATA_RECEIVED:
            /* Convert the UUID */
            char* uuid_buffer = new char[uuid_length + 1];
            env->GetByteArrayRegion (uuid, 0, uuid_length, reinterpret_cast<jbyte*>(uuid_buffer));
            uuid_buffer[uuid_length] = '\0';
            std::string uuid_str = uuid_buffer;
            delete[](uuid_buffer);


            /* Convert the content */
            unsigned char* buffer = new unsigned char[data_bytes_array_length + 1];
            env->GetByteArrayRegion (data_bytes_array, 0, data_bytes_array_length, reinterpret_cast<jbyte*>(buffer));
            buffer[data_bytes_array_length] = '\0';

            std::vector<unsigned char> buffer_data(buffer, buffer + data_bytes_array_length);
            //std::string buffer_str = buffer;
            delete[](buffer);
            //std::string data_to_send = "Reply from C++";
            //std::vector<unsigned char> data_bytes(data_to_send.begin(), data_to_send.end());
            //send_ble_data("6E400002-B5A3-F393-E0A9-E50E24DCCA2E", data_bytes);
            //std::vector<unsigned char> buffer_data(buffer_str.begin(), buffer_str.end());
            receive_ble_data(uuid_str, buffer_data);
            break;
    }

}

#if 0
extern "C"
JNIEXPORT void JNICALL
Java_com_texelography_hybridinsight_MainView_android_1ble_1data_1received(JNIEnv *env,
jobject obj, jbyteArray uuid, jint uuid_length, jbyteArray data_bytes_array, jint data_bytes_array_length)
{
    /* Convert the UUID */
    char* uuid_buffer = new char[uuid_length + 1];
    env->GetByteArrayRegion (uuid, 0, uuid_length, reinterpret_cast<jbyte*>(uuid_buffer));
    uuid_buffer[uuid_length] = '\0';
    std::string uuid_str = uuid_buffer;
    delete[](uuid_buffer);


    /* Convert the content */
    unsigned char* buffer = new unsigned char[data_bytes_array_length + 1];
    env->GetByteArrayRegion (data_bytes_array, 0, data_bytes_array_length, reinterpret_cast<jbyte*>(buffer));
    buffer[data_bytes_array_length] = '\0';

    std::vector<unsigned char> buffer_data(buffer, buffer + data_bytes_array_length);
    //std::string buffer_str = buffer;
    delete[](buffer);
    //std::string data_to_send = "Reply from C++";
    //std::vector<unsigned char> data_bytes(data_to_send.begin(), data_to_send.end());
    //send_ble_data("6E400002-B5A3-F393-E0A9-E50E24DCCA2E", data_bytes);
    //std::vector<unsigned char> buffer_data(buffer_str.begin(), buffer_str.end());
    receive_ble_data(uuid_str, buffer_data);
}

#endif

