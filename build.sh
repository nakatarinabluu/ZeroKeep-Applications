#!/bin/bash
./gradlew assembleDebug > build_log_final.txt 2>&1
echo "Build finished with code $?"
