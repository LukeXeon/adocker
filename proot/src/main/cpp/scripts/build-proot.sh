#!/bin/bash
set -e

# 环境变量: CC, AR, STRIP, STATIC_ROOT, INSTALL_ROOT, PROOT_SOURCE_DIR

cd "$PROOT_SOURCE_DIR/src"

export CFLAGS="-I$STATIC_ROOT/include -Werror=implicit-function-declaration"
export LDFLAGS="-L$STATIC_ROOT/lib -Wl,-z,max-page-size=16384"
export PROOT_UNBUNDLE_LOADER='.'
export PROOT_UNBUNDLE_LOADER_NAME='libproot_loader.so'
export PROOT_UNBUNDLE_LOADER_NAME_32='libproot_loader32.so'

make distclean || true
make V=1 "PREFIX=$INSTALL_ROOT" install

# Strip 和重命名
cd "$INSTALL_ROOT/bin"
for FN in *; do
    "$STRIP" "$FN" 2>/dev/null || true
    case "$FN" in
        lib*.so) ;;
        *) mv -f "$FN" "lib$FN.so" ;;
    esac
done
