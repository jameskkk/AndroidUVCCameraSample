# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)
#set(opencv_sdk "${CMAKE_CURRENT_SOURCE_DIR}/../OpenCV-android-sdk/sdk/")
#set(libs "${opencv_sdk}/native/libs/")
#set(3rdparty_libs "${opencv_sdk}/native/3rdparty/libs/")

include_directories(${CMAKE_SOURCE_DIR}/src/main/cpp/include)
#include_directories(${opencv_sdk}/native/jni/include)

#add_library(libopencv_java3 SHARED IMPORTED )
#set_target_properties(libopencv_java3 PROPERTIES
#IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_java3.so")




# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_subdirectory(src/main/cpp/libjpeg-turbo-1.5.0)

include_directories(src/main/cpp/libusb/libusb
                    src/main/cpp/libusb/libusb/os
                    src/main/cpp/libusb/android
                    src/main/cpp/libusb
                    src/main/cpp)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DACCESS_RAW_DESCRIPTORS -fstrict-aliasing -fprefetch-loop-arrays")

add_library( usb100
             SHARED
             src/main/cpp/libusb/libusb/core.c
             src/main/cpp/libusb/libusb/descriptor.c
             src/main/cpp/libusb/libusb/hotplug.c
             src/main/cpp/libusb/libusb/io.c
             src/main/cpp/libusb/libusb/sync.c
             src/main/cpp/libusb/libusb/strerror.c
             src/main/cpp/libusb/libusb/os/android_usbfs.c
             src/main/cpp/libusb/libusb/os/poll_posix.c
             src/main/cpp/libusb/libusb/os/threads_posix.c
             src/main/cpp/libusb/libusb/os/android_netlink.c)


include_directories(src/main/cpp/libuvc/include
                    src/main/cpp/libuvc/include/libuvc
                    src/main/cpp
                    src/main/cpp/libjpeg-turbo-1.5.0
                    src/main/cpp/libjpeg-turbo-1.5.0/include)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DUVC_DEBUGGING")

add_library( uvc
             SHARED
             src/main/cpp/libuvc/src/ctrl.c
             src/main/cpp/libuvc/src/device.c
             src/main/cpp/libuvc/src/diag.c
             src/main/cpp/libuvc/src/frame.c
             src/main/cpp/libuvc/src/frame-mjpeg.c
             src/main/cpp/libuvc/src/init.c
             src/main/cpp/libuvc/src/stream.c)

include_directories(src/main/cpp/UVCCamera
                    src/main/cpp/rapidjson/include
                    src/main/cpp)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DACCESS_RAW_DESCRIPTORS -fstrict-aliasing -fprefetch-loop-arrays")

add_library( UVCCamera
             SHARED
             src/main/cpp/UVCCamera/_onload.cpp
             src/main/cpp/UVCCamera/utilbase.cpp
             src/main/cpp/UVCCamera/UVCCamera.cpp
             src/main/cpp/UVCCamera/UVCPreview.cpp
             src/main/cpp/UVCCamera/UVCButtonCallback.cpp
             src/main/cpp/UVCCamera/UVCStatusCallback.cpp
             src/main/cpp/UVCCamera/Parameters.cpp
             src/main/cpp/UVCCamera/serenegiant_usb_UVCCamera.cpp)
#             src/main/cpp/UVCCamera/UVCCamera_jni.cpp)

#add_library(samplecode
#            SHARED
#            src/main/cpp/native-lib.cpp)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )
find_library( android-lib android )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       usb100

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib})

#target_link_libraries(uvc libopencv_java3)

target_link_libraries(uvc usb100 jpeg-turbo1500 ${log-lib})

target_link_libraries(UVCCamera uvc ${log-lib} ${android-lib})

#target_link_libraries(samplecode uvc)





