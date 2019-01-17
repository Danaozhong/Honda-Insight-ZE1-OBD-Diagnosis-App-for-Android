#ifndef _OBD_DATA_H
#define _OBD_DATA_H

/* System header */
#include <string>
#include <mutex>
#include <atomic>
#include <vector>

/* Own header files */
#include "midware/data_pool/data_pool.hpp"

#define OBD_VALUE_DESCRIPTION_MAX_LENGTH 40
#define OBD_VALUE_SHORT_DESCRIPTION_LENGTH 4
#define OBD_VALUE_VALUE_UNIT_LENGTH 10


enum OBDValueType
{
    OBD_VALUE_NUMERIC,
    OBD_VALUE_BOOLEAN
};


struct OBDValue
{
    unsigned char identifier;
    char description[OBD_VALUE_DESCRIPTION_MAX_LENGTH];
    char short_description[OBD_VALUE_SHORT_DESCRIPTION_LENGTH];
    OBDValueType type;
    float min;
    float max;
    float zero;
    char value_unit[OBD_VALUE_VALUE_UNIT_LENGTH];
    float value_f;
    bool value_b;
};

typedef std::vector<OBDValue> OBDDataList;


namespace OBDValueHelper
{
    /* Data Pool uses own data types, therefore need to write conversions */
    DataPoolOBDData to_dpool_obd_data(const OBDValue &st_obd_value);
    void v_update_from_dpool_obd_data(OBDValue &st_obd_value, const DataPoolOBDData& st_dpool_obd_data);
}

class OBDData
{
public:
    /* Factory method */
    static OBDData* create_from_obd_value(const OBDValue &obd_value);

    OBDData(int identifier, const std::string &description, const std::string &unit);
    virtual ~OBDData();



    virtual const std::string& get_description() const;
    virtual const std::string& get_unit() const;

    virtual OBDValue get_value() const = 0;

    virtual size_t get_value_size() const = 0;

    int get_data_identifier() const;

protected:
    std::string description;
    std::string unit;

    /* Unique index for an OBD data. */
    int identifier;

};

class NumericOBDData: public OBDData
{
public:
    NumericOBDData(int identifier, const std::string &description, const std::string &unit, float min, float max, float zero = 0.0);

    NumericOBDData(const OBDValue& obd_value);
    virtual OBDValue get_value() const;

    virtual size_t get_value_size() const;

    void set_value(float value);

    float get_min() const;
    float get_max() const;
    float get_zero() const;

private:
    float value; /* Should be thread-safe */
    float min;
    float max;
    float zero;

    mutable std::mutex value_mutex;
};

class BooleanOBDData: public OBDData
{
public:
    BooleanOBDData(int identifier, const std::string &description, bool value);

    BooleanOBDData(const OBDValue& obd_value);

    virtual OBDValue get_value() const;

    virtual size_t get_value_size() const;
private:
    std::atomic<bool> value;
};
#endif  /* _OBD_DATA_H */

