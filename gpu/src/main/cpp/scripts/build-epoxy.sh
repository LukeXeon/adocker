#!/bin/bash
# Build libepoxy for Android using meson
# Environment variables required:
#   EPOXY_SOURCE_DIR - path to libepoxy source
#   CROSS_FILE - path to meson cross file
#   INSTALL_PREFIX - installation prefix
#   ARCH - architecture name (for build directory suffix)
#   PYTHON_EXECUTABLE - path to Python interpreter
#   MESON_VENV_DIR - path to meson virtual environment

set -e

# Use meson from virtual environment
MESON="${PYTHON_EXECUTABLE} -m mesonbuild.mesonmain"

cd "$EPOXY_SOURCE_DIR"

BUILD_DIR="builddir-$ARCH"

if [ ! -d "$BUILD_DIR" ]; then
    $MESON setup "$BUILD_DIR" \
        --cross-file="$CROSS_FILE" \
        --prefix="$INSTALL_PREFIX" \
        --default-library=shared \
        --buildtype=release \
        -Dtests=false \
        -Dx11=false \
        -Degl=yes \
        -Dglx=no
fi

$MESON compile -C "$BUILD_DIR"
$MESON install -C "$BUILD_DIR"
