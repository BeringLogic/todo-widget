#!/bin/bash

export QT_QPA_PLATFORM=xcb && export ANDROID_AVD_HOME=$HOME/.android/avd && /opt/android-sdk/emulator/emulator -avd Pixel6_API_34 -gpu swiftshader_indirect
