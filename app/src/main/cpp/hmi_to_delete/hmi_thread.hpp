//
// Created by Clemens on 16.04.2018.
//

#ifndef ANDROID_HMI_THREAD_H
#define ANDROID_HMI_THREAD_H

#include <deque>
#include <mutex>
#include <jni.h>

/* Foreign header */
#include "midware/threads/cyclic_thread.h"

/* Own header */
#include "hmi/hmi_state_machine.hpp"

namespace HMI
{
    class HMICyclicThread : public CyclicThread
    {
    public:
        HMICyclicThread();

        virtual ~HMICyclicThread();

        virtual int start();
        virtual void run();

        HMIStateMachine& get_state_machine();

        JNIEnv* get_java_env_thread_hmi() const;
    private:
        JNIEnv* java_env_thread_hmi;
        HMIStateMachine m_o_hmi_state_machine;
    };

    enum HMIEventType
    {
        HMIEvent_ShowForm = 0,
        HMIEvent_CloseForm = 1,
        HMIEvent_PressButton = 2,
        HMIEvent_MenuButtonClick = 3,
        HMIEvent_FormOBDValueList_ConfirmSelection = 4
    };

    class HMIEvent
    {
    public:
        virtual ~HMIEvent();

        HMIEventType m_event_type;
        int m_button_id;
        int m_menu_button_click_id;
    };

    // TODO change HMIEventQueue to use virtual classes
    class HMIEventButtonPress : public HMIEvent
    {
    public:
        virtual ~HMIEventButtonPress();
        int m_button_id;
    };

    class HMIEventQueue
    {
    public:
        int enqueue_menu_button_click_event(int menu_button_click_id);

        int enqueue_event(const HMIEvent &hmi_event);
        int get_event(HMIEvent& event);
    private:
        std::deque<HMIEvent> m_event_queue;
        std::mutex message_queue_mutex;

    };

    HMIEventQueue& get_event_queue()
    {
        static HMIEventQueue hmi_event_queue;
        return hmi_event_queue;
    }

    extern HMICyclicThread* p_o_hmi_thread;
    HMICyclicThread& get_hmi_thread()
    {
        return *p_o_hmi_thread;
    }
}


#endif //ANDROID_HMI_THREAD_H
