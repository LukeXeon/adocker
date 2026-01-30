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
