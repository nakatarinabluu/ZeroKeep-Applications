#!/bin/bash
adb logcat -c  # Clear buffer (opsional)
echo "🔍 Watching for ZeroKeepOps logs... (Ctrl+C to stop)"
adb logcat -s "ZeroKeepOps" -v color
