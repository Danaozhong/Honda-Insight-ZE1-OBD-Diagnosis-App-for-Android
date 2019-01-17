
#include <thread>
#include <mutex>
#include <condition_variable>
#include <atomic>
#include <chrono>
#include <stdlib.h>     /* srand, rand */

#include <jni.h>
#include <string>
#include <cstdint>

/* Foreign header files */
#include "driver/ble/ble_interface.h"

/* Own header files */
#include "midware/trace/trace.h"
#include "obd_data_repository.h"
#include "driver/ble/ble_interface.h"
#include "app/ble_communication/obd_ble_interface.h"
#include "app/shared/obd_ble_shared.hpp"

std::condition_variable x;
std::atomic<int> stop_main_thread(0);
#include "app/diagnosis_reader_application.h"
#if 0
void thread_communication_main()
{


    while(true)
    {
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
        /* Simulate modifying data */
        int num_of_data = 5;
        for (int i = 0; i < num_of_data; ++i)
        {
            int change = rand() % 201 - 100; /* -100 to +100 */
            std::shared_ptr<NumericOBDData> data =
                    std::static_pointer_cast<NumericOBDData, OBDData>(data_repository.data_repository[i]);

            float value_span = data->get_max() - data->get_min();
            float new_value = data->get_value().value_f + static_cast<float>(change) / 10000.0f * value_span;

            if (new_value > data->get_max())
            {
                new_value = data->get_max();
            }
            else if (new_value < data->get_min())
            {
                new_value = data->get_min();
            }
            data->set_value(new_value);
        }

        /* Updata data pool */
        data_repository.write_to_data_pool();
        /* Send signal to HMI */
        std::string data_to_send = "Reply from C++";
        std::vector<unsigned char> data_bytes(data_to_send.begin(), data_to_send.end());
        send_ble_data("6E400002-B5A3-F393-E0A9-E50E24DCCA2E", data_bytes);

    }

}
#endif


#include "app/shared/obd_parameters.hpp"

extern "C"
JNIEXPORT void JNICALL
Java_com_texelography_hybridinsight_MainView_android_1on_1create_1form(JNIEnv *env, jobject obj)
{
    /* First, get the Java VM - to be used in other threads */
    jint result = env->GetJavaVM(&java_vm);
    if (result < 0) {
        // TODO error handler
    }


    /* Get a globally valid (among all threads) reference to the object class. */
    jclass clsLocal = env->GetObjectClass(obj);
    java_jobject_main_view = (jclass)env->NewWeakGlobalRef(clsLocal);

    /* Create a globally valid reference to the object itself */
    java_jobject_main_view_instance = env->NewWeakGlobalRef(obj);

    /* Launch main thread. */
    std::thread* thread_main = new std::thread(App::main);
}