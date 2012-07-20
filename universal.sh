#!/bin/sh

# Copyright © 2003-2012 RAAF Technology bv
#
# This file is part of Universal.
#
# Universal is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Universal is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with Universal.  If not, see <http://www.gnu.org/licenses/>.  


# Determine where $UNIVERSAL_HOME is.
if [ ! "$UNIVERSAL_HOME" ]; then
    ABS="$PWD"
    IFS="/"
    for DIR in $0; do
        if [ -n "$DIR" ]; then
            if [ "$DIR" = ".." ]; then
                ABS="${ABS%/*}"
            elif [ "$DIR" != "." ]; then
                ABS="$ABS/$DIR"
            fi;
        fi;
    done;
    unset DIR
    unset IFS
    OFFSET=""
    if [ -e "`dirname "$ABS"`$OFFSET" ]; then
        # Found, probably called relatively.
        cd "`dirname "$ABS"`$OFFSET"
        UNIVERSAL_HOME="`pwd`"
        unset ABS
    elif [ -e "`dirname "$0"`$OFFSET" ]; then
        # Found, probably called absolutely.
        cd "`dirname "$0"`$OFFSET"
        UNIVERSAL_HOME="`pwd`"
    fi;
fi;

# Show version.
if [ "$1" = "version" ]; then
    echo "RELEASE.STRING.VERSION
    RELEASE.STRING.COPYRIGHT
    RELEASE.STRING.RELDATE
    RELEASE.STRING.BUILT
    RELEASE.STRING.LICENSE" | sed 's/^[ \t]*//;s/[ \t]*$//'
    exit 0
fi

# We're running globally, calling other subsystems.
GLOBAL_RUN=1

# Include global setup, environment and functions.
. $UNIVERSAL_HOME/Includes/environment.sh
. $UNIVERSAL_HOME/Includes/functions.sh


# Make sure our target directories exist.
if [ ! -e "$TARGET_ROOT" ]; then
    mkdir -p "$TARGET_ROOT"
fi;
if [ ! -e "$TARGET_LOGS" ]; then
    mkdir -p "$TARGET_LOGS"
fi;

# Main case statement.
case "$1" in
    build)
    # Check if our local system environment is sane.
    if [ "$SANITY_DO_CONFIGURATION" = 1 ]; then
        "$UNIVERSAL_HOME"/Tools/sanity/run.sh
    fi;

    # Set up networking.
    if [ "$NETWORKING_DO_CONFIGURATION" = 1 ]; then
        "$UNIVERSAL_HOME"/Tools/networking/run.sh $1
    fi;

    # Set up packaging.
    if [ "$PACKAGING_DO_CONFIGURATION" = 1 ]; then
        "$UNIVERSAL_HOME"/Tools/packaging/run.sh $1
    fi;

    # Create certificates for server and defined aliases.
    if [ "$CERTIFICATES_DO_CONFIGURATION" = 1 ]; then
        "$UNIVERSAL_HOME"/Tools/certificates/run.sh $1
    fi;

    # Create custom utilities that aid in administration.
    if [ "$UTILITIES_DO_CONFIGURATION" = 1 ]; then
        "$UNIVERSAL_HOME"/Tools/utilities/run.sh $1
    fi;

    # Enumerate requested services from role.
    echo "Setting up services for the requested role:"
    for SERVICE in $SERVICES; do
        echo -n " * $SERVICE: "
        $UNIVERSAL_HOME/Services/$SERVICE/run.sh $1 \
          >> $TARGET_LOGS/$SERVICE.log 2>&1 < /dev/null
        
        # Check exit code.
        if [ "$?" = 0 ]; then
            echo "success"
        else
            STATE="fail"
            echo "$STATE"
        fi;
    done;
    echo ""
    ;;

    clean)
    # Clean up created networking config.
    if [ "$NETWORKING_DO_CONFIGURATION" = 1 ]; then
        "$UNIVERSAL_HOME"/Tools/networking/run.sh $1
    fi;

    # Clean up created packaging config.
    if [ "$PACKAGING_DO_CONFIGURATION" = 1 ]; then
        "$UNIVERSAL_HOME"/Tools/packaging/run.sh $1
    fi;

    # Clean certificates for server and defined aliases.
    if [ "$SANITY_DO_CONFIGURATION" = 1 ]; then
        "$UNIVERSAL_HOME"/Tools/certificates/run.sh $1
    fi;

    # Clean up custom utilities from target directory.
    if [ $UTILITIES_DO_CONFIGURATION = 1 ]; then
        "$UNIVERSAL_HOME"/Tools/utilities/run.sh $1
    fi;

    # Enumerate requested services from role.
    echo "Cleaning up services for the requested role:"
    for SERVICE in $SERVICES; do
        echo -n " * $SERVICE: "
        $UNIVERSAL_HOME/Services/$SERVICE/run.sh $1 \
          >> $TARGET_LOGS/$SERVICE.log 2>&1 < /dev/null
        
        # Check exit code.
        if [ "$?" = 0 ]; then
            echo "success"
        else
            STATE="fail"
            echo "$STATE"
        fi;
    done;
    echo ""
    ;;

    install)
    # Install created networking config.
    if [ "$NETWORKING_DO_CONFIGURATION" = 1 ]; then
        "$UNIVERSAL_HOME"/Tools/networking/run.sh $1
    fi;

    # Install created packaging config.
    if [ "$PACKAGING_DO_CONFIGURATION" = 1 ]; then
        "$UNIVERSAL_HOME"/Tools/packaging/run.sh $1
    fi;

    # Install certificates for server and defined aliases.
    if [ "$SANITY_DO_CONFIGURATION" = 1 ]; then
        "$UNIVERSAL_HOME"/Tools/certificates/run.sh $1
    fi;

    # Install custom utilities to host.
    if [ "$UTILITIES_DO_CONFIGURATION" = 1 ]; then
        "$UNIVERSAL_HOME"/Tools/utilities/run.sh $1
    fi;

    # Enumerate requested services from role.
    echo "Installing services for the requested role:"
    for SERVICE in $SERVICES; do
        echo -n " * $SERVICE: "
        $UNIVERSAL_HOME/Services/$SERVICE/run.sh $1 \
          >> $TARGET_LOGS/$SERVICE.log 2>&1 < /dev/null
        
        # Check exit code.
        if [ "$?" = 0 ]; then
            echo "success"
        else
            STATE="fail"
            echo "$STATE"
        fi;
    done;
    echo ""
    ;;

    uninstall)
    # Check if our local system environment is sane.
    if [ "$SANITY_DO_CONFIGURATION" = 1 ]; then
        "$UNIVERSAL_HOME"/Tools/sanity/run.sh
    fi;

    # Uninstall networking from host.
    if [ "$NETWORKING_DO_CONFIGURATION" = 1 ]; then
        "$UNIVERSAL_HOME"/Tools/networking/run.sh $1
    fi;

    # Uninstall packaging config from host.
    if [ "$PACKAGING_DO_CONFIGURATION" = 1 ]; then
        "$UNIVERSAL_HOME"/Tools/packaging/run.sh $1
    fi;

    # Remove installed certificates from host.
    if [ "$CERTIFICATES_DO_CONFIGURATION" = 1 ]; then
        "$UNIVERSAL_HOME"/Tools/certificates/run.sh $1
    fi;

    # Remove custom utilities from host.
    if [ "$UTILITIES_DO_CONFIGURATION" = 1 ]; then
        "$UNIVERSAL_HOME"/Tools/utilities/run.sh $1
    fi;

    # Enumerate requested services from role.
    echo "Uninstalling services for the requested role:"
    for SERVICE in $SERVICES; do
        echo -n " * $SERVICE: "
        $UNIVERSAL_HOME/Services/$SERVICE/run.sh $1 \
          >> $TARGET_LOGS/$SERVICE.log 2>&1 < /dev/null

        # Check exit code.
        if [ "$?" = 0 ]; then
            echo "success"
        else
            STATE="fail"
            echo "$STATE"
        fi;
    done;
    ;;
    
    start)
    # Enumerate requested services from role.
    echo "Configuring boot-time parameters and starting up services:"
    for SERVICE in $SERVICES; do
        echo -n " * $SERVICE: "
        $UNIVERSAL_HOME/Services/$SERVICE/run.sh $1 \
        >> $TARGET_LOGS/$SERVICE.log 2>&1 < /dev/null

        # Check exit code.
        if [ "$?" = 0 ]; then
            echo "success"
        else
            STATE="fail"
            echo "$STATE"
        fi;
    done;
    ;;
    
    stop)
    # Enumerate requested services from role.
    echo "Unconfiguring boot-time parameters and stopping services:"
    for SERVICE in $SERVICES; do
        echo -n " * $SERVICE: "
        $UNIVERSAL_HOME/Services/$SERVICE/run.sh $1 \
        >> $TARGET_LOGS/$SERVICE.log 2>&1 < /dev/null

        # Check exit code.
        if [ "$?" = 0 ]; then
            echo "success"
        else
            STATE="fail"
            echo "$STATE"
        fi;
    done;
    ;;

    state)
    echo "Showing state from tools:"
    # Show state of networking configuration.
    if [ "$NETWORKING_DO_CONFIGURATION" = 1 ]; then
        echo " * networking: `$UNIVERSAL_HOME/Tools/networking/run.sh $1`"
    fi;

    # Show state of packaging configuration.
    if [ "$PACKAGING_DO_CONFIGURATION" = 1 ]; then
        echo " * packaging: `$UNIVERSAL_HOME/Tools/packaging/run.sh $1`"
    fi;

    # Show state of certificate generator.
    if [ "$SANITY_DO_CONFIGURATION" = 1 ]; then
        echo " * certificates: `$UNIVERSAL_HOME/Tools/certificates/run.sh $1`"
    fi;

    # Show state of utility generator.
    if [ "$UTILITIES_DO_CONFIGURATION" = 1 ]; then
        echo " * utilities: `$UNIVERSAL_HOME/Tools/utilities/run.sh $1`"
    fi;
    echo ""

    # Enumerate requested services from role.
    echo "Showing state from included services:"
    for SERVICE in $SERVICES; do
        echo " * $SERVICE: `$UNIVERSAL_HOME/Services/$SERVICE/run.sh $1`"
    done;
    ;;

    deps)
    # Show external software dependencies for creating the network setup.
    if [ "$NETWORKING_DO_CONFIGURATION" = 1 ]; then
        "$UNIVERSAL_HOME"/Tools/networking/run.sh $1
    fi;

    # Show external software dependencies for creating the packaging config.
    if [ "$PACKAGING_DO_CONFIGURATION" = 1 ]; then
        "$UNIVERSAL_HOME"/Tools/packaging/run.sh $1
    fi;

    # Show external software dependencies for creating certificates.
    if [ "$SANITY_DO_CONFIGURATION" = 1 ]; then
        "$UNIVERSAL_HOME"/Tools/certificates/run.sh $1
    fi;

    # Show external software dependencies for the generated utilities.
    if [ "$UTILITIES_DO_CONFIGURATION" = 1 ]; then
        "$UNIVERSAL_HOME"/Tools/utilities/run.sh $1
    fi;

    # Enumerate requested services from role.
    for SERVICE in $SERVICES; do
        "$UNIVERSAL_HOME"/Services/$SERVICE/run.sh $1
    done;
    ;;

    debug)
    set
    ;;
    *)
    echo $"Usage: $0 {build|clean|install|uninstall|start|stop|state|deps|version}"
    exit 1
esac
