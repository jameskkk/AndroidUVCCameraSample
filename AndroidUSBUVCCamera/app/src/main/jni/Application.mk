# By default, the NDK build system will generate machine code for the
# 'armeabi' ABI. This corresponds to an ARMv5TE based CPU with software
# floating point operations. You can use APP_ABI to select a different
# ABI. 'all' which means "all ABIs supported by this NDK release"
APP_PLATFORM := android-21

# APP_STL := stlport_static    --> static STLport library
# APP_STL := stlport_shared    --> shared STLport library
# APP_STL := system            --> default C++ runtime library
APP_ABI := all
#APP_ABI := armeabi-v7a arm64-v8a x86
#APP_STL := stlport_static
APP_STL := c++_static