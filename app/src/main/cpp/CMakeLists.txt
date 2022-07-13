# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE ON)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.


set(banuba-lib bnb_effect_player)
add_library(${banuba-lib} INTERFACE)

target_link_libraries(${banuba-lib} INTERFACE
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../libs/lib/${ANDROID_ABI}/libbanuba.so)

target_include_directories(
    ${banuba-lib} INTERFACE
    ${CMAKE_CURRENT_LIST_DIR}/../../../../libs/bnb_sdk)

add_subdirectory(oep)

add_library(
    # Sets the name of the path variable
    native-lib SHARED 
    # Files
    native-lib.cpp effect_player.cpp effect_player.hpp render_context.cpp render_context.hpp)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( 
    # Sets the name of the path variable
    log-lib
    # Specifies the name of the NDK library that
    # you want CMake to locate.
    log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries(
    # Specifies the target library.
    native-lib
    # Links the target library to the log library
    # included in the NDK.
    ${log-lib}
    # below OEP targets
    bnb_oep_pixel_buffer_target
    bnb_oep_image_processing_result_target
    bnb_oep_offscreen_effect_player_target
    bnb_oep_offscreen_render_target_target)

add_library(banuba INTERFACE)

target_link_libraries(
    banuba
    INTERFACE
    "${CMAKE_CURRENT_LIST_DIR}/../../../libs/lib/${ANDROID_ABI}/libBNBEffectPlayerC.so")

target_include_directories(
    banuba
    INTERFACE
    "${CMAKE_CURRENT_LIST_DIR}/../../../libs/include")

# Link with banuba sdk
target_link_libraries(native-lib
    banuba
    # below OEP targets
    bnb_oep_pixel_buffer_target
    bnb_oep_image_processing_result_target
    bnb_oep_offscreen_effect_player_target
    bnb_oep_offscreen_render_target_target
    EGL)
