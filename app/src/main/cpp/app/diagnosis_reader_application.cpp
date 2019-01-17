//
// Created by Clemens on 26.03.2018.
//

#include <thread>
#include <mutex>
#include <condition_variable>
#include <atomic>
#include <chrono>
#include <stdlib.h>     /* srand, rand */

#include <jni.h>
#include <string>
//#include <form_main.hpp>
#include <cstdint>

/* Foreign header files */
#include "driver/ble/ble_interface.h"

/* Own header files */
#include "midware/trace/trace.h"
#include "obd_data_repository.h"
#include "driver/ble/ble_interface.h"
#include "app/ble_communication/obd_ble_interface.h"
#include "app/shared/obd_ble_shared.hpp"
#include "app/shared/obd_parameters.hpp"

#include "diagnosis_reader_application.h"
//#include "app/hmi/hmi_main.h"


DiagnosisReaderApplication::DiagnosisReaderApplication()
 : stop_main_thread(0)
{
}


DiagnosisReaderApplication::~DiagnosisReaderApplication()
{

}


int DiagnosisReaderApplication::application_main()
{
    /* Bootup */

    /* Give the Java HMI some seconds to start up */
    std::this_thread::sleep_for(std::chrono::milliseconds(500));

    TRACE_PRINTF("Main Thread started!");
    /* Create dummy signals for testing */
    for(auto obd_value : obd_ii_diagnosis_data)
    {
        std::shared_ptr<OBDData> obd_data(OBDData::create_from_obd_value(obd_value));
        data_repository.add_obd_data(obd_data);
    }
    data_repository.create_shared_data();

    /* spawn bluetooth communication thread */
    BLEOBDDataClient ble_obd_client;

    //std::thread thread_communication(thread_communication_main);

#if 0
    /* spawn HMI communication thread */
    std::thread thread_hmi(thread_hmi_main);
#endif
    while(!stop_main_thread.load())
    {
        /* Some workload may be here */
        if (true == ble_obd_client.is_connected())
        {
            /* Check if there was a state change for the OBD Data transmission mode */
            OBDDataTransmissionMode new_data_transmission_mode = TRANSMISSION_MODE_ONLY_SELECTED_DATA_FAST;
            if (true == DataPoolAccessor::get_read_all_obd_values())
            {
                new_data_transmission_mode = TRANSMISSION_MODE_ALL_DATA_SLOW;
            }
            /* The check if there was a change is done inside the setter */
            ble_obd_client.set_obd_data_transmission_mode(new_data_transmission_mode);
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }
    return 0;
    //stop_threads.notify_all();
}


OBDDataRepository& DiagnosisReaderApplication::get_data_repository()
{
    return this->data_repository;
}

namespace
{
    DiagnosisReaderApplication *application = nullptr;
}

namespace App
{
    DiagnosisReaderApplication& get_app()
    {
        return *application;
    }

    int main()
    {
        application = new DiagnosisReaderApplication();
        application->application_main();
        return 0;
    }
}

