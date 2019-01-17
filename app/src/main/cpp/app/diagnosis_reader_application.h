//
// Created by Clemens on 26.03.2018.
//

#ifndef ANDROID_DIAGNOSIS_READER_APPLICATION_H
#define ANDROID_DIAGNOSIS_READER_APPLICATION_H

#include "app/obd_data_repository.h"

class DiagnosisReaderApplication
{
public:
    DiagnosisReaderApplication();
    ~DiagnosisReaderApplication();


    int application_main();

    OBDDataRepository& get_data_repository();
private:
    OBDDataRepository data_repository;
    std::atomic<int> stop_main_thread;

};

namespace App
{
    DiagnosisReaderApplication& get_app();
    int main();
}


#endif //ANDROID_DIAGNOSIS_READER_APPLICATION_H
