//
// Created by Clemens on 11.12.2017.
//

/* System header */
#include <algorithm>    // std::find_if

/* Local header */
#include "obd_data_repository.h"
#include "midware/data_pool/data_pool.hpp"

void OBDDataRepository::add_obd_data(const std::shared_ptr<OBDData> &data)
{
    /* TODO Check if identifier is already stored */
    this->data_repository.push_back(data);
}

std::shared_ptr<OBDData> OBDDataRepository::get_obd_data_by_identifier(int identifier) const
{
    /* TODO implement as a look up table */
    auto it = std::find_if (
            this->data_repository.begin(), this->data_repository.end(),
            [identifier](const std::shared_ptr<OBDData> &obd_data)
                { return obd_data->get_data_identifier() == identifier; });

    if (it == this->data_repository.end())
    {
        return nullptr;
    }
    return *it;
}

void OBDDataRepository::create_shared_data() const
{
    for (int i = 0; i < this->data_repository.size(); ++i) /* TOdo change to iterator */
    {
        DataPoolOBDData current_data = { 0 };

        std::shared_ptr<NumericOBDData> current_item = std::dynamic_pointer_cast<NumericOBDData, OBDData>(this->data_repository[i]);

        /* TODO support other OBD Data types as well */
        current_data.f_min = current_item->get_min();
        current_data.f_max = current_item->get_max();
        current_data.f_zero = current_item->get_zero();
        current_data.f_value = current_item->get_value().value_f;
        current_data.i32_identifier = current_item->get_data_identifier();
        current_data.str_unit = current_item->get_unit();
        current_data.str_description = current_item->get_description();
        DataPoolAccessor::set_obd_data_array_element(i, current_data);
    }
}

void OBDDataRepository::update_shared_data() const
{
    for (int i = 0; i < this->data_repository.size(); ++i)
    {
        DataPoolOBDData current_data = DataPoolAccessor::get_obd_data_array_element(i);
        current_data.f_value = this->data_repository[i]->get_value().value_f;
        DataPoolAccessor::set_obd_data_array_element(i, current_data);
    }
}