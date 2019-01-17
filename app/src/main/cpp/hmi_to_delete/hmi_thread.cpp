//
// Created by Clemens on 16.04.2018.
//

#include <mutex>
#include "hmi_thread.hpp"



#include <thread>

/* Foreign header */
#include "midware/trace/trace.h"

/* Own header */
#include "hmi/forms/form_obd_value_list.hpp"
#include "hmi/hmi_state_machine.hpp"
#include "hmi/forms/form_main.hpp"
#include "hmi/hmi_state_machine.hpp"

HMI::HMIStateMachine* p_o_hmi_state_machine = nullptr;

using namespace HMI;

HMICyclicThread* p_o_hmi_thread = nullptr;

HMICyclicThread::HMICyclicThread()
        : CyclicThread("HMI_10ms", std::chrono::milliseconds(50)),
          java_env_thread_hmi(nullptr)
{
    p_o_hmi_thread = this;
}

HMICyclicThread::~HMICyclicThread()
{
}

int HMICyclicThread::start()
{
    TRACE_PRINTF("HMI Thread started!");
    java_vm->AttachCurrentThread(&java_env_thread_hmi, NULL);

    p_o_hmi_state_machine = &m_o_hmi_state_machine;
}

void HMICyclicThread::run()
{
    /* Continue while there are messages left in the queue */
    HMI::HMIEvent current_hmi_event;
    while (0 < HMI::get_event_queue().get_event(current_hmi_event))
    {
        switch(current_hmi_event.m_event_type)
        {
            case HMI::HMIEvent_PressButton:

                break;
            case HMI::HMIEvent_MenuButtonClick:
                m_o_hmi_state_machine.trigger_transition(static_cast<HMI::HMISMState>(current_hmi_event.m_menu_button_click_id));
                break;
            case HMIEvent_FormOBDValueList_ConfirmSelection:
                /* The user confirmed the OBD data items in the sub menu, return to main view */
                m_o_hmi_state_machine.trigger_transition(HMI_STATE_MAIN_VIEW);
                break;
            default:
                break;
        }
    }

    m_o_hmi_state_machine.process_5ms();
    /* Store data in DPOOL */
    //form_main->update_view();
    //TRACE_PRINTF("Triggered update in main view.");
}

HMIStateMachine& HMICyclicThread::get_state_machine()
{
    return m_o_hmi_state_machine;
}

JNIEnv* HMICyclicThread::get_java_env_thread_hmi() const
{
    return this->java_env_thread_hmi;
}


int HMIEventQueue::enqueue_menu_button_click_event(int menu_button_click_id)
{
    HMIEvent menu_button_event;
    menu_button_event.m_event_type = HMIEvent_MenuButtonClick;
    menu_button_event.m_menu_button_click_id = menu_button_click_id;
    return enqueue_event(menu_button_event);
}


int HMIEventQueue::enqueue_event(const HMIEvent &hmi_event)
{
    std::lock_guard<std::mutex> lock(this->message_queue_mutex);
    this->m_event_queue.push_back(hmi_event);
    return 0;
}

int HMIEventQueue::get_event(HMIEvent& event)
{
    std::lock_guard<std::mutex> lock(this->message_queue_mutex);
    if (this->m_event_queue.size() == 0)
    {
        return 0;
    }
    event = this->m_event_queue.front();
    this->m_event_queue.pop_front();
    return static_cast<int>(this->m_event_queue.size()) + 1;
}

    std::deque<HMIEvent> hmi_message;
    std::mutex message_queue_mutex;



extern "C"
JNIEXPORT void JNICALL
Java_com_texelography_hybridinsight_MainView_android_1hmi_1trigger_1state_1machine(jint new_state)
{
    HMI::get_event_queue().enqueue_menu_button_click_event(1);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_texelography_hybridinsight_OBDValueListView_android_1hmi_1trigger_1state_1machine(jint new_state)
{
    HMI::get_event_queue().enqueue_menu_button_click_event(1);
}



extern "C"
JNIEXPORT void JNICALL
Java_com_texelography_hybridinsight_OBDValueListView_android_1on_1create_1form(JNIEnv *env, jobject obj)
{
    if (p_o_hmi_state_machine != nullptr)
    {
        /* Get a globally valid (among all threads) reference to the object class. */
        jclass clsLocal = env->GetObjectClass(obj);
        jclass java_obd_value_list_view_class = (jclass)env->NewWeakGlobalRef(clsLocal);

        /* Create a globally valid reference to the object itself */
        jobject java_obd_value_list_view_object = env->NewWeakGlobalRef(obj);

        /* create the C++ form with the reference to the Java class */
        p_o_hmi_state_machine->m_p_form_obd_value_list->set_android_object_references(java_obd_value_list_view_class, java_obd_value_list_view_object);
    }
}
