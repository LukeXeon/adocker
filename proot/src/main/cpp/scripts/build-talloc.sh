#!/bin/bash
set -e

# 需要的环境变量: CC, AR, RANLIB, STATIC_ROOT, TALLOC_SOURCE_DIR, PYTHON

cd "$TALLOC_SOURCE_DIR"

# 创建 cross-answers.txt
cat <<EOF >cross-answers.txt
Checking uname sysname type: "Linux"
Checking uname machine type: "dontcare"
Checking uname release type: "dontcare"
Checking uname version type: "dontcare"
Checking simple C program: OK
rpath library support: OK
-Wl,--version-script support: FAIL
Checking getconf LFS_CFLAGS: OK
Checking for large file support without additional flags: OK
Checking for -D_FILE_OFFSET_BITS=64: OK
Checking for -D_LARGE_FILES: OK
Checking correct behavior of strtoll: OK
Checking for working strptime: OK
Checking for C99 vsnprintf: OK
Checking for HAVE_SHARED_MMAP: OK
Checking for HAVE_MREMAP: OK
Checking for HAVE_INCOHERENT_MMAP: OK
Checking for HAVE_SECURE_MKSTEMP: OK
Checking getconf large file support flags work: OK
Checking for HAVE_IFACE_IFCONF: FAIL
EOF

./configure build "--prefix=$STATIC_ROOT" \
    --disable-rpath --disable-python \
    --cross-compile --cross-answers=cross-answers.txt || true

mkdir -p "$STATIC_ROOT/include" "$STATIC_ROOT/lib"
"$AR" rcs "$STATIC_ROOT/lib/libtalloc.a" bin/default/talloc*.o
cp -f talloc.h "$STATIC_ROOT/include"
