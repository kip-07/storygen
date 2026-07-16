#!/bin/bash
cd "$(dirname "$0")"

# Kill any existing instance
pkill -f "storygen-0.0.1-SNAPSHOT.jar" 2>/dev/null
sleep 1

# Run the app
java -jar target/storygen-0.0.1-SNAPSHOT.jar

# Cleanup on exit
pkill -f "storygen-0.0.1-SNAPSHOT.jar" 2>/dev/null
