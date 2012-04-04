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

# Set local variables.
UTILITIES_FILES_CONFIG="hostctl.sh"

# Include state for this specific module.
if [ -e $TARGET_STATE/utilities.state ]; then
    . $TARGET_STATE/utilities.state
else
    mkdir -p $TARGET_STATE
    echo "SERVICE_STATE=uninitialized" > $TARGET_STATE/utilities.state
    . $TARGET_STATE/utilities.state
fi;


# Main case statement.
case "$1" in
    build)
    # Make sure our target directories exist.
    mkdir -p $TARGET_ROOT/$UTILITIES_CONFIG_DIRECTORY

    cd $UNIVERSAL_HOME/Tools/utilities

    echo "Generating utilities:"
    for FILE in $UTILITIES_FILES_CONFIG; do
        NEWFILE=`echo $FILE | sed "s|.sh||g"`
        echo -n " * $FILE: ";
        cat $FILE   | sed "s|SERVER_NAME|$SERVER_NAME|g" \
                    | sed "s|TARGET_ROOT|$TARGET_ROOT|g" \
                    | sed "s|DHCP3_CONFIG_DIRECTORY|$DHCP3_CONFIG_DIRECTORY|g" \
                    | sed "s|BIND9_DATA_DIRECTORY|$BIND9_DATA_DIRECTORY|g" \
                    | sed "s|DOMAIN_NAME_PART_A|$DOMAIN_NAME_PART_A|g" \
                    | sed "s|DOMAIN_NAME_PART_B|$DOMAIN_NAME_PART_B|g" \
                    | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" \
                    | sed "s|DOMAIN_SUB_BACK|$DOMAIN_SUB_BACK|g" \
                    | sed "s|DOMAIN_SUB_FRONT|$DOMAIN_SUB_FRONT|g" \
                    > $TARGET_ROOT/$UTILITIES_CONFIG_DIRECTORY/$NEWFILE

        if [ $? = 0 ]; then
            echo "success"
            echo "SERVICE_STATE=built" > $TARGET_STATE/utilities.state
        else
            STATE="fail"
            echo "$STATE"
            echo "SERVICE_STATE=unknown" > $TARGET_STATE/utilities.state
        fi;
    done;
    echo ""
    ;;

    clean)
    if [ -e $TARGET_ROOT/$UTILITIES_CONFIG_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$UTILITIES_CONFIG_DIRECTORY"
        cd $TARGET_ROOT/$UTILITIES_CONFIG_DIRECTORY
        rm -vf `echo $UTILITIES_FILES_CONFIG|sed 's|.sh||g'`
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$UTILITIES_CONFIG_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $UTILITIES_CONFIG_DIRECTORY | sed 's|/| |g'`"
        while [ "$CLEAN_DIRS_EMPTY" != "$TARGET_ROOT" ]; do
            cd $CLEAN_DIRS_EMPTY/..
            rmdir $CLEAN_DIRS_NAMES > /dev/null 2>&1 < /dev/null
            CLEAN_DIRS_EMPTY=`pwd`
        done;
        echo "SERVICE_STATE=cleaned" > $TARGET_STATE/utilities.state
    else
        echo "No created config directory found. use \"build\" first. "
    fi;
    echo ""
    ;;

    install)
    if [ -e $TARGET_ROOT/$UTIlITIES_CONFIG_DIRECTORY ]; then
        echo "Installing generated utilities:"
        mkdir -p $UTILITIES_CONFIG_DIRECTORY
        for UTILITY in $UTILITIES_FILES_CONFIG; do
            echo -n " * $UTILITY: ";
            install -m 0755 -o root $TARGET_ROOT/$UTILITIES_CONFIG_DIRECTORY/$UTILITY $UTILITIES_CONFIG_DIRECTORY/$UTILITY

            if [ $? = 0 ]; then
                echo "success"
                echo "SERVICE_STATE=installed" > $TARGET_STATE/utilities.state
            else
                STATE="fail"
                echo "$STATE"
                echo "SERVICE_STATE=unknown" > $TARGET_STATE/utilities.state
            fi;
        done;
    else
         echo "No generated utilities found. use \"build\" first. "
    fi;
    echo ""
    ;;

    uninstall)
    if [ -e $UTILITIES_CONFIG_DIRECTORY ]; then
        echo "Uninstalling generated utilities:"
        for UTILITY in $UTILITIES_FILES_CONFIG; do
            echo -n " * $UTILITY: ";
            rm -f $UTILITIES_CONFIG_DIRECTORY/$UTILITY

            if [ $? = 0 ]; then
                echo "success"
                echo "SERVICE_STATE=uninstalled" > $TARGET_STATE/utilities.state
            else
                STATE="fail"
                echo "$STATE"
                echo "SERVICE_STATE=unknown" > $TARGET_STATE/utilities.state
            fi;
        done;
        rmdir $UTILITIES_CONFIG_DIRECTORY > /dev/null 2>&1 < /dev/null
    else
        echo "No installed utility directory found; Are you sure you've"
        echo "specified the right platform for this machine? Or did you"
        echo "forget to use \"install\" in the first place?"
    fi
    echo ""
    ;;

    state)
    echo $SERVICE_STATE
    ;;

    deps)
    if [ $UTILITIES_PACKAGES_REQUIRED = 1 ]; then
        echo "$UTILITIES_PACKAGES_LISTING"
    fi;
    ;;

    *)
    echo $"Usage: $0 {build|clean|install|uninstall|state|deps}"
    exit 1
esac

