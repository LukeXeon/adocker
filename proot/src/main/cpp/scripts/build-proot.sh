#!/bin/bash
set -e

# 环境变量: CC, AR, STRIP, STATIC_ROOT, INSTALL_ROOT, PROOT_SOURCE_DIR

cd "$PROOT_SOURCE_DIR/src"

export CFLAGS="-I$STATIC_ROOT/include -Werror=implicit-function-declaration"
export LDFLAGS="-L$STATIC_ROOT/lib -Wl,-z,max-page-size=16384"
export PROOT_UNBUNDLE_LOADER='.'
export PROOT_UNBUNDLE_LOADER_NAME='libproot_loader.so'
export PROOT_UNBUNDLE_LOADER_NAME_32='libproot_loader32.so'

# 补丁 GNUmakefile 添加 16KB 页面对齐
if ! grep -q "max-page-size=16384" GNUmakefile 2>/dev/null; then
    sed -i.bak 's/LOADER_LDFLAGS\$1 += -static -nostdlib/LOADER_LDFLAGS\$1 += -static -nostdlib -Wl,-z,max-page-size=16384/' GNUmakefile
fi

make distclean || true

# 过滤功能检测阶段的错误输出（这些错误是预期的，不影响编译）
# .check_process_vm 和 .check_seccomp_filter 在 Android 上不可用
make V=1 "PREFIX=$INSTALL_ROOT" install 2>&1 | grep -v -E "\.check_process_vm|\.check_seccomp_filter|os2_delete\.c"

# Strip 和重命名
cd "$INSTALL_ROOT/bin"
for FN in *; do
    "$STRIP" "$FN" 2>/dev/null || true
    case "$FN" in
        lib*.so) ;;
        *) mv -f "$FN" "lib$FN.so" ;;
    esac
done
