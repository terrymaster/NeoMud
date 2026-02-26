#!/usr/bin/env bash
# ADB helper script for NeoMud playtester agent
# Handles Windows/MSYS path mangling and provides high-level commands
export MSYS_NO_PATHCONV=1

ADB="/c/Users/lbarnes/AppData/Local/Android/Sdk/platform-tools/adb"

usage() {
    echo "Usage: adb-helper.sh <command> [args...]"
    echo ""
    echo "Commands:"
    echo "  screenshot <output.png>     Capture emulator screen to local file"
    echo "  burst <dir> [count]         Rapid-fire screenshots (default 10) to directory"
    echo "  start-recording [seconds]   Start screen recording (default 180s max)"
    echo "  stop-recording <output.mp4> Stop recording and save to file"
    echo "  ui-dump                     Dump UI hierarchy XML to stdout"
    echo "  tap <x> <y>                 Tap at screen coordinates"
    echo "  text <string>               Type text (spaces handled automatically)"
    echo "  swipe <x1> <y1> <x2> <y2>  Swipe gesture"
    echo "  delete                      Press backspace/delete key"
    echo "  clear-field                 Select all text and delete it"
    echo "  back                        Press back button"
    echo "  enter                       Press enter key"
    echo "  key <KEYCODE>               Send any keyevent (e.g. KEYCODE_TAB)"
    exit 1
}

if [ -z "$1" ]; then
    usage
fi

case "${1}" in
    screenshot)
        if [ -z "$2" ]; then echo "Error: output path required"; exit 1; fi
        OUTPUT="$2"
        $ADB shell screencap -p /sdcard/screenshot.png
        $ADB pull /sdcard/screenshot.png "$OUTPUT" 2>/dev/null
        $ADB shell rm /sdcard/screenshot.png
        echo "$OUTPUT"
        ;;
    start-recording)
        # Start screen recording in background (max 180s by default, or specify duration)
        DURATION="${2:-180}"
        $ADB shell "screenrecord --time-limit $DURATION /sdcard/recording.mp4 &"
        echo "Recording started (max ${DURATION}s). Use 'stop-recording <output.mp4>' to save."
        ;;
    stop-recording)
        if [ -z "$2" ]; then echo "Error: output path required"; exit 1; fi
        OUTPUT="$2"
        # Kill the screenrecord process on device
        $ADB shell "pkill -f screenrecord" 2>/dev/null
        sleep 1
        $ADB pull /sdcard/recording.mp4 "$OUTPUT" 2>/dev/null
        $ADB shell rm /sdcard/recording.mp4
        echo "Recording saved to $OUTPUT"
        ;;
    burst)
        # Rapid-fire screenshots at ~500ms intervals for observing fast gameplay
        if [ -z "$2" ]; then echo "Error: output directory required"; exit 1; fi
        OUTDIR="$2"
        COUNT="${3:-10}"
        mkdir -p "$OUTDIR"
        for i in $(seq 1 "$COUNT"); do
            FNAME="$OUTDIR/burst_$(printf '%03d' $i).png"
            $ADB shell screencap -p /sdcard/screenshot.png
            $ADB pull /sdcard/screenshot.png "$FNAME" 2>/dev/null
            $ADB shell rm /sdcard/screenshot.png
        done
        echo "Captured $COUNT frames to $OUTDIR"
        ;;
    ui-dump)
        $ADB shell uiautomator dump /sdcard/ui.xml 2>/dev/null
        $ADB shell cat /sdcard/ui.xml
        $ADB shell rm /sdcard/ui.xml
        ;;
    tap)
        if [ -z "$2" ] || [ -z "$3" ]; then echo "Error: x and y coordinates required"; exit 1; fi
        $ADB shell input tap "$2" "$3"
        ;;
    text)
        if [ -z "$2" ]; then echo "Error: text string required"; exit 1; fi
        shift
        INPUT="$*"
        # ADB input text uses %s for spaces
        ESCAPED="${INPUT// /%s}"
        $ADB shell input text "$ESCAPED"
        ;;
    swipe)
        if [ -z "$2" ] || [ -z "$3" ] || [ -z "$4" ] || [ -z "$5" ]; then
            echo "Error: x1 y1 x2 y2 coordinates required"; exit 1
        fi
        $ADB shell input swipe "$2" "$3" "$4" "$5"
        ;;
    delete)
        $ADB shell input keyevent KEYCODE_DEL
        ;;
    clear-field)
        # Ctrl+A to select all, then delete
        $ADB shell input keycombination KEYCODE_CTRL_LEFT KEYCODE_A
        $ADB shell input keyevent KEYCODE_DEL
        ;;
    back)
        $ADB shell input keyevent KEYCODE_BACK
        ;;
    enter)
        $ADB shell input keyevent KEYCODE_ENTER
        ;;
    key)
        if [ -z "$2" ]; then echo "Error: keycode required (e.g. KEYCODE_TAB)"; exit 1; fi
        $ADB shell input keyevent "$2"
        ;;
    *)
        usage
        ;;
esac
