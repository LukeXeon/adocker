#!/bin/bash
# Build virglrenderer for Android using meson
# Environment variables required:
#   VIRGL_SOURCE_DIR - path to virglrenderer source
#   CROSS_FILE - path to meson cross file
#   INSTALL_PREFIX - installation prefix
#   PKG_CONFIG_PATH - path to pkg-config files (for epoxy)
#   ARCH - architecture name (for build directory suffix)
#   PYTHON_EXECUTABLE - path to Python interpreter

set -e

# Use meson from virtual environment
MESON="${PYTHON_EXECUTABLE} -m mesonbuild.mesonmain"

cd "$VIRGL_SOURCE_DIR"

# Apply Android compatibility patches for pthread_barrier
# Android Bionic libc doesn't support pthread_barrier_t
THREAD_H="src/mesa/util/u_thread.h"
if grep -q 'HAVE_PTHREAD.*__APPLE__.*__HAIKU__[^_]' "$THREAD_H" 2>/dev/null; then
    echo "Patching u_thread.h for Android pthread_barrier compatibility..."
    # macOS sed requires -i '' for in-place without backup
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' 's/#if defined(HAVE_PTHREAD) && !defined(__APPLE__) && !defined(__HAIKU__)$/#if defined(HAVE_PTHREAD) \&\& !defined(__APPLE__) \&\& !defined(__HAIKU__) \&\& !defined(__ANDROID__)/g' "$THREAD_H"
    else
        sed -i 's/#if defined(HAVE_PTHREAD) && !defined(__APPLE__) && !defined(__HAIKU__)$/#if defined(HAVE_PTHREAD) \&\& !defined(__APPLE__) \&\& !defined(__HAIKU__) \&\& !defined(__ANDROID__)/g' "$THREAD_H"
    fi
fi

BUILD_DIR="builddir-$ARCH"

export PKG_CONFIG_PATH="$INSTALL_PREFIX/lib/pkgconfig:$PKG_CONFIG_PATH"

if [ ! -d "$BUILD_DIR" ]; then
    $MESON setup "$BUILD_DIR" \
        --cross-file="$CROSS_FILE" \
        --prefix="$INSTALL_PREFIX" \
        --default-library=shared \
        --buildtype=release \
        -Dplatforms=egl \
        -Dtests=false \
        -Dvenus=false \
        -Ddrm-renderers=[] \
        -Dvideo=false
fi

$MESON compile -C "$BUILD_DIR"
$MESON install -C "$BUILD_DIR"
