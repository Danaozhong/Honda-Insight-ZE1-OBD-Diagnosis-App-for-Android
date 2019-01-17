//
// Created by Clemens on 01.07.2018.
//

#ifndef ANDROID_DATA_POOL_ACCESSOR_H
#define ANDROID_DATA_POOL_ACCESSOR_H


#include <string>

struct DataPoolOBDData
{
    /* Value data types */
    int i32_identifier;
    float f_min;
    float f_max;
    float f_zero;
    float f_value;
    std::string str_description;
    std::string str_unit;
};

struct DataPoolOBDReadynessCode
{
    int i32CatalystState;
    int i32HeatedCatalystState;
    int i32EVAPSystemState;
    int i32SecondaryAirSystemState;
    int i32OxygenSensorState;
    int i32OxygenSensorHeaterState;
    int I32EGRState;
};

struct DataPoolIMAState
{
    int i32FanState;
    bool bo_request_override_soc;
    unsigned int u32_request_override_soc_new_value;

    bool bo_request_override_fan_state;
    unsigned int u32_request_override_fan_state_value;
};

struct DataPoolOBDDTC
{
    unsigned int u32_dtc_code;
};


struct DataPoolDisplayedElementData
{
    /** reference to the OBD II value which should be displayed */
    unsigned int u32_element;

    /** In which row should the graph be listed? */
    unsigned int u32_row;
    unsigned int u32_col;
    unsigned int u32_row_span;
    unsigned int u32_col_span;
};

namespace DataPoolAccessor
{
    /* This could be generated... Or made more generic??*/
    DataPoolOBDData get_obd_data_array_element(int i32_element);
    void set_obd_data_array_element(int element, const DataPoolOBDData &value);
    int get_obd_data_array_num_of_elements();

    /* Array sized are fixed (static), and cannot be changed during runtime */
    DataPoolDisplayedElementData get_main_view_elements_of_interest_array_element(int i32_element);
    void set_main_view_elements_of_interest_array_element(int element, DataPoolDisplayedElementData o_value);
    int get_main_view_elements_of_interest_array_size();

    int get_main_view_elements_of_interest_count();
    void set_main_view_elements_of_interest_count(int i32_value);

    void set_main_view_num_of_cols(int i32_value);
    int get_main_view_num_of_cols();

    void set_main_view_num_of_rows(int i32_value);
    int get_main_view_num_of_rows();

    bool get_read_all_obd_values();
    int set_read_all_obd_values(bool value);

    DataPoolOBDReadynessCode get_obd_readyness_code();
    void set_obd_readyness_code(DataPoolOBDReadynessCode value);

    DataPoolIMAState get_ima_state();
    void set_ima_state(DataPoolIMAState value);

    DataPoolOBDDTC get_obd_dtc_array_element(int element);
    void set_obd_dtc_array_element(int i32Element, const DataPoolOBDDTC &value);
    int get_obd_dtc_array_num_of_elements();

    int get_obd_dtc_count();
    void set_obd_dtc_count(int value);

};

#endif //ANDROID_DATA_POOL_ACCESSOR_H
