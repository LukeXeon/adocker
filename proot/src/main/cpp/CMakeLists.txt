cmake_minimum_required(VERSION 3.22.1)

# ============ 配置 C++ STL ============
# 必须在 project() 之前设置

project("proot")

# ============ 配置 ============
set(TALLOC_VERSION "2.4.2")

# 架构映射
if(CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
    set(PROOT_ARCH "aarch64")
    set(HAS_LOADER_32BIT TRUE)
    set(NDK_CLANG_PREFIX "aarch64-linux-android")
elseif(CMAKE_ANDROID_ARCH_ABI STREQUAL "armeabi-v7a")
    set(PROOT_ARCH "armv7a")
    set(HAS_LOADER_32BIT FALSE)
    set(NDK_CLANG_PREFIX "armv7a-linux-androideabi")
elseif(CMAKE_ANDROID_ARCH_ABI STREQUAL "x86_64")
    set(PROOT_ARCH "x86_64")
    set(HAS_LOADER_32BIT TRUE)
    set(NDK_CLANG_PREFIX "x86_64-linux-android")
elseif(CMAKE_ANDROID_ARCH_ABI STREQUAL "x86")
    set(PROOT_ARCH "i686")
    set(HAS_LOADER_32BIT FALSE)
    set(NDK_CLANG_PREFIX "i686-linux-android")
else()
    message(FATAL_ERROR "Unsupported ABI: ${CMAKE_ANDROID_ARCH_ABI}")
endif()

# ============ 目录 ============
set(PROOT_BUILD_DIR "${CMAKE_CURRENT_BINARY_DIR}/proot-build")
set(PROOT_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/proot")
set(TALLOC_SOURCE_DIR "${PROOT_BUILD_DIR}/talloc-${TALLOC_VERSION}")
set(STATIC_ROOT "${PROOT_BUILD_DIR}/static-${PROOT_ARCH}")
set(INSTALL_ROOT "${PROOT_BUILD_DIR}/install-${PROOT_ARCH}")
set(SCRIPTS_DIR "${CMAKE_CURRENT_SOURCE_DIR}/scripts")

# ============ 下载 talloc ============
set(TALLOC_URL "https://download.samba.org/pub/talloc/talloc-${TALLOC_VERSION}.tar.gz")

file(MAKE_DIRECTORY ${PROOT_BUILD_DIR})

if(NOT EXISTS "${TALLOC_SOURCE_DIR}")
    message(STATUS "Downloading talloc-${TALLOC_VERSION}...")
    file(DOWNLOAD ${TALLOC_URL} "${PROOT_BUILD_DIR}/talloc.tar.gz" SHOW_PROGRESS)
    execute_process(
        COMMAND ${CMAKE_COMMAND} -E tar xzf talloc.tar.gz
        WORKING_DIRECTORY ${PROOT_BUILD_DIR}
    )
endif()

# ============ Python ============
find_package(Python3 COMPONENTS Interpreter REQUIRED)

# ============ NDK 工具链 ============
get_filename_component(TOOLCHAIN_BIN "${CMAKE_C_COMPILER}" DIRECTORY)
set(NDK_AR "${TOOLCHAIN_BIN}/llvm-ar")
set(NDK_RANLIB "${TOOLCHAIN_BIN}/llvm-ranlib")
set(NDK_STRIP "${TOOLCHAIN_BIN}/llvm-strip")

# 使用 NDK 的架构特定 clang wrapper（如 aarch64-linux-android21-clang）
# 这些 wrapper 已经内置了 --target 和 --sysroot 参数，避免了空格导致的参数解析问题
# ANDROID_PLATFORM 格式为 "android-21"，需要提取 API level 数字
string(REGEX REPLACE "android-" "" ANDROID_API_LEVEL "${ANDROID_PLATFORM}")
set(NDK_CC "${TOOLCHAIN_BIN}/${NDK_CLANG_PREFIX}${ANDROID_API_LEVEL}-clang")

# ============ 编译 talloc ============
set(TALLOC_LIB "${STATIC_ROOT}/lib/libtalloc.a")

add_custom_command(
    OUTPUT ${TALLOC_LIB}
    COMMENT "Building talloc for ${PROOT_ARCH}"
    COMMAND bash ${SCRIPTS_DIR}/filter-output.sh
        ${CMAKE_COMMAND} -E env
        "CC=${NDK_CC}"
        "AR=${NDK_AR}"
        "RANLIB=${NDK_RANLIB}"
        "STATIC_ROOT=${STATIC_ROOT}"
        "TALLOC_SOURCE_DIR=${TALLOC_SOURCE_DIR}"
        "PYTHON=${Python3_EXECUTABLE}"
        "PATH=${SCRIPTS_DIR}:$ENV{PATH}"
        bash ${SCRIPTS_DIR}/build-talloc.sh
    WORKING_DIRECTORY ${TALLOC_SOURCE_DIR}
    VERBATIM
)

add_custom_target(talloc_static DEPENDS ${TALLOC_LIB})

# ============ 编译 proot ============
set(PROOT_OUTPUT "${INSTALL_ROOT}/bin/libproot.so")
set(LOADER_OUTPUT "${INSTALL_ROOT}/bin/libproot_loader.so")

add_custom_command(
    OUTPUT ${PROOT_OUTPUT} ${LOADER_OUTPUT}
    COMMENT "Building proot for ${PROOT_ARCH}"
    COMMAND bash ${SCRIPTS_DIR}/filter-output.sh
        ${CMAKE_COMMAND} -E env
        "CC=${NDK_CC}"
        "AR=${NDK_AR}"
        "STRIP=${NDK_STRIP}"
        "STATIC_ROOT=${STATIC_ROOT}"
        "INSTALL_ROOT=${INSTALL_ROOT}"
        "PROOT_SOURCE_DIR=${PROOT_SOURCE_DIR}"
        "PATH=${SCRIPTS_DIR}:$ENV{PATH}"
        bash ${SCRIPTS_DIR}/build-proot.sh
    WORKING_DIRECTORY ${PROOT_SOURCE_DIR}/src
    DEPENDS talloc_static
    VERBATIM
)

add_custom_target(proot_build ALL DEPENDS ${PROOT_OUTPUT} ${LOADER_OUTPUT})

# ============ 复制到输出目录 ============
set(OUTPUT_DIR "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}")

add_custom_command(TARGET proot_build POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy ${PROOT_OUTPUT} ${OUTPUT_DIR}/libproot.so
    COMMAND ${CMAKE_COMMAND} -E copy ${LOADER_OUTPUT} ${OUTPUT_DIR}/libproot_loader.so
    COMMENT "Copying proot binaries to ${OUTPUT_DIR}"
)

if(HAS_LOADER_32BIT)
    add_custom_command(TARGET proot_build POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy
            ${INSTALL_ROOT}/bin/libproot_loader32.so
            ${OUTPUT_DIR}/libproot_loader32.so
        COMMENT "Copying loader32 to ${OUTPUT_DIR}"
    )
endif()

# ============ 创建 proot_ext 库 ============
add_library(proot_ext SHARED ext.cpp)
add_dependencies(proot_ext proot_build)
target_include_directories(proot_ext PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
# 16KB 页面对齐 (Android 15+ 兼容性)
target_link_options(proot_ext PRIVATE -Wl,-z,max-page-size=16384)
