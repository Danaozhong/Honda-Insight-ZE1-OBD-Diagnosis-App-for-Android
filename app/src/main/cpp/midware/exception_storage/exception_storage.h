//
// Created by Clemens on 25.04.2018.
//

#ifndef ANDROID_EXCEPTION_STORAGE_H
#define ANDROID_EXCEPTION_STORAGE_H

/* Libraries */
#include <vector>



class ExceptionStorage
{
public:
    static ExceptionStorage& get()
    {
        static ExceptionStorage exception_storage;
        return exception_storage;
    }

    static void trigger_exception();
private:
};


#endif //ANDROID_EXCEPTION_STORAGE_H
