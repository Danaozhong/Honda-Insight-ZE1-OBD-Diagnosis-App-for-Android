//
// Created by Clemens on 27.12.2017.
//



#include "data_pool.hpp"

#include "app/shared/obd_parameters.hpp"


DataPool::DataPool()
{
    /* Initialize the Dpool data with the static DPOOL list */
    for (auto itr = obd_ii_diagnosis_data.begin(); itr != obd_ii_diagnosis_data.end(); ++itr)
    {
        this->dpool_obd_data.push_back(OBDValueHelper::to_dpool_obd_data(*itr));
    }

    for (int i = 0; i < 12; ++i)
    {
        DataPoolDisplayedElementData o_current_displayed_element_data = { .u32_element = static_cast<uint32_t>(i), .u32_col = 0, .u32_row = 0 };
        this->dpool_hmi_main_view_elements_of_interest.push_back(o_current_displayed_element_data);
    }

    i32_elements_of_interest_count = 9;
    i32_hmi_main_views_num_of_cols = 3;
    i32_hmi_main_views_num_of_rows = 3;
}

namespace
{
    DataPool o_data_pool;
}

/* This could be generated...*/
DataPoolOBDData DataPoolAccessor::get_obd_data_array_element(int element)
{
    std::lock_guard<std::mutex> lock(o_data_pool.dpool_obd_data_mutex);
    if (element < 0 || element >= o_data_pool.dpool_obd_data.size())
    {
        throw;
    }
    return o_data_pool.dpool_obd_data[element];
}

void DataPoolAccessor::set_obd_data_array_element(int element, const DataPoolOBDData &value)
{
    std::lock_guard<std::mutex> lock(o_data_pool.dpool_obd_data_mutex);
    if (element < 0 || element >= o_data_pool.dpool_obd_data.size())
    {
        throw;
    }
    o_data_pool.dpool_obd_data[element] = value;
}

int DataPoolAccessor::get_obd_data_array_num_of_elements()
{
    std::lock_guard<std::mutex> lock(o_data_pool.dpool_obd_data_mutex);
    return o_data_pool.dpool_obd_data.size();
}

/* Array sized are fixed (static), and cannot be changed during runtime */
DataPoolDisplayedElementData DataPoolAccessor::get_main_view_elements_of_interest_array_element(int element)
{
    std::lock_guard<std::mutex> lock(o_data_pool.dpool_hmi_main_view_elements_of_interest_mutex);
    if (element < 0 || element >= o_data_pool.dpool_hmi_main_view_elements_of_interest.size())
    {
        throw;
    }
    return o_data_pool.dpool_hmi_main_view_elements_of_interest[element];
}

void DataPoolAccessor::set_main_view_elements_of_interest_array_element(int element, DataPoolDisplayedElementData value)
{
    std::lock_guard<std::mutex> lock(o_data_pool.dpool_hmi_main_view_elements_of_interest_mutex);
    if (element < 0 || element >= o_data_pool.dpool_hmi_main_view_elements_of_interest.size())
    {
        throw;
    }
    o_data_pool.dpool_hmi_main_view_elements_of_interest[element] = value;
}

int DataPoolAccessor::get_main_view_elements_of_interest_array_size()
{
    std::lock_guard<std::mutex> lock(o_data_pool.dpool_hmi_main_view_elements_of_interest_mutex);
    return o_data_pool.dpool_hmi_main_view_elements_of_interest.size();
}


int DataPoolAccessor::get_main_view_elements_of_interest_count()
{
    return o_data_pool.i32_elements_of_interest_count;
}

void DataPoolAccessor::set_main_view_elements_of_interest_count(int value)
{
    o_data_pool.i32_elements_of_interest_count = value;
}

void DataPoolAccessor::set_main_view_num_of_cols(int i32_value)
{
    o_data_pool.i32_hmi_main_views_num_of_cols = i32_value;
}

int DataPoolAccessor::get_main_view_num_of_cols()
{
    return o_data_pool.i32_hmi_main_views_num_of_cols;
}

void DataPoolAccessor::set_main_view_num_of_rows(int i32_value)
{
    o_data_pool.i32_hmi_main_views_num_of_rows = i32_value;
}

int DataPoolAccessor::get_main_view_num_of_rows()
{
    return o_data_pool.i32_hmi_main_views_num_of_rows;
}

bool DataPoolAccessor::get_read_all_obd_values()
{
    return o_data_pool.bo_read_all_values;
}

int DataPoolAccessor::set_read_all_obd_values(bool value)
{
    o_data_pool.bo_read_all_values = value;
    return 0;
}

DataPoolOBDReadynessCode DataPoolAccessor::get_obd_readyness_code()
{
}

void DataPoolAccessor::set_obd_readyness_code(DataPoolOBDReadynessCode value)
{}

DataPoolIMAState DataPoolAccessor::get_ima_state()
{
    return DataPoolIMAState();
}
void DataPoolAccessor::set_ima_state(DataPoolIMAState value)
{}

DataPoolOBDDTC DataPoolAccessor::get_obd_dtc_array_element(int element)
{
    return DataPoolOBDDTC();
}
void DataPoolAccessor::set_obd_dtc_array_element(int i32Element, const DataPoolOBDDTC &value)
{}
int DataPoolAccessor::get_obd_dtc_array_num_of_elements()
{
    return 0;
}

int DataPoolAccessor::get_obd_dtc_count()
{
    return 0;
}

void DataPoolAccessor::set_obd_dtc_count(int value)
{}

