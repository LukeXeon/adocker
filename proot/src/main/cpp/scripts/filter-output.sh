#!/bin/bash
# 包装脚本：编译输出重定向到临时文件
# 成功时静默，失败时输出完整日志供调试

TEMP_LOG=$(mktemp)
trap "rm -f '$TEMP_LOG'" EXIT

"$@" > "$TEMP_LOG" 2>&1
EXIT_CODE=$?

if [ $EXIT_CODE -ne 0 ]; then
    cat "$TEMP_LOG"
fi

exit $EXIT_CODE
