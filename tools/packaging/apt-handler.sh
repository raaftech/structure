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
APT_PLATFORM_NAME=$2
APT_REPOSITORY_NAME=$3


# Main case statement.
case "$1" in
    build)
    cd $UNIVERSAL_HOME/Tools/packaging
    mkdir -p $TARGET_ROOT/$PACKAGING_CONFIG_DIRECTORY/sources.list.d
    
    # If a preferences file previously exists, take it in.
    if [ -e $PACKAGING_CONFIG_DIRECTORY/preferences ]; then
        cp $PACKAGING_CONFIG_DIRECTORY/preferences \
           $TARGET_ROOT/$PACKAGING_CONFIG_DIRECTORY/preferences
    fi;
    
    # Append or create the preferences file.
    cat apt-data/$APT_REPOSITORY_NAME.preferences-$APT_PLATFORM_NAME \
        >> $TARGET_ROOT/$PACKAGING_CONFIG_DIRECTORY/preferences

    # Copy the repository files to the target directory.
    cp apt-data/$APT_REPOSITORY_NAME.list-$APT_PLATFORM_NAME \
       $TARGET_ROOT/$PACKAGING_CONFIG_DIRECTORY/sources.list.d/$APT_REPOSITORY_NAME.list

    # Check the state of the last command. 
    if [ $? = 0 ]; then
        echo "SERVICE_STATE=built" > $TARGET_STATE/packaging.state
    else
        echo "SERVICE_STATE=unknown" > $TARGET_STATE/packaging.state
    fi;
    ;;

    clean)
    if [ -e $TARGET_ROOT/$PACKAGING_CONFIG_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$PACKAGING_CONFIG_DIRECTORY"
        cd $TARGET_ROOT/$PACKAGING_CONFIG_DIRECTORY
        rm -f sources.list.d/$APT_REPOSITORY_NAME.list
        rmdir sources.list.d > /dev/null 2>&1 < /dev/null
        rm -f preferences
        
        # What and where do we clean empty directories.
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$PACKAGING_CONFIG_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $PACKAGING_CONFIG_DIRECTORY | sed 's|/| |g'`"
        
        # Traverse down from topmost to $TARGET_ROOT and rmdir.
        while [ "$CLEAN_DIRS_EMPTY" != "$TARGET_ROOT" ]; do
            cd $CLEAN_DIRS_EMPTY/..
            rmdir $CLEAN_DIRS_NAMES > /dev/null 2>&1 < /dev/null
            CLEAN_DIRS_EMPTY=`pwd`
        done;
        
        # Check the state of the last command. 
        if [ $? = 0 ]; then
            echo "SERVICE_STATE=cleaned" > $TARGET_STATE/packaging.state
        else
            echo "SERVICE_STATE=unknown" > $TARGET_STATE/packaging.state
        fi;
    else
        echo "No created config directory found. use \"build\" first. "
    fi;
    ;;
    
    install)
    cd $UNIVERSAL_HOME/Tools/packaging
    mkdir -p $PACKAGING_CONFIG_DIRECTORY/sources.list.d

    # Make sure we don't overwrite a previous backup copy of preferences.
    if [ -e $PACKAGING_CONFIG_DIRECTORY/preferences ]; then
        if [ ! -e $PACKAGING_CONFIG_DIRECTORY/preferences.orig ]; then
            cp $PACKAGING_CONFIG_DIRECTORY/preferences \
               $PACKAGING_CONFIG_DIRECTORY/preferences.orig
        fi;
    fi;
        
    # Install our previously created files.
    install -m 0755 -o root $TARGET_ROOT/$PACKAGING_CONFIG_DIRECTORY/preferences \
                                         $PACKAGING_CONFIG_DIRECTORY/preferences
    install -m 0755 -o root $TARGET_ROOT/$PACKAGING_CONFIG_DIRECTORY/sources.list.d/$APT_REPOSITORY_NAME.list \
                                         $PACKAGING_CONFIG_DIRECTORY/sources.list.d/$APT_REPOSITORY_NAME.list

    # If we have a public key for the repository, import it.
    if [ -e apt-data/$APT_REPOSITORY_NAME.key ]; then
        apt-key add apt-data/$APT_REPOSITORY_NAME.key > /dev/null 2>&1 < /dev/null
    fi;

    # Check the state of the last command. 
    if [ $? = 0 ]; then
        echo "SERVICE_STATE=installed" > $TARGET_STATE/packaging.state
    else
        echo "SERVICE_STATE=unknown" > $TARGET_STATE/packaging.state
    fi;
    ;;
    
    uninstall)
    cd $UNIVERSAL_HOME/Tools/packaging
    rm -f $PACKAGING_CONFIG_DIRECTORY/sources.list.d/$APT_REPOSITORY_NAME.list
    
    # If there was an preferences backup, restore it, else remove it.
    if [ -e  $PACKAGING_CONFIG_DIRECTORY/preferences.orig ]; then
        cp $PACKAGING_CONFIG_DIRECTORY/preferences.orig \
           $PACKAGING_CONFIG_DIRECTORY/preferences
    else
        rm -f $PACKAGING_CONFIG_DIRECTORY/preferences
    fi;

    # Remove the imported key from apt-key setup.
    if [ -e apt-data/$APT_REPOSITORY_NAME.key ]; then
        apt-key del $APT_REPOSITORY_NAME > /dev/null 2>&1 < /dev/null
    fi;

    # Check the state of the last command. 
    if [ $? = 0 ]; then
        echo "SERVICE_STATE=uninstalled" > $TARGET_STATE/packaging.state
    else
        echo "SERVICE_STATE=unknown" > $TARGET_STATE/packaging.state
    fi;
    ;;

    state)
    # Check for handywork, set state to installed if so.
    if [ -e $PACKAGING_CONFIG_DIRECTORY/sources.list.d/$APT_REPOSITORY_NAME.list ]; then
        echo "SERVICE_STATE=installed" > $TARGET_STATE/packaging.state
        . $TARGET_STATE/packaging.state
    fi;
    echo $SERVICE_STATE 
    ;;

    *)
    echo $"Usage: $0 {build|clean|install|uninstall|state} {platform} {repository}"
    exit 1
esac

