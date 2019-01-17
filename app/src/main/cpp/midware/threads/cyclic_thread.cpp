
/* Foreign header files */
#include "midware/trace/trace.h"

#include <chrono>
#include <functional>   // std::bind

/* Own header files */
#include "midware/threads/cyclic_thread.h"

namespace TaskHelper
{
    void sleep_for(std::chrono::milliseconds milliseconds)
    {
        std::this_thread::sleep_for(milliseconds);
    }
}

Thread::Thread(const std::string &name)
        : name(name), terminate(false), thread_handle(nullptr)
{
}

Thread::~Thread()
{
    if (this->thread_handle != nullptr)
    {
        this->join();
    }
}

int Thread::start()
{
    TRACE_PRINTF("Task " + this->name + " started!");
    this->start_task(4096, 1, [](void* o){ static_cast<Thread*>(o)->run(); });
    return 0;
}

int Thread::start_task(size_t stack_size, size_t priority, void (*fp)(void*))
{
    auto thread_main = std::bind(fp, static_cast<void*>(this));

    this->thread_handle = std::shared_ptr<std::thread>(new std::thread(thread_main));
    return 0;
}


int Thread::stop()
{
    TRACE_PRINTF("Sending thread " + this->name + " to stop!");
    this->terminate = true;
    return 0;
}


int Thread::join()
{
    if (this->terminate == false)
    {
        this->stop();
    }
    this->thread_handle->join();
    this->thread_handle = nullptr;
    this->terminate = false;
    TRACE_PRINTF("Thread " + this->name + " stopped!");
    return 0;
}



int CyclicThread::start()
{
    TRACE_PRINTF("Starting cyclic Thread " + this->name + "...");
    this->start_task(4096, 1, [](void* o){ static_cast<CyclicThread*>(o)->main(); });
    return 0;
}

CyclicThread::CyclicThread(const std::string &name, const std::chrono::milliseconds &interval)
        :Thread(name), interval(interval)
{}

void CyclicThread::main()
{
    TRACE_PRINTF("Cyclic Thread " + this->name + " started!");
    while (this->terminate == false)
    {
        this->run();
        TaskHelper::sleep_for(std::chrono::milliseconds(this->interval));
    }
    TRACE_PRINTF("Cyclic Thread " + this->name + " terminated!");
}


void ThreadRepository::start_all_threads()
{
    for (auto itr = this->m_threads.begin(); itr != this->m_threads.end(); ++itr)
    {
        (*itr)->start();
    }
}

void ThreadRepository::join_all_threads()
{
    TRACE_PRINTF("Thread repository will now join all threads!");
    /* Request all threads to terminate*/
    for (auto itr = this->m_threads.begin(); itr != this->m_threads.end(); ++itr)
    {
        (*itr)->stop();
    }

    /* Wait for all threads to have finished */
    for (auto itr = this->m_threads.begin(); itr != this->m_threads.end(); ++itr)
    {
        (*itr)->join();
    }

}

void ThreadRepository::add_thread(std::shared_ptr<Thread> p_thread)
{
    this->m_threads.push_back(p_thread);
}
