# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html
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
# Architecture mapping for meson cross-compilation
# =============================================================================
if (ANDROID_ABI STREQUAL "arm64-v8a")
    set(MESON_CPU_FAMILY "aarch64")
    set(MESON_CPU "aarch64")
    set(NDK_CLANG_PREFIX "aarch64-linux-android")
elseif (ANDROID_ABI STREQUAL "armeabi-v7a")
    set(MESON_CPU_FAMILY "arm")
    set(MESON_CPU "armv7a")
    set(NDK_CLANG_PREFIX "armv7a-linux-androideabi")
elseif (ANDROID_ABI STREQUAL "x86_64")
    set(MESON_CPU_FAMILY "x86_64")
    set(MESON_CPU "x86_64")
    set(NDK_CLANG_PREFIX "x86_64-linux-android")
elseif (ANDROID_ABI STREQUAL "x86")
    set(MESON_CPU_FAMILY "x86")
    set(MESON_CPU "i686")
    set(NDK_CLANG_PREFIX "i686-linux-android")
else ()
    message(FATAL_ERROR "Unsupported Android ABI: ${ANDROID_ABI}")
endif ()

message(STATUS "Building for ${ANDROID_ABI} (meson: ${MESON_CPU_FAMILY}/${MESON_CPU})")

# =============================================================================
# Find Python for meson
# =============================================================================
find_package(Python3 REQUIRED COMPONENTS Interpreter)
message(STATUS "Python3 found: ${Python3_EXECUTABLE}")

# =============================================================================
# Setup paths
# =============================================================================
set(SCRIPTS_DIR "${CMAKE_CURRENT_SOURCE_DIR}/scripts")
set(EPOXY_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/libepoxy")
set(VIRGL_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/virglrenderer")
set(INSTALL_PREFIX "${CMAKE_CURRENT_BINARY_DIR}/install/${ANDROID_ABI}")
set(CROSS_FILE "${CMAKE_CURRENT_BINARY_DIR}/android-cross-${ANDROID_ABI}.txt")
# 顶层共享脚本目录 (gpu/src/main/cpp -> scripts)
get_filename_component(ROOT_SCRIPTS_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../scripts" ABSOLUTE)

# Meson virtual environment (shared across all ABIs to avoid repeated installation)
set(MESON_VENV_DIR "${CMAKE_CURRENT_SOURCE_DIR}/.meson-venv")
set(MESON_VENV_PYTHON "${MESON_VENV_DIR}/bin/python")
set(MESON_STAMP "${MESON_VENV_DIR}/.meson_installed")

# Output library paths
set(EPOXY_OUTPUT "${INSTALL_PREFIX}/lib/libepoxy.so")
set(VIRGL_OUTPUT "${INSTALL_PREFIX}/lib/libvirglrenderer.so")

# =============================================================================
# NDK toolchain paths
# =============================================================================
get_filename_component(TOOLCHAIN_BIN "${CMAKE_C_COMPILER}" DIRECTORY)

# Use NDK architecture-specific clang wrappers (e.g., aarch64-linux-android21-clang)
# These wrappers have built-in --target and --sysroot, avoiding argument parsing issues
# ANDROID_PLATFORM format is "android-21", extract API level number
string(REGEX REPLACE "android-" "" ANDROID_API_LEVEL "${ANDROID_PLATFORM}")
set(NDK_CC "${TOOLCHAIN_BIN}/${NDK_CLANG_PREFIX}${ANDROID_API_LEVEL}-clang")
set(NDK_CXX "${TOOLCHAIN_BIN}/${NDK_CLANG_PREFIX}${ANDROID_API_LEVEL}-clang++")
set(NDK_AR "${TOOLCHAIN_BIN}/llvm-ar")
set(NDK_STRIP "${TOOLCHAIN_BIN}/llvm-strip")
set(PKG_CONFIG_PATH "${INSTALL_PREFIX}/lib/pkgconfig")

# =============================================================================
# Generate meson cross file
# =============================================================================
configure_file(
    "${SCRIPTS_DIR}/android-cross.txt.in"
    "${CROSS_FILE}"
    @ONLY
)

# =============================================================================
# Setup meson virtual environment (runs at configure time)
# =============================================================================
message(STATUS "=== Setting up meson virtual environment ===")

if (NOT EXISTS "${MESON_STAMP}")
    message(STATUS "Creating meson virtual environment...")
    execute_process(
        COMMAND "${Python3_EXECUTABLE}" -m venv "${MESON_VENV_DIR}"
        RESULT_VARIABLE VENV_RESULT
        OUTPUT_VARIABLE VENV_OUTPUT
        ERROR_VARIABLE VENV_ERROR
    )
    if (NOT VENV_RESULT EQUAL 0)
        message(FATAL_ERROR "Failed to create virtual environment: ${VENV_ERROR}")
    endif ()

    message(STATUS "Installing meson, ninja, pkgconf and pyyaml...")
    execute_process(
        COMMAND "${MESON_VENV_PYTHON}" -m pip install --upgrade pip meson ninja pkgconf pyyaml
        RESULT_VARIABLE PIP_RESULT
        OUTPUT_VARIABLE PIP_OUTPUT
        ERROR_VARIABLE PIP_ERROR
    )
    if (NOT PIP_RESULT EQUAL 0)
        message(FATAL_ERROR "Failed to install meson: ${PIP_ERROR}")
    endif ()

    # Create stamp file to indicate successful installation
    file(WRITE "${MESON_STAMP}" "meson installed")
    message(STATUS "Meson setup complete")
else ()
    message(STATUS "Meson virtual environment already exists")
endif ()

# =============================================================================
# Build libepoxy using meson
# =============================================================================
message(STATUS "=== Configuring libepoxy build ===")

add_custom_command(
    OUTPUT "${EPOXY_OUTPUT}"
    COMMENT "Building libepoxy for ${ANDROID_ABI}"
    COMMAND ${CMAKE_COMMAND} -E env
        "EPOXY_SOURCE_DIR=${EPOXY_SOURCE_DIR}"
        "CROSS_FILE=${CROSS_FILE}"
        "INSTALL_PREFIX=${INSTALL_PREFIX}"
        "ARCH=${ANDROID_ABI}"
        "PYTHON_EXECUTABLE=${MESON_VENV_PYTHON}"
        "PATH=${MESON_VENV_DIR}/bin:${ROOT_SCRIPTS_DIR}:$ENV{PATH}"
        bash "${ROOT_SCRIPTS_DIR}/filter-output.sh"
            bash "${SCRIPTS_DIR}/build-epoxy.sh"
    WORKING_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}"
    DEPENDS "${CROSS_FILE}"
    VERBATIM
)

add_custom_target(epoxy_build DEPENDS "${EPOXY_OUTPUT}")

# =============================================================================
# Build virglrenderer using meson (depends on libepoxy)
# =============================================================================
message(STATUS "=== Configuring virglrenderer build ===")

add_custom_command(
    OUTPUT "${VIRGL_OUTPUT}"
    COMMENT "Building virglrenderer for ${ANDROID_ABI}"
    COMMAND ${CMAKE_COMMAND} -E env
        "VIRGL_SOURCE_DIR=${VIRGL_SOURCE_DIR}"
        "CROSS_FILE=${CROSS_FILE}"
        "INSTALL_PREFIX=${INSTALL_PREFIX}"
        "PKG_CONFIG_PATH=${PKG_CONFIG_PATH}"
        "ARCH=${ANDROID_ABI}"
        "PYTHON_EXECUTABLE=${MESON_VENV_PYTHON}"
        "PATH=${MESON_VENV_DIR}/bin:${ROOT_SCRIPTS_DIR}:$ENV{PATH}"
        bash "${ROOT_SCRIPTS_DIR}/filter-output.sh"
            bash "${SCRIPTS_DIR}/build-virglrenderer.sh"
    WORKING_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}"
    DEPENDS "${EPOXY_OUTPUT}" "${CROSS_FILE}"
    VERBATIM
)

add_custom_target(virglrenderer_build DEPENDS "${VIRGL_OUTPUT}")

# =============================================================================
# Import pre-built libraries
# =============================================================================
# Create include directories to avoid CMake configuration errors
# (directories must exist at configure time for INTERFACE_INCLUDE_DIRECTORIES)
file(MAKE_DIRECTORY "${INSTALL_PREFIX}/include")
file(MAKE_DIRECTORY "${INSTALL_PREFIX}/include/virgl")

add_library(epoxy SHARED IMPORTED)
set_target_properties(epoxy PROPERTIES
    IMPORTED_LOCATION "${EPOXY_OUTPUT}"
    INTERFACE_INCLUDE_DIRECTORIES "${INSTALL_PREFIX}/include"
)
add_dependencies(epoxy epoxy_build)

add_library(virglrenderer SHARED IMPORTED)
set_target_properties(virglrenderer PROPERTIES
    IMPORTED_LOCATION "${VIRGL_OUTPUT}"
    INTERFACE_INCLUDE_DIRECTORIES "${INSTALL_PREFIX}/include/virgl"
)
add_dependencies(virglrenderer virglrenderer_build)

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
        ${INSTALL_PREFIX}/include
        ${INSTALL_PREFIX}/include/virgl
)

# Specifies libraries CMake should link to your target library. You
# can link libraries from various origins, such as libraries defined in this
# build script, prebuilt third-party libraries, or Android system libraries.
target_link_libraries(${CMAKE_PROJECT_NAME}
        # GPU rendering libraries (built via meson)
        virglrenderer
        epoxy
        # Android system libraries
        android
        log
        EGL
        GLESv2
)

# =============================================================================
# Copy shared libraries to output directory for APK packaging
# =============================================================================
set(OUTPUT_LIB_DIR "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}")

add_custom_command(TARGET ${CMAKE_PROJECT_NAME} POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        "${EPOXY_OUTPUT}"
        "${OUTPUT_LIB_DIR}/libepoxy.so"
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        "${VIRGL_OUTPUT}"
        "${OUTPUT_LIB_DIR}/libvirglrenderer.so"
    COMMENT "Copying epoxy and virglrenderer libraries to output directory"
)

# Print build summary
message(STATUS "=== GPU project configured ===")
message(STATUS "  Meson venv: ${MESON_VENV_DIR}")
message(STATUS "  Install prefix: ${INSTALL_PREFIX}")
message(STATUS "  libepoxy.so: OpenGL function dispatch library (meson)")
message(STATUS "  libvirglrenderer.so: VRend OpenGL virtual GPU renderer (meson)")
message(STATUS "  lib${CMAKE_PROJECT_NAME}.so: Main application library")
message(STATUS "===============================")
