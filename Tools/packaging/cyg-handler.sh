#!/bin/sh

# Copyright © 2003, 2004, 2005, 2008, 2009 Rubin Simons
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
if [ ! $UNIVERSAL_HOME ]; then
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
    OFFSET="/../../"
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

# Include global setup, environment and functions.
if [ ! $GLOBAL_RUN ]; then
    . $UNIVERSAL_HOME/Includes/environment.sh
    . $UNIVERSAL_HOME/Includes/functions.sh
fi;

# Include state for this specific module.
if [ -e $TARGET_STATE/packaging.state ]; then
    . $TARGET_STATE/packaging.state
else
    mkdir -p $TARGET_STATE
    echo "SERVICE_STATE=uninitialized" > $TARGET_STATE/packaging.state
    . $TARGET_STATE/packaging.state
fi;

# Set local variables.
CYG_PLATFORM_NAME=$2
CYG_REPOSITORY_NAME=$3


# Main case statement.
case "$1" in
    build)
    cd $UNIVERSAL_HOME/Tools/packaging
    mkdir -p $TARGET_ROOT/bin
    wget -q http://apt-cyg.googlecode.com/svn/trunk/apt-cyg -O $TARGET_ROOT/bin/apt-cyg
    chmod 755 $TARGET_ROOT/bin/apt-cyg

    # Check the state of the last command.
    if [ $? = 0 ]; then
        echo "SERVICE_STATE=built" > $TARGET_STATE/packaging.state
    else
        echo "SERVICE_STATE=unknown" > $TARGET_STATE/packaging.state
    fi;
    ;;

    clean)
    cd $UNIVERSAL_HOME/Tools/packaging
    rm  -f $TARGET_ROOT/bin/apt-cyg
    if [ -e $TARGET_ROOT/bin ]; then rmdir $TARGET_ROOT/bin; fi;

    # Check the state of the last command.
    if [ $? = 0 ]; then
        echo "SERVICE_STATE=cleaned" > $TARGET_STATE/packaging.state
    else
        echo "SERVICE_STATE=unknown" > $TARGET_STATE/packaging.state
    fi;
    ;;
    
    install)
    mkdir -p /bin
    cp $TARGET_ROOT/bin/apt-cyg /bin/apt-cyg

    # Check the state of the last command.
    if [ $? = 0 ]; then
        echo "SERVICE_STATE=installed" > $TARGET_STATE/packaging.state
    else
        echo "SERVICE_STATE=unknown" > $TARGET_STATE/packaging.state
    fi;
    ;;
    
    uninstall)
    rm -f /bin/apt-cyg

    # Check the state of the last command.
    if [ $? = 0 ]; then
        echo "SERVICE_STATE=uninstalled" > $TARGET_STATE/packaging.state
    else
        echo "SERVICE_STATE=unknown" > $TARGET_STATE/packaging.state
    fi;
    ;;

    state)
    if [ -e /bin/apt-cyg ]; then
        echo "SERVICE_STATE=installed" > $TARGET_STATE/packaging.state
        . $TARGET_STATE/packaging.state
    fi;
    echo $SERVICE_STATE
    ;;

    *)
    echo $"Usage: $0 {build|clean|install|uninstall|state} {platform} {repository}"
    exit 1
esac

