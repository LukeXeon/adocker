# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
# Since this is the top level CMakeLists.txt, the project name is also accessible
# with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
# build script scope).
project("virgl_jni")

# Android 15+ 16KB page size support
# Required for devices like Pixel 9 with 16KB memory pages
# This is backward compatible with 4KB page devices
if (ANDROID)
    set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")
    message(STATUS "Android 16KB page alignment enabled")
endif ()

# =============================================================================
# Build libepoxy (OpenGL dispatch library - dependency for virglrenderer)
# =============================================================================
message(STATUS "=== Building libepoxy ===")
add_subdirectory(libepoxy)

# =============================================================================
# Build virglrenderer (Virtual GPU renderer - depends on libepoxy)
# =============================================================================
message(STATUS "=== Building virglrenderer ===")
add_subdirectory(virglrenderer)

# =============================================================================
# Main GPU library
# =============================================================================
# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
#
# In this top level CMakeLists.txt, ${CMAKE_PROJECT_NAME} is used to define
# the target library name; in the sub-module's CMakeLists.txt, ${PROJECT_NAME}
# is preferred for the same purpose.
#
# In order to load a library into your app from Java/Kotlin, you must call
# System.loadLibrary() and pass the name of the library defined here;
# for GameActivity/NativeActivity derived applications, the same library name must be
# used in the AndroidManifest.xml file.
add_library(${CMAKE_PROJECT_NAME} SHARED
        # List C/C++ source files with relative paths to this CMakeLists.txt.
        virgl_jni.cpp)

# Include directories for virglrenderer and libepoxy headers
target_include_directories(${CMAKE_PROJECT_NAME}
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/virglrenderer/src
        ${CMAKE_CURRENT_SOURCE_DIR}/libepoxy/include
        ${CMAKE_CURRENT_BINARY_DIR}/virglrenderer  # For generated virgl-version.h
        ${CMAKE_CURRENT_BINARY_DIR}/libepoxy/include  # For generated epoxy headers
)

# Specifies libraries CMake should link to your target library. You
# can link libraries from various origins, such as libraries defined in this
# build script, prebuilt third-party libraries, or Android system libraries.
target_link_libraries(${CMAKE_PROJECT_NAME}
        # GPU rendering libraries (built as subprojects)
        virglrenderer
        epoxy
        # Android system libraries
        android
        log
        EGL
        GLESv2
)

# Print build summary
message(STATUS "=== GPU project configured ===")
message(STATUS "  libepoxy.so: OpenGL function dispatch library")
message(STATUS "  libvirglrenderer.so: VRend OpenGL virtual GPU renderer")
message(STATUS "  libgpu.so: Main application library")
message(STATUS "===============================")