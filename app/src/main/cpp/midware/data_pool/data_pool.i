/* File : example.i */
%module data_pool

%include "std_string.i"
%{
#include "data_pool_accessor.hpp"
%}

/* Let's just grab the original header file here */
%include "data_pool_accessor.hpp"