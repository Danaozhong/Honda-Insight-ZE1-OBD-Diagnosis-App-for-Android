//
// Created by Clemens on 20.12.2017.
//

#ifndef ANDROID_FORM_MAIN_H_H
#define ANDROID_FORM_MAIN_H_H

#include <memory>

#include "data_pool.hpp"

#include "hmi/forms/form_base.hpp"
#include "hmi/widgets/obd_value_bar.hpp"

namespace HMI
{
    struct FormMainOBDDataLayout
    {
        DpoolOBDData obd_data;

        /* Display related parameters */
        int column;
        int row;
        int colspan;
        int rowspan;
    };

    struct FormMainLayoutData
    {
        std::vector<FormMainOBDDataLayout> obd_data;
        int rows;
        int columns;

        bool show_hybrid_indicator;
    };


    class FormMain : public FormBase
    {
    public:
        FormMain();

        /* Destructor */
        virtual ~FormMain() {}

        virtual void create_view();
        virtual void update_view();

        void set_displayed_identifiers(const std::vector<unsigned int> &displayed_identifiers);

    private:
        void update_data_from_dpool();

        bool is_identifier_displayed(const unsigned char identifier) const;

        FormMainLayoutData layout_data;

        /* Platform specific */
        void add_obd_value_bar_to_android_gui(const OBDValueBar &obd_value_bar);

        std::vector<std::shared_ptr<OBDValueBar>> obv_value_bars;

        bool displayed_identifiers[255];


    };
}
#endif //ANDROID_FORM_MAIN_H_H
