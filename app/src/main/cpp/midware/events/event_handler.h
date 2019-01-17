#ifndef _EVENT_HANDLER_H_
#define _EVENT_HANDLER_H_

// tried imitating boost::signals2, couldn't do it.
#include <functional>
#include <vector>
#include <memory>
#include <string>

#include "midware/trace/trace.h"

namespace boost
{
    namespace signals2
    {
        template<typename T>
        class signal
        {
        private:
            typedef std::function< void( T ) > signal_handler_function;

            std::shared_ptr<std::vector<signal_handler_function>> signal_handlers;
            //std::vector<U*> signal_handlers;
        public:
            //void connect(void (*handler)(T))
            signal()
                : signal_handlers(new std::vector<signal_handler_function>())
            {}

            void connect(const signal_handler_function &handler)
            {
                signal_handlers->push_back(handler);
            }

            void operator()(const T &arg) const
            {

                for (auto itr = signal_handlers->begin(); itr != signal_handlers->end(); ++itr)
                {
                    (*itr)(arg);
                }
            }

            /*
            template<typename U>
            void connect(void (*handler)(U, T))
            {


            }*/

        };
    }
}

#endif /* _EVENT_HANDLER_H_ */
