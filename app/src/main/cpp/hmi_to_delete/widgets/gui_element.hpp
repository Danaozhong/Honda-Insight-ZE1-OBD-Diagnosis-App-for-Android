//
// Created by Clemens on 16.04.2018.
//

#ifndef ANDROID_GUI_ELEMENT_H
#define ANDROID_GUI_ELEMENT_H

namespace HMI
{
    class GUIElement
    {
    public:
        GUIElement() :gui_id(GUI_ID_COUNT++) {}

        virtual ~GUIElement()
        {}

        virtual void update_view() const = 0;

        int get_gui_id() const { return gui_id; }
    protected:
        const int gui_id;

        static int GUI_ID_COUNT;
    };
}

#endif //ANDROID_GUI_ELEMENT_H
