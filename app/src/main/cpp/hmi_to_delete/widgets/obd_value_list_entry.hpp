//
// Created by Clemens on 20.12.2017.
//

#ifndef _OBD_VALUE_LIST_ENTRY_H
#define _OBD_VALUE_LIST_ENTRY_H

/* Libraries*/
#include <string>

/* Own header */
#include "hmi/forms/form_base.hpp"
#include "hmi/widgets/gui_element.hpp"

namespace HMI
{
    /*
    This should just be a wrapper to the java class und simply forward everyhing to java
    */
    class OBDValueListEntry : public GUIElement
    {
    public:
        /* Default constructor */
        OBDValueListEntry(FormBase &owner, int identifier);

        /* Constructor */
        OBDValueListEntry(FormBase &owner, int identifier, float min, float max, float zero, float value,
                          const std::string &description, const std::string &unit, bool checked);

        void set_min(float min);
        void set_max(float max);
        void set_zero(float zero);
        void set_value(float value);
        void set_description(const std::string &description);
        void set_checked(bool bo_checked);

        bool get_checked() const;

        int get_identifier() const;

        virtual void update_view() const;


    private:
        FormBase &m_owner;

        const int m_identifier;

        float m_min;
        float m_max;
        float m_zero;
        float m_value;
        std::string description;
        std::string unit;
        bool m_bo_checked;
        unsigned char m_index;

        mutable bool m_bo_value_changed;



        /* Platform-specific */
//#ifdef PLATFORM_ANDROID
    public:
        int android_gui_id;

        void create_android_gui_element();
        //void update_android_gui_element() const;

//#endif
    };
}
#endif /* _OBD_VALUE_LIST_ENTRY_H */
