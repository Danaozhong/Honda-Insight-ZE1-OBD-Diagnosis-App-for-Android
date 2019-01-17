#ifndef _OBD_DATA_REPOSITORY_H
#define _OBD_DATA_REPOSITORY_H

/*
 * System header
 */
#include <vector>
#include<memory>
/*
 * Local header
 */
#include "obd_data.h"

class OBDDataRepository
{
public:
    OBDDataRepository()
    {}

    void add_obd_data(const std::shared_ptr<OBDData> &data);

    /**
     * Returns the pointer to the OBD data structure, based on the unique identifier.
     * Should implement a hash table.
     * @param identifier
     * @return
     */
    std::shared_ptr<OBDData> get_obd_data_by_identifier(int identifier) const;

    void create_shared_data() const;
    void update_shared_data() const;
//#private:
    std::vector<std::shared_ptr<OBDData>> data_repository;
};

#endif /* _OBD_DATA_REPOSITORY_H */
