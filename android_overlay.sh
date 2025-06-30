#!/bin/bash

LOWER=/opt/android-sdk/
UPPER="$HOME/.local/android/.sdk/upper"
WORK="$HOME/.local/android/.sdk/work"
ANDROID_HOME="$HOME/.local/android/sdk"

mkdir -p "$LOWER" "$UPPER" "$WORK" "$ANDROID_HOME"

fuse-overlayfs -o squash_to_uid="$(id -u)",squash_to_gid="$(id -g)",lowerdir=$LOWER,upperdir="$UPPER",workdir="$WORK" "$ANDROID_HOME"

export ANDROID_HOME

