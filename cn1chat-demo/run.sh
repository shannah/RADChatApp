#!/bin/bash
MVNW="./mvnw"

function simulator {
  
  "$MVNW" "verify" "-Psimulator" "-DskipTests" "-Dcodename1.platform=javase"
}
function desktop {
  
  "$MVNW" "verify" "-Prun-desktop" "-DskipTests" "-Dcodename1.platform=javase"
}
function settings {
  
  "$MVNW" "cn:settings"
}
function update {
  
  "$MVNW" "cn:update"
}
function help {
  "echo" "-e" "run.sh [COMMAND]"
  "echo" "-e" "Commands:"
  "echo" "-e" "  simulator"
  "echo" "-e" "    Runs app using Codename One Simulator"
  "echo" "-e" "  desktop"
  "echo" "-e" "    Runs app as a desktop app."
  "echo" "-e" "  settings"
  "echo" "-e" "    Opens Codename One settings"
  "echo" "-e" "  update"
  "echo" "-e" "    Update Codename One libraries"
}
CMD=$1

if [ "$CMD" == "" ]; then
  CMD="simulator"
fi
"$CMD" 