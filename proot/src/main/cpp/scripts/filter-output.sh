#!/bin/bash
# 包装脚本：将 stderr 合并到 stdout
# 避免 IDE 将 stderr 输出识别为编译错误

"$@" 2>&1
exit $?
