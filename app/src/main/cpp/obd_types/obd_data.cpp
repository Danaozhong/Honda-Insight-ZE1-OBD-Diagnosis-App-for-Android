
/* System header */
#include <string>

/* Own header */
#include "midware/trace/trace.h"
#include "obd_types/obd_data.h"




DataPoolOBDData OBDValueHelper::to_dpool_obd_data(const OBDValue &st_obd_value)
{
    DataPoolOBDData result;
    result.str_description = st_obd_value.description;
    result.i32_identifier = st_obd_value.identifier;
    result.f_max = st_obd_value.max;
    result.f_min = st_obd_value.min;
    result.str_unit = st_obd_value.value_unit;
    result.f_value = st_obd_value.value_f;
    result.f_zero = st_obd_value.zero;
    return result;
}

void OBDValueHelper::v_update_from_dpool_obd_data(OBDValue &st_obd_value, const DataPoolOBDData& st_dpool_obd_data)
{
    st_obd_value.value_f = st_dpool_obd_data.f_value;
}

OBDData* OBDData::create_from_obd_value(const OBDValue &obd_value)
{
    switch (obd_value.type)
    {
        case OBD_VALUE_NUMERIC:
            return new NumericOBDData(obd_value);
        case OBD_VALUE_BOOLEAN:
            return new BooleanOBDData(obd_value);
        default:
            return nullptr;
    }
}

OBDData::OBDData(int identifier, const std::string &description, const std::string &unit)
        : identifier(identifier), description(description), unit(unit)
{
    TRACE_PRINTF("Data" + this->get_description() + " is being destroyed!");
}

OBDData::~OBDData()
{
    TRACE_PRINTF("Data" + this->get_description() + " is being destroyed!");
}

int OBDData::get_data_identifier() const
{
    return this->identifier;
}

const std::string& OBDData::get_description() const
{
    return this->description;
}

const std::string& OBDData::get_unit() const
{
    return this->unit;
}

NumericOBDData::NumericOBDData(int identifier, const std::string &description, const std::string &unit,float min, float max, float zero)
        : OBDData(identifier, description, unit), min(min), max(max), zero(zero), value(0.0f)
{}


NumericOBDData::NumericOBDData(const OBDValue& obd_value)
        : OBDData(obd_value.identifier, obd_value.description, obd_value.value_unit),
          min(obd_value.min), max(obd_value.max), value(obd_value.value_f)
{}

OBDValue NumericOBDData::get_value() const
{
    OBDValue ret_val;
    snprintf(ret_val.description, OBD_VALUE_DESCRIPTION_MAX_LENGTH, "%s", this->description.c_str());
    ret_val.max = this->max;
    ret_val.min = this->min;
    ret_val.zero = this->zero;
    this->value_mutex.lock();
    ret_val.value_f = this->value;
    this->value_mutex.unlock();
    ret_val.type = OBD_VALUE_NUMERIC;
    return ret_val;
}

size_t NumericOBDData::get_value_size() const
{
    return sizeof(float);
}

void NumericOBDData::set_value(float value)
{
    this->value_mutex.lock();
    this->value = value;
    this->value_mutex.unlock();
}

float NumericOBDData::get_min() const { return this->min; }
float NumericOBDData::get_max() const { return this->max; }
float NumericOBDData::get_zero() const { return this->zero; }


BooleanOBDData::BooleanOBDData(const OBDValue& obd_value)
    :  OBDData(obd_value.identifier, obd_value.description, obd_value.value_unit), value(obd_value.value_b)
{}

OBDValue BooleanOBDData::get_value() const
{
    OBDValue ret_val;
    snprintf(ret_val.description, OBD_VALUE_DESCRIPTION_MAX_LENGTH, "%s", this->description.c_str());
    ret_val.value_b = this->value;
    ret_val.type = OBD_VALUE_BOOLEAN;
    return ret_val;
}

size_t BooleanOBDData::get_value_size() const
{
    return sizeof(bool);
}
