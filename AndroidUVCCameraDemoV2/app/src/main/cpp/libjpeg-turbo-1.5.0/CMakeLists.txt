# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

#if(NOT DEFINED ARMASM)
#  set(ARMASM ${NDKPATH}/toolchains/arm-linux-androideabi-4.9/prebuilt/windows-x86_64/bin/arm-linux-androideabi-as
#             CACHE FILEPATH "Path to ARMASM executable")
#endif()

include_directories(include
                    smid
                    ./)

enable_language(ASM)

if(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(SIMD_C_SRC simd/jsimd_arm.c)
    set(SIMD_SRC simd/jsimd_arm_neon.S)
    set(SIMD_OBJ jsimd_arm.o)
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DSIZEOF_SIZE_T=4")
endif()
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(SIMD_C_SRC simd/jsimd_arm64.c)
    set(SIMD_SRC simd/jsimd_arm64_neon.S)
    set(SIMD_OBJ jsimd_arm64.o)
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DSIZEOF_SIZE_T=8")
endif()

add_custom_command(OUTPUT ${SIMD_OBJ}
        DEPENDS ${SIMD_SRC} ${DEPFILE} ${INC_FILES}
        COMMAND ${ARMASM} ${NAFLAGS} ${SIMD_SRC} -o${SIMD_OBJ})

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DELF")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-incompatible-pointer-types")

add_library( jpeg-turbo1500
             SHARED
             jcapimin.c
             jcapistd.c
             jccoefct.c
             jccolor.c
             jcdctmgr.c
             jchuff.c
             jcinit.c
             jcmainct.c
             jcmarker.c
             jcmaster.c
             jcomapi.c
             jcparam.c
             jcphuff.c
             jcprepct.c
             jcsample.c
             jctrans.c
             jdapimin.c
             jdapistd.c
             jdatadst.c
             jdatasrc.c
             jdcoefct.c
             jdcolor.c
             jddctmgr.c
             jdhuff.c
             jdinput.c
             jdmainct.c
             jdmarker.c
             jdmaster.c
             jdmerge.c
             jdphuff.c
             jdpostct.c
             jdsample.c
             jdtrans.c
             jerror.c
             jfdctflt.c
             jfdctfst.c
             jfdctint.c
             jidctflt.c
             jidctfst.c
             jidctint.c
             jidctred.c
             jquant1.c
             jquant2.c
             jutils.c
             jmemmgr.c
             jmemnobs.c
             jaricom.c
             jcarith.c
             jdarith.c
             turbojpeg.c
             transupp.c
             jdatadst-tj.c
             jdatasrc-tj.c
             ${SIMD_C_SRC}
             ${SIMD_SRC})


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

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       jpeg-turbo1500

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib})