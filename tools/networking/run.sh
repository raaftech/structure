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

# Include state for this specific module.
if [ -e $TARGET_STATE/networking.state ]; then
    . $TARGET_STATE/networking.state
else
    mkdir -p $TARGET_STATE
    echo "SERVICE_STATE=uninitialized" > $TARGET_STATE/networking.state
    . $TARGET_STATE/networking.state
fi;


# Main case statement.
case "$1" in
    build)
    ;;

    clean)
    ;;

    install)
    ;;

    uninstall)
    ;;

    state)
    echo $SERVICE_STATE
    ;;

    deps)
    ;;

    mask)
    for RANGE in $DOMAIN_RANGES; do
        DOMAIN_NAME=`echo $RANGE | cut -d "/" -f 1`
        DOMAIN_ADDR=`echo $RANGE | cut -d "/" -f 2`
        DOMAIN_CIDR=`echo $RANGE | cut -d "/" -f 3`
        DOMAIN_MASK=`cidr2Mask $DOMAIN_CIDR`
        
        BITS=`expr 32 - $DOMAIN_CIDR`
        NUMBER=`awk "BEGIN{print 2^$BITS}"`
        echo Number of addresses in $DOMAIN_ADDR/$DOMAIN_CIDR: $NUMBER
        BEGIN=`ip2Int $DOMAIN_ADDR`
        CALC=`expr $BEGIN + $NUMBER - 1`
        echo And the last IP in this range: `int2Ip $CALC`
        echo And the domain mask: $DOMAIN_MASK
    done;
    ;;

    *)
    echo $"Usage: $0 {build|clean|install|uninstall|state|deps|mask}"
    exit 1
esac

