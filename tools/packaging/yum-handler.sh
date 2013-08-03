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
YUM_PLATFORM_NAME=$2
YUM_REPOSITORY_NAME=$3


# Main case statement.
case "$1" in
    build)
    cd $UNIVERSAL_HOME/Tools/packaging
    mkdir -p $TARGET_ROOT/$PACKAGING_CONFIG_DIRECTORY
    mkdir -p $TARGET_ROOT/etc/pki/rpm-gpg

    # Copy the repository files to the target directory.
    cp yum-data/$YUM_REPOSITORY_NAME.repo-$YUM_PLATFORM_NAME \
       $TARGET_ROOT/$PACKAGING_CONFIG_DIRECTORY/$YUM_REPOSITORY_NAME.repo

    # If we have a public key for the repository, copy it.
    if [ -e yum-data/$YUM_REPOSITORY_NAME.key ]; then
        cp yum-data/$YUM_REPOSITORY_NAME.key \
           $TARGET_ROOT/etc/pki/rpm-gpg/RPM-GPG-KEY-$YUM_REPOSITORY_NAME
    fi;

    # Check the state of the last command. 
    if [ $? = 0 ]; then
        echo "SERVICE_STATE=built" > $TARGET_STATE/packaging.state
    else
        echo "SERVICE_STATE=unknown" > $TARGET_STATE/packaging.state
    fi;
    ;;

    clean)
    if [ -e $TARGET_ROOT/$PACKAGING_CONFIG_DIRECTORY ]; then
        cd $TARGET_ROOT/$PACKAGING_CONFIG_DIRECTORY
        rm -f $YUM_REPOSITORY_NAME.repo
        cd $TARGET_ROOT/etc/pki/rpm-gpg
        rm -f RPM-GPG-KEY-$YUM_REPOSITORY_NAME
        rmdir $TARGET_ROOT/etc/pki/rpm-gpg > /dev/null 2>&1 < /dev/null
        rmdir $TARGET_ROOT/etc/pki         > /dev/null 2>&1 < /dev/null

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
    mkdir -p $PACKAGING_CONFIG_DIRECTORY
    mkdir -p /etc/pki/rpm-gpg

    # Install our previously created files.
    cp $TARGET_ROOT/$PACKAGING_CONFIG_DIRECTORY/$YUM_REPOSITORY_NAME.repo \
                    $PACKAGING_CONFIG_DIRECTORY/$YUM_REPOSITORY_NAME.repo
   
    # If we have a public key for the repository, copy it.
    if [ -e $TARGET_ROOT/etc/pki/rpm-gpg/RPM-GPG-KEY-$YUM_REPOSITORY_NAME ]; then
        cp $TARGET_ROOT/etc/pki/rpm-gpg/RPM-GPG-KEY-$YUM_REPOSITORY_NAME \
                       /etc/pki/rpm-gpg/RPM-GPG-KEY-$YUM_REPOSITORY_NAME
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
    if [ -e $PACKAGING_CONFIG_DIRECTORY ]; then
        cd $PACKAGING_CONFIG_DIRECTORY
        rm -f $YUM_REPOSITORY_NAME.repo
        cd /etc/pki/rpm-gpg
        rm -f RPM-GPG-KEY-$YUM_REPOSITORY_NAME
        rmdir /etc/pki/rpm-gpg > /dev/null 2>&1 < /dev/null
        rmdir /etc/pki         > /dev/null 2>&1 < /dev/null
        
        # What and where do we clean empty directories.
        CLEAN_DIRS_EMPTY="$PACKAGING_CONFIG_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $PACKAGING_CONFIG_DIRECTORY | sed 's|/| |g'`"
        
        # Traverse down from topmost to root and rmdir.
        while [ "$CLEAN_DIRS_EMPTY" != "/" ]; do
            cd $CLEAN_DIRS_EMPTY/..
            rmdir $CLEAN_DIRS_NAMES > /dev/null 2>&1 < /dev/null
            CLEAN_DIRS_EMPTY=`pwd`
        done;
    
        # Check the state of the last command. 
        if [ $? = 0 ]; then
            echo "SERVICE_STATE=uninstalled" > $TARGET_STATE/packaging.state
        else
            echo "SERVICE_STATE=unknown" > $TARGET_STATE/packaging.state
        fi;
    else
        echo "No created config directory found. use \"install\" first. "
    fi;
    ;;

    state)
    # Check for handywork, set state to installed if so.
    if [ -e $PACKAGING_CONFIG_DIRECTORY/$YUM_REPOSITORY_NAME.repo ]; then
        echo "SERVICE_STATE=installed" > $TARGET_STATE/packaging.state
        . $TARGET_STATE/packaging.state
    fi;
    echo $SERVICE_STATE
    ;;

    *)
    echo $"Usage: $0 {build|clean|install|uninstall|state} {platform} {repository}"
    exit 1
esac

