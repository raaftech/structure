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
    OFFSET="/../"
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

# Include current configuration.
CONFIG="$UNIVERSAL_HOME/Configuration/`hostname`.conf"
if [ -e "$CONFIG" ]; then
    . "$CONFIG"
elif [ -e "$UNIVERSAL_HOME/Configuration/setup.conf" ]; then
    CONFIG="$UNIVERSAL_HOME/Configuration/setup.conf"
    . $CONFIG
else
    CONFIG="none"
    echo "No configuration file found. You can create one by taking a look"
    echo "at \"example.conf\" in the Configuration directory. You can copy"
    echo "this file to \"setup.conf\" or `hostname`.conf in the same directory"
    echo "and it will be picked up by the environment."
    exit 1
fi;

# Specify required tools and utilities.
MANDATORY_TOOLS="awk printf sed sh uname netstat openssl find cat tr echo printf"

# Set platform, release and architecture, both requested and detected.
REQUESTED_PLATFORM=`printf "%s\n" "$PLATFORM_NAME" | tr '[A-Z]' '[a-z]'`
REQUESTED_RELEASE=`printf "%s\n" "$PLATFORM_RELEASE" | tr '[A-Z]' '[a-z]'`
REQUESTED_ARCHITECTURE=`printf "%s\n" "$PLATFORM_ARCHITECTURE" | tr '[A-Z]' '[a-z]'`
DETECTED_PLATFORM="`uname -s`"
DETECTED_RELEASE="`uname -r`"

# Create a _MAJOR and _MINOR release variable so we can use it.
REQUESTED_MAJOR=`echo $REQUESTED_RELEASE | cut -d "." -f 1`
REQUESTED_MINOR=`echo $REQUESTED_RELEASE | cut -d "." -f 2`

# Create the platform descriptor filename so we can include it.
REQUESTED_DESCRIPTOR="$REQUESTED_PLATFORM-$REQUESTED_RELEASE-$REQUESTED_ARCHITECTURE.desc"

# Include the platform descriptor file.
if [ -e "$UNIVERSAL_HOME/Platforms/$REQUESTED_DESCRIPTOR" ]; then
    . "$UNIVERSAL_HOME/Platforms/$REQUESTED_DESCRIPTOR"
else
    echo "No platform definition file found for your platform. Did you specify"
    echo "it correctly? Hint: Take a look at the \"Platforms\" subdirectory."
    echo "I was looking for: $REQUESTED_DESCRIPTOR."
    exit 1
fi;

# Rewrite role to a sensible file that we can include.
REQUESTED_ROLE=`printf "%s\n" "$SERVER_ROLE" | tr '[A-Z]' '[a-z]'`.role

# Include the role file.
if [ -e "$UNIVERSAL_HOME/Roles/$REQUESTED_ROLE" ]; then
    . "$UNIVERSAL_HOME/Roles/$REQUESTED_ROLE"
else
    echo "No role file found. Did you specify it correctly? Hint: Take a look"
    echo "at the \"Roles\" subdirectory."
    echo "I was looking for: $REQUESTED_ROLE."
    exit 1
fi;

# Include open ports array if exists (also created by sanity if possible).
if [ -e "$UNIVERSAL_HOME/Tools/sanity/netstat.$REQUESTED_PLATFORM$REQUESTED_RELEASE" ]; then
    echo "DETECTED_PORTS=\"`sh $UNIVERSAL_HOME/Tools/sanity/netstat.$REQUESTED_PLATFORM$REQUESTED_RELEASE`\"" > $UNIVERSAL_HOME/Includes/ports.out 
fi;
if [ -e "$UNIVERSAL_HOME/Includes/ports.out" ]; then
    . "$UNIVERSAL_HOME/Includes/ports.out"
fi;

# Find all shell files once and make sure they're executable.
if [ ! -e "$UNIVERSAL_HOME/Includes/executables.out" ]; then
    find $UNIVERSAL_HOME -name "*.sh" > $UNIVERSAL_HOME/Includes/executables.out
    for SCRIPT in `cat "$UNIVERSAL_HOME/Includes/executables.out"`; do
    	chmod +x $SCRIPT
    done;
fi;

# Set the target directory for building the setup.
TARGET_ROOT="$UNIVERSAL_HOME/Build/$SERVER_NAME"
TARGET_LOGS="$UNIVERSAL_HOME/Logging/$SERVER_NAME"
TARGET_STATE="$UNIVERSAL_HOME/State/$SERVER_NAME"

