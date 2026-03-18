#!/bin/bash

# Function to ensure the generator JAR exists, building it if necessary
function ensure_jar_exists {
    local PROJECT_ROOT="$1"
    local JAR_PATH="$2"

    if [[ ! -f "$JAR_PATH" ]]; then
        echo "Could not find generator jar at: $JAR_PATH"
        echo "Building with maven..."
        if ! (cd "$PROJECT_ROOT" && mvn clean package -DskipTests); then # Added skipTests to speed up
            echo "Maven build failed - exiting."
            exit 1
        fi
        if [[ ! -f "$JAR_PATH" ]]; then
            echo "Maven build completed but $JAR_PATH still missing - exiting."
            exit 1
        fi
    fi
}

function error {
    echo
    echo "***************************************************************************"
    echo "*                                                                         *"
    echo "*                     DEV ENV Initialization FAILED!                      *"
    echo "*                                                                         *"
    echo "***************************************************************************"
    echo
    exit 1
}
