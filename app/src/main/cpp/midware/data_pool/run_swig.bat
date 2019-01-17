pushd ..\..\..\java\com\texelography\hybridinsight\datapool
del *.* /f /q
popd

swig.exe -c++ -java -package com.texelography.hybridinsight.datapool -outdir ../../../java/com/texelography/hybridinsight/datapool -o data_pool_accessor_java.cpp data_pool.i