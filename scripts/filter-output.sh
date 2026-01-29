#!/bin/bash
# 包装脚本：编译输出重定向到构建目录的日志文件
# 成功时静默，失败时输出完整日志供调试
# 日志文件保留不删除，路径会打印到控制台

BUILD_DIR="${PWD}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="${BUILD_DIR}/build_${TIMESTAMP}_$$.log"

echo "LOG_FILE: ${LOG_FILE}"

"$@" > "$LOG_FILE" 2>&1
EXIT_CODE=$?

if [ $EXIT_CODE -ne 0 ]; then
    cat "$LOG_FILE"
fi

exit $EXIT_CODE
