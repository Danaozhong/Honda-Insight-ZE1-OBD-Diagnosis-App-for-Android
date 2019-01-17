//
// Created by Clemens on 20.12.2017.
//

#ifndef _OBD_VALUE_BAR_H
#define _OBD_VALUE_BAR_H

/* System header */
#include <string>

/* Own header */
#include "hmi/widgets/gui_element.hpp"

namespace HMI
{
    /*
    This should just be a wrapper to the java class und simply forward everyhing to java
    */
    class OBDValueBar : public GUIElement
    {
    public:
        /* Constructor */
        OBDValueBar(int identifier, float min, float max, float zero, float value, const std::string &description, const std::string &unit);

        void set_min(float min);
        void set_max(float max);
        void set_zero(float zero);
        void set_value(float value);
        void set_description(const std::string &description);

        int get_identifier() const;

        virtual void update_view() const;


    private:
        const int m_identifier;

        float m_min;
        float m_max;
        float m_zero;
        float m_value;
        std::string description;
        std::string unit;

        float width;
        float height;

        int bar_color;
        int font_color;

        /* Platform-specific */
//#ifdef PLATFORM_ANDROID
    public:
        int android_gui_id;

        void create_android_gui_element();
        void update_android_gui_element() const;

//#endif
    };
}
#endif /* _OBD_VALUE_BAR_H */
