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


# Main case statement.
case "$1" in
    build)
    echo "Building packaging configuration:"
    cd $UNIVERSAL_HOME/Tools/packaging
    for REPO in $PACKAGING_REPOSITORY_LIST; do
        echo -n " * $REPO: ";
        ./$PACKAGING_DISTRIBUTION_TYPE-handler.sh $1 $REQUESTED_PLATFORM$REQUESTED_MAJOR $REPO
        if [ $? = 0 ]; then
            echo "success"
        else
            echo "fail"
        fi;
    done;
    echo ""
    ;;

    clean)
    echo "Cleaning packaging configuration:"
    cd $UNIVERSAL_HOME/Tools/packaging
    for REPO in $PACKAGING_REPOSITORY_LIST; do
        echo -n " * $REPO: ";
        ./$PACKAGING_DISTRIBUTION_TYPE-handler.sh $1 $REQUESTED_PLATFORM$REQUESTED_MAJOR $REPO
        if [ $? = 0 ]; then
            echo "success"
        else
            echo "fail"
        fi;
    done;
    echo ""
    ;;

    install)
    echo "Installing packaging configuration:"
    cd $UNIVERSAL_HOME/Tools/packaging
    for REPO in $PACKAGING_REPOSITORY_LIST; do
        echo -n " * $REPO: ";
        ./$PACKAGING_DISTRIBUTION_TYPE-handler.sh $1 $REQUESTED_PLATFORM$REQUESTED_MAJOR $REPO
        if [ $? = 0 ]; then
            echo "success"
        else
            echo "fail"
        fi;
    done;
    echo ""
    ;;

    uninstall)
    echo "Uninstalling packaging configuration:"
    cd $UNIVERSAL_HOME/Tools/packaging
    for REPO in $PACKAGING_REPOSITORY_LIST; do
        echo -n " * $REPO: ";
        ./$PACKAGING_DISTRIBUTION_TYPE-handler.sh $1 $REQUESTED_PLATFORM$REQUESTED_MAJOR $REPO
        if [ $? = 0 ]; then
            echo "success"
        else
            echo "fail"
        fi;
    done;
    echo ""
    ;;

    state)
    cd $UNIVERSAL_HOME/Tools/packaging
    for REPO in $PACKAGING_REPOSITORY_LIST; do
       STATE=`./$PACKAGING_DISTRIBUTION_TYPE-handler.sh $1 $REQUESTED_PLATFORM$REQUESTED_MAJOR $REPO`
    done;
    echo $STATE
    ;;

    deps)
    if [ $PACKAGING_PACKAGES_REQUIRED = 1 ]; then
        echo "$PACKAGING_PACKAGES_LISTING"
    fi;
    ;;

    *)
    echo $"Usage: $0 {build|clean|install|uninstall|state|deps}"
    exit 1
esac

