#ifndef _CYCLIC_THREAD_H_
#define _CYCLIC_THREAD_H_

#include <atomic>
#include <chrono>
#include <string>
#include <thread>
#include <memory>
#include <vector>


/* TODO Implement abstraction */

namespace TaskHelper
{
    void sleep_for(std::chrono::milliseconds milliceconds);
    //Thread* //std::this_thread::sleep_for(std::chrono::seconds(1));
}

class Thread
{
public:
    Thread(const std::string &name);

    virtual ~Thread();

    virtual int start();
    int start_task(size_t stack_size, size_t priority, void (*fp)(void*));
    virtual int stop();
    virtual int join();
    virtual void run() = 0;
protected:
    std::string name;
    std::atomic<bool> terminate;
    std::shared_ptr<std::thread> thread_handle;
};


class CyclicThread : public Thread
{
public:
    CyclicThread(const std::string &name, const std::chrono::milliseconds &interval);
    virtual ~CyclicThread() {}
    virtual int start();

private:
    void main();
    std::chrono::milliseconds interval;


};

enum ThreadType
{
    DATA_ACQUISITION_THREAD,
    BLUETOOTH_COMMUNICATION_THREAD
};

class ThreadRepository
{
public:
    void start_all_threads();

    void join_all_threads();

    void add_thread(std::shared_ptr<Thread> thread);
private:

    std::vector<std::shared_ptr<Thread>> m_threads;
};

#endif /* _CYCLIC_THREAD_H_ */
