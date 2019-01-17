//
// Created by Clemens on 20.12.2017.
//

#ifndef ANDROID_DATA_POOL_HPP
#define ANDROID_DATA_POOL_HPP






/*
 * CONCEPT
 * Zugriff via Get / set functions -> selbst geschrieben
 *
 * Unterligende Daten auschließlich POD
 * Datenstrukturen werden automatisch generiert für Java und C++
 *
 * Alternativ:
 * Jedes Datenelement eine Struct / array / POD
 * Get/set auf Structs -> Die komplette Struct wird returned/überschrieben
 * Get auf POD -> Der POD wird gesetzt / überschrieben
 * Get auf Array
 *
 *
 * Verwendung:
 * MyFieldXYZ = Dpool_Get_data_from_array(DPOOL_OBD_DATA, 11);
 *
 *
 *
 *
 * Ganz andere Idee:
 * Jedes Element in eine Klasse packen
 * Jede Klasse hat eine eigene Getter / Setter Funktion
 * Beide werden gewrapped, aber nur in Cpp implementiert
 */

/* std library headerfiles */
#include <vector>
#include <mutex>
#include <atomic>

/* own header files */
#include "midware/data_pool/data_pool_accessor.hpp"



/* Defined here is a list of all data items that are being shared between app and hmi. This data is
 * only allowed to use structs of basic data types or stdlib data types.
 */



class DataPool
{
public:
    DataPool();

    std::vector<DataPoolOBDData> dpool_obd_data;
    std::mutex dpool_obd_data_mutex;


    std::vector<DataPoolDisplayedElementData> dpool_hmi_main_view_elements_of_interest;
    std::mutex dpool_hmi_main_view_elements_of_interest_mutex;

    std::atomic<int32_t> i32_elements_of_interest_count;


    std::atomic<int32_t> i32_hmi_main_views_num_of_cols;
    std::atomic<int32_t> i32_hmi_main_views_num_of_rows;

    std::atomic<bool> bo_read_all_values;
};




    //extern std::vector<unsigned int> dpool_hmi_main_view_elements_of_interest;

    //extern std::atomic<bool> bo_read_all_values;
    /* TODO Figura out how to do this nicely. */


//}
#endif //ANDROID_DATA_POOL_HPP
