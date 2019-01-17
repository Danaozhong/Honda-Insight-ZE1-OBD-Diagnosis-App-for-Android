# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

set(CPP_SOURCE_DIR ${PROJECT_SOURCE_DIR}/src/main/cpp)

add_definitions(-DPLATFORM_ANDROID)

# add the binary tree to the search path for include files
# so that we will find TutorialConfig.h
include_directories(
    "${CPP_SOURCE_DIR}"
    "${CPP_SOURCE_DIR}/obd_types"
    "${CPP_SOURCE_DIR}/driver.ble"
    "${CPP_SOURCE_DIR}/data_pool"
    "${CPP_SOURCE_DIR}/midware/abstraction"
    "${CPP_SOURCE_DIR}/midware/data_pool"
    "${CPP_SOURCE_DIR}/hmi/forms"
    "${CPP_SOURCE_DIR}/hmi/widgets"
)



add_library( # Sets the name of the library.
             native-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/native-lib.cpp


             # Bluetooth driver
             ${CPP_SOURCE_DIR}/driver/ble/ble_interface.h
             ${CPP_SOURCE_DIR}/driver/ble/ble_interface.cpp
             ${CPP_SOURCE_DIR}/driver/ble/adapt/ble_configuration.hpp
             ${CPP_SOURCE_DIR}/driver/ble/adapt/ble_configuration.cpp
             ${CPP_SOURCE_DIR}/driver/ble/platform_specific/ble_platform_specific.h
             ${CPP_SOURCE_DIR}/driver/ble/platform_specific/ble_platform_specific.cpp

             ${CPP_SOURCE_DIR}/obd_types/obd_data.cpp
             ${CPP_SOURCE_DIR}/obd_types/obd_data.h
             ${CPP_SOURCE_DIR}/obd_types/obd_error_code.h
             ${CPP_SOURCE_DIR}/obd_types/obd_readyness_flags.h

             #${CPP_SOURCE_DIR}/app/hmi/hmi_main.h
             #${CPP_SOURCE_DIR}/app/hmi/hmi_main.cpp

             ${CPP_SOURCE_DIR}/app/diagnosis_reader_application.h
             ${CPP_SOURCE_DIR}/app/diagnosis_reader_application.cpp

             ${CPP_SOURCE_DIR}/app/obd_data_repository.h
             ${CPP_SOURCE_DIR}/app/obd_data_repository.cpp

             ${CPP_SOURCE_DIR}/app/ble_communication/obd_ble_interface.h
             ${CPP_SOURCE_DIR}/app/ble_communication/obd_ble_interface.cpp
             ${CPP_SOURCE_DIR}/app/ble_communication/ble_request_queue.hpp
             ${CPP_SOURCE_DIR}/app/ble_communication/ble_request_queue.cpp

             ${CPP_SOURCE_DIR}/app/shared/obd_ble_shared.hpp
             ${CPP_SOURCE_DIR}/app/shared/obd_parameters.hpp
             ${CPP_SOURCE_DIR}/app/shared/obd_parameters.cpp

             # midware
             ${CPP_SOURCE_DIR}/midware/trace/trace.h
             ${CPP_SOURCE_DIR}/midware/trace/trace.cpp

             ${CPP_SOURCE_DIR}/midware/data_pool/data_pool.hpp
             ${CPP_SOURCE_DIR}/midware/data_pool/data_pool.cpp
             ${CPP_SOURCE_DIR}/midware/data_pool/data_pool_accessor.hpp
             ${CPP_SOURCE_DIR}/midware/data_pool/data_pool_accessor.cpp

             # For the Java interface
             ${CPP_SOURCE_DIR}/midware/data_pool/data_pool_accessor_java.cpp

             ${CPP_SOURCE_DIR}/midware/threads/cyclic_thread.h
             ${CPP_SOURCE_DIR}/midware/threads/cyclic_thread.cpp

             ${CPP_SOURCE_DIR}/midware/events/event_handler.h
             ${CPP_SOURCE_DIR}/midware/events/event_handler.cpp

             # HMI subsystem
             #${CPP_SOURCE_DIR}/hmi/forms/form_base.hpp
             #${CPP_SOURCE_DIR}/hmi/forms/form_base.cpp
             #${CPP_SOURCE_DIR}/hmi/forms/form_main.hpp
             #${CPP_SOURCE_DIR}/hmi/forms/form_main.cpp
             #${CPP_SOURCE_DIR}/hmi/forms/form_obd_value_list.hpp
             #${CPP_SOURCE_DIR}/hmi/forms/form_obd_value_list.cpp

             # HMI widgets
             #${CPP_SOURCE_DIR}/hmi/widgets/gui_element.hpp
             #${CPP_SOURCE_DIR}/hmi/widgets/gui_element.cpp
             #${CPP_SOURCE_DIR}/hmi/widgets/obd_value_bar.hpp
             #${CPP_SOURCE_DIR}/hmi/widgets/obd_value_bar.cpp
             #${CPP_SOURCE_DIR}/hmi/widgets/obd_value_list_entry.hpp
             #${CPP_SOURCE_DIR}/hmi/widgets/obd_value_list_entry.cpp

            # ${CPP_SOURCE_DIR}/hmi/hmi_state_machine.hpp
             #${CPP_SOURCE_DIR}/hmi/hmi_state_machine.cpp
             #${CPP_SOURCE_DIR}/hmi/hmi_thread.hpp
             #${CPP_SOURCE_DIR}/hmi/hmi_thread.cpp

             # Main system
             ${CPP_SOURCE_DIR}/app/main.cpp

             )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )


# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       native-lib
                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )
