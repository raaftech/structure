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
        cd - > /dev/null
        unset ABS
    elif [ -e "`dirname "$0"`$OFFSET" ]; then
        # Found, probably called absolutely.
        cd "`dirname "$0"`$OFFSET"
        UNIVERSAL_HOME="`pwd`"
        cd - > /dev/null
    fi;
fi;

# Include global setup, environment and functions.
if [ ! $GLOBAL_RUN ]; then
    . $UNIVERSAL_HOME/Includes/environment.sh
    . $UNIVERSAL_HOME/Includes/functions.sh
fi;

# Set local variables.
SERVICE="$(basename $(dirname $(readlink -f $0)))"
SERVICE_VENDOR="`echo "$SERVICE" | cut -d '-' -f 1`"
SERVICE_NAME="`echo "$SERVICE" | cut -d '-' -f 2`"
SERVICE_VERSION="`echo "$SERVICE" | cut -d '-' -f 3`"
SERVICE_DEPS="$CFENGINE2_SERVICE_DEPS"
SERVICE_PRIO="26"
SERVICE_PORT="5308"

# Check if globals we care about are set.
NAMESPACE=CFENGINE2
CHECK="`set | grep ^${NAMESPACE}_`"
if [ -z "$CHECK" ]; then
    mkdir -p $TARGET_STATE
    echo "SERVICE_STATE=failed" > $TARGET_STATE/$SERVICE.state
fi;
unset CHECK
unset NAMESPACE

# What are we working with?
CFENGINE2_FILES_CONFIG="cfservd.conf update.conf"

# We only care about the first (tld) domain within DOMAIN_RANGES.
DOMAIN_NAME=`echo $DOMAIN_RANGES | cut -d "/" -f 1`

# Create ALLOWED_RANGES.
COUNT=0
for RANGE in $DOMAIN_RANGES; do
    COUNT=`expr $COUNT "+" 1`
done;
if [ "$COUNT" -eq 1 ]; then
    for RANGE in $DOMAIN_RANGES; do
        DOMAIN_ADDR=`echo $RANGE | cut -d "/" -f 2`
        DOMAIN_CIDR=`echo $RANGE | cut -d "/" -f 3`
        CFENGINE2_ALLOWED_RANGES="$CFENGINE2_ALLOWED_RANGES $DOMAIN_ADDR/$DOMAIN_CIDR"
    done;
else
    COUNT=0
    for RANGE in $DOMAIN_RANGES; do
        DOMAIN_ADDR=`echo $RANGE | cut -d "/" -f 2`
        DOMAIN_CIDR=`echo $RANGE | cut -d "/" -f 3`
        if [ "$COUNT" -ne 0 ]; then
            CFENGINE2_ALLOWED_RANGES="$CFENGINE2_ALLOWED_RANGES $DOMAIN_ADDR/$DOMAIN_CIDR"
        fi;
        COUNT=`expr $COUNT "+" 1`
    done;
fi;

# Only for update.conf. Can go away when fixed upstream.
if [ $REQUESTED_PLATFORM = debian ]; then
    CFENGINE2_CFEXECD_INIT="cfengine2"
else
    CFENGINE2_CFEXECD_INIT="cfexecd"
fi;

# Include state for this specific module.
if [ -e $TARGET_STATE/$SERVICE.state ]; then
    . $TARGET_STATE/$SERVICE.state
else
    mkdir -p $TARGET_STATE
    echo "SERVICE_STATE=uninitialized" > $TARGET_STATE/$SERVICE.state
    . $TARGET_STATE/$SERVICE.state
fi;

# Main case statement.
case "$1" in
    build)
    if [ $SERVICE_STATE = uninitialized ]; then
        echo "SERVICE_STATE is uninitialized, continuing $1."
    elif [ $SERVICE_STATE = built ]; then
        echo "SERVICE_STATE is already built, aborting $1."
        exit 1
    elif [ $SERVICE_STATE = cleaned ]; then
        echo "SERVICE_STATE is cleaned, continuing $1."
    elif [ $SERVICE_STATE = installed ]; then
        echo "SERVICE_STATE is installed, aborting $1."
        exit 1
    elif [ $SERVICE_STATE = uninstalled ]; then
        echo "SERVICE_STATE is uninstalled, aborting $1."
        exit 1
    else
        echo "SERVICE_STATE is $SERVICE_STATE, aborting $1."
        exit 1
    fi;
    
    if [ $SERVICE_DEPS ]; then
        echo "This service depends on other services to be built first."
        echo "Checking to make sure if they've been built before this module:"
        for SERVICE_DEP in $SERVICE_DEPS; do
            SERVICE_DEP_NAME=`echo $SERVICE_DEP | sed 's/(/ /g' \
                                                | sed 's/)/ /g' \
                                                | awk '{ print $1 }'`
            SERVICE_DEP_IMPL=`echo $SERVICE_DEP | sed 's/(/ /g' \
                                                | sed 's/)/ /g' \
                                                | awk '{ print $2 }'`
            SERVICE_DEP_STATE=`$UNIVERSAL_HOME/Services/$SERVICE_DEP_NAME/$SERVICE_DEP_IMPL/run.sh state`
            echo -n " * $SERVICE_DEP: "
            if [ $SERVICE_DEP_STATE = "built" ]; then
                echo $SERVICE_DEP_STATE
            else
                echo -n "building... "
                $UNIVERSAL_HOME/Services/$SERVICE_DEP_NAME/$SERVICE_DEP_IMPL/run.sh $1 > /dev/null 2>&1 < /dev/null
                SERVICE_DEP_STATE=`$UNIVERSAL_HOME/Services/$SERVICE_DEP_NAME/$SERVICE_DEP_IMPL/run.sh state`
                echo $SERVICE_DEP_STATE
            fi;
        done;
    fi;

    # Make sure our target directories exist.
    mkdir -p $TARGET_ROOT/$CFENGINE2_CONFIG_DIRECTORY
    mkdir -p $TARGET_ROOT/$CFENGINE2_DATA_DIRECTORY/backups
    mkdir -p $TARGET_ROOT/$CFENGINE2_DATA_DIRECTORY/bin
    mkdir -p $TARGET_ROOT/$CFENGINE2_DATA_DIRECTORY/masterfiles
    mkdir -p $TARGET_ROOT/$CFENGINE2_DATA_DIRECTORY/outputs
    mkdir -p $TARGET_ROOT/$CFENGINE2_DATA_DIRECTORY/ppkeys
    mkdir -p $TARGET_ROOT/$CFENGINE2_DATA_DIRECTORY/rpc_in
    mkdir -p $TARGET_ROOT/$CFENGINE2_DATA_DIRECTORY/rpc_out
    mkdir -p $TARGET_ROOT/$CFENGINE2_DATA_DIRECTORY/state

    cd $UNIVERSAL_HOME/Services/$SERVICE

    echo "Creating default default set of configuration files.."
    for FILE in $CFENGINE2_FILES_CONFIG; do
        cat $FILE   | sed "s|SERVER_NAME|$SERVER_NAME|g" \
                    | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" \
                    | sed "s|CFENGINE2_ALLOWED_RANGES|$CFENGINE2_ALLOWED_RANGES|g" \
                    | sed "s|CFENGINE2_CFEXECD_INIT|$CFENGINE2_CFEXECD_INIT|g" \
                    | sed "s|CFENGINE2_CONFIG_DIRECTORY|$CFENGINE2_CONFIG_DIRECTORY|g" \
                    | sed "s|CFENGINE2_DATA_DIRECTORY|$CFENGINE2_DATA_DIRECTORY|g" \
                    | sed "s|PLATFORM_INIT_DIRECTORY|$PLATFORM_INIT_DIRECTORY|g" \
                    > $TARGET_ROOT/$CFENGINE2_CONFIG_DIRECTORY/$FILE
    done;

    echo "Creating mandatory links in \$TARGET_ROOT$CFENGINE2_DATA_DIRECTORY.."
    cd $TARGET_ROOT/$CFENGINE2_DATA_DIRECTORY
        ln -s $CFENGINE2_CONFIG_DIRECTORY inputs
    cd -
    cd $TARGET_ROOT/$CFENGINE2_DATA_DIRECTORY/bin
        ln -s $CFENGINE2_BINARY_DIRECTORY/cfagent cfagent
    cd -

    if [ $? = 0 ]; then
        echo "SERVICE_STATE=built" > $TARGET_STATE/$SERVICE.state
    else
        echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE.state
    fi;
    echo ""
    ;;

    clean)
    if [ $SERVICE_STATE = uninitialized ]; then
        echo "SERVICE_STATE is uninitialized, aborting $1."
        exit 1
    elif [ $SERVICE_STATE = built ]; then
        echo "SERVICE_STATE is built, continuing $1."
    elif [ $SERVICE_STATE = cleaned ]; then
        echo "SERVICE_STATE is already cleaned, aborting $1."
        exit 1
    elif [ $SERVICE_STATE = installed ]; then
        echo "SERVICE_STATE is installed, aborting $1."
        exit 1
    elif [ $SERVICE_STATE = uninstalled ]; then
        echo "SERVICE_STATE is uninstalled, continuing $1."
    else
        echo "SERVICE_STATE is $SERVICE_STATE, aborting $1."
        exit 1
    fi;
    
    if [ $SERVICE_DEPS ]; then
        echo "This service depends on other services. Cleaning them first:"
        for SERVICE_DEP in $SERVICE_DEPS; do
            SERVICE_DEP_NAME=`echo $SERVICE_DEP | sed 's/(/ /g' \
                                                | sed 's/)/ /g' \
                                                | awk '{ print $1 }'`
            SERVICE_DEP_IMPL=`echo $SERVICE_DEP | sed 's/(/ /g' \
                                                | sed 's/)/ /g' \
                                                | awk '{ print $2 }'`
            SERVICE_DEP_STATE=`$UNIVERSAL_HOME/Services/$SERVICE_DEP_NAME/$SERVICE_DEP_IMPL/run.sh state`
            echo -n " * $SERVICE_DEP: "
            if [ $SERVICE_DEP_STATE = "cleaned" ]; then
                echo $SERVICE_DEP_STATE
            else
                echo -n "cleaning... "
                $UNIVERSAL_HOME/Services/$SERVICE_DEP_NAME/$SERVICE_DEP_IMPL/run.sh $1 > /dev/null 2>&1 < /dev/null
                SERVICE_DEP_STATE=`$UNIVERSAL_HOME/Services/$SERVICE_DEP_NAME/$SERVICE_DEP_IMPL/run.sh state`
                echo $SERVICE_DEP_STATE
            fi;
        done;
    fi;

    if [ -e $TARGET_ROOT/$CFENGINE2_CONFIG_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$CFENGINE2_CONFIG_DIRECTORY"
        cd $TARGET_ROOT/$CFENGINE2_CONFIG_DIRECTORY
        rm -vf $CFENGINE2_FILES_CONFIG
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$CFENGINE2_CONFIG_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $CFENGINE2_CONFIG_DIRECTORY | sed 's|/| |g'`"
        while [ "$CLEAN_DIRS_EMPTY" != "$TARGET_ROOT" ]; do
            cd $CLEAN_DIRS_EMPTY/..
            rmdir $CLEAN_DIRS_NAMES > /dev/null 2>&1 < /dev/null
            CLEAN_DIRS_EMPTY=`pwd`
        done;
        
        if [ $? = 0 ]; then
            echo "SERVICE_STATE=cleaned" > $TARGET_STATE/$SERVICE.state
        else
            echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE.state
        fi;
    else
        echo "No created config directory found. use \"build\" first. "
    fi;

    if [ -e $TARGET_ROOT/$CFENGINE2_DATA_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$CFENGINE2_DATA_DIRECTORY"
        cd $TARGET_ROOT/$CFENGINE2_DATA_DIRECTORY
        rm -v inputs
        rm -v bin/cfagent
        rmdir backups bin masterfiles outputs ppkeys rpc_in rpc_out state
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$CFENGINE2_DATA_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $CFENGINE2_DATA_DIRECTORY | sed 's|/| |g'`"
        while [ "$CLEAN_DIRS_EMPTY" != "$TARGET_ROOT" ]; do
            cd $CLEAN_DIRS_EMPTY/..
            rmdir $CLEAN_DIRS_NAMES > /dev/null 2>&1 < /dev/null
            CLEAN_DIRS_EMPTY=`pwd`
        done;
        
        if [ $? = 0 ]; then
            echo "SERVICE_STATE=cleaned" > $TARGET_STATE/$SERVICE.state
        else
            echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE.state
        fi;
    else
        echo "No created data directory found. use \"build\" first. "
    fi;

    echo ""
    ;;
    
    install)
    if [ $SERVICE_STATE = uninitialized ]; then
        echo "SERVICE_STATE is uninitialized, aborting $1."
        exit 1
    elif [ $SERVICE_STATE = built ]; then
        echo "SERVICE_STATE is built, continuing $1."
    elif [ $SERVICE_STATE = cleaned ]; then
        echo "SERVICE_STATE is cleaned, aborting $1."
        exit 1
    elif [ $SERVICE_STATE = installed ]; then
        echo "SERVICE_STATE is already installed, aborting $1."
        exit 1
    elif [ $SERVICE_STATE = uninstalled ]; then
        echo "SERVICE_STATE is uninstalled, aborting $1."
        exit 1
    else
        echo "SERVICE_STATE is $SERVICE_STATE, aborting $1."
        exit 1
    fi;
    
    for PORT in $SERVICE_PORT; do
        CHECK=`echo $DETECTED_PORTS | grep -w $PORT`
        if [ ! -z "$CHECK" ];then
            echo "$PORT is detected as being in use, refusing to install."
            echo "I need port $PORT to function."
            echo "Please make sure any service using port $PORT is not running!"
            FOUND="1"
        fi;
    done;
    if [ ! -z "$FOUND" ]; then
        exit 1
    fi;
    unset PORT CHECK FOUND
    
    if [ $SERVICE_DEPS ]; then
        echo "This service depends on other services. Installing them first:"
        for SERVICE_DEP in $SERVICE_DEPS; do
            SERVICE_DEP_NAME=`echo $SERVICE_DEP | sed 's/(/ /g' \
                                                | sed 's/)/ /g' \
                                                | awk '{ print $1 }'`
            SERVICE_DEP_IMPL=`echo $SERVICE_DEP | sed 's/(/ /g' \
                                                | sed 's/)/ /g' \
                                                | awk '{ print $2 }'`
            SERVICE_DEP_STATE=`$UNIVERSAL_HOME/Services/$SERVICE_DEP_NAME/$SERVICE_DEP_IMPL/run.sh state`
            echo -n " * $SERVICE_DEP: "
            if [ $SERVICE_DEP_STATE = "installed" ]; then
                echo $SERVICE_DEP_STATE
            else
                echo -n "installing... "
                $UNIVERSAL_HOME/Services/$SERVICE_DEP_NAME/$SERVICE_DEP_IMPL/run.sh $1 > /dev/null 2>&1 < /dev/null
                SERVICE_DEP_STATE=`$UNIVERSAL_HOME/Services/$SERVICE_DEP_NAME/$SERVICE_DEP_IMPL/run.sh state`
                echo $SERVICE_DEP_STATE
            fi;
        done;
    fi;

    if [ -e $TARGET_ROOT/$CFENGINE2_CONFIG_DIRECTORY ]; then
        echo "Installing generated configuration:"
        install -d -m 0755 -o root $CFENGINE2_CONFIG_DIRECTORY
        install -d -m 0755 -o root $CFENGINE2_DATA_DIRECTORY/orig

        echo "Moving existing cfengine data aside, if any.."
        mv $CFENGINE2_DATA_DIRECTORY/* $CFENGINE2_DATA_DIRECTORY/orig

        install -d -m 0755 -o root $CFENGINE2_DATA_DIRECTORY/backups
        install -d -m 0755 -o root $CFENGINE2_DATA_DIRECTORY/bin
        install -d -m 0755 -o root $CFENGINE2_DATA_DIRECTORY/masterfiles/inputs
        install -d -m 0755 -o root $CFENGINE2_DATA_DIRECTORY/outputs
        install -d -m 0700 -o root $CFENGINE2_DATA_DIRECTORY/ppkeys
        install -d -m 0700 -o root $CFENGINE2_DATA_DIRECTORY/rpc_in
        install -d -m 0700 -o root $CFENGINE2_DATA_DIRECTORY/rpc_out
        install -d -m 0755 -o root $CFENGINE2_DATA_DIRECTORY/state

        for CONFIG in $CFENGINE2_FILES_CONFIG; do
            if [ -e $CFENGINE2_CONFIG_DIRECTORY/$CONFIG ]; then
                if [ ! -e $CFENGINE2_CONFIG_DIRECTORY/$CONFIG.orig ]; then
                    cp $CFENGINE2_CONFIG_DIRECTORY/$CONFIG $CFENGINE2_CONFIG_DIRECTORY/$CONFIG.orig
                fi;
            fi;
            echo -n " * $CONFIG: ";
            install -m 0755 -o root $TARGET_ROOT/$CFENGINE2_CONFIG_DIRECTORY/$CONFIG $CFENGINE2_CONFIG_DIRECTORY/$CONFIG

            if [ $? = 0 ]; then
                echo "success"
                echo "SERVICE_STATE=installed" > $TARGET_STATE/$SERVICE.state
            else
                STATE="fail"
                echo "$STATE"
                echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE.state
            fi;
        done;

        echo "Creating mandatory links in $CFENGINE2_DATA_DIRECTORY.."
        cd $CFENGINE2_DATA_DIRECTORY
            ln -s $CFENGINE2_CONFIG_DIRECTORY inputs
        cd -
        cd $CFENGINE2_DATA_DIRECTORY/bin
            ln -s $CFENGINE2_BINARY_DIRECTORY/cfagent cfagent
        cd -

        echo "Running cfkey to create localhost.pub and localhost.priv.."
        cfkey
    else
         echo "No generated configuration found. use \"build\" first. "
    fi; 
    echo ""
    ;;
    
    uninstall)
    if [ $SERVICE_STATE = uninitialized ]; then
        echo "SERVICE_STATE is uninitialized, aborting $1."
        exit 1
    elif [ $SERVICE_STATE = built ]; then
        echo "SERVICE_STATE is built, aborting $1."
        exit 1
    elif [ $SERVICE_STATE = cleaned ]; then
        echo "SERVICE_STATE is cleaned, aborting $1."
        exit 1
    elif [ $SERVICE_STATE = installed ]; then
        echo "SERVICE_STATE is installed, continuing $1."
    elif [ $SERVICE_STATE = uninstalled ]; then
        echo "SERVICE_STATE is already uninstalled, aborting $1."
        exit 1
    else
        echo "SERVICE_STATE is $SERVICE_STATE, aborting $1."
        exit 1
    fi;
    
    if [ $SERVICE_DEPS ]; then
        echo "This service depends on other services. Uninstalling them first:"
        for SERVICE_DEP in $SERVICE_DEPS; do
            SERVICE_DEP_NAME=`echo $SERVICE_DEP | sed 's/(/ /g' \
                                                | sed 's/)/ /g' \
                                                | awk '{ print $1 }'`
            SERVICE_DEP_IMPL=`echo $SERVICE_DEP | sed 's/(/ /g' \
                                                | sed 's/)/ /g' \
                                                | awk '{ print $2 }'`
            SERVICE_DEP_STATE=`$UNIVERSAL_HOME/Services/$SERVICE_DEP_NAME/$SERVICE_DEP_IMPL/run.sh state`
            echo -n " * $SERVICE_DEP: "
            if [ $SERVICE_DEP_STATE = "uninstalled" ]; then
                echo $SERVICE_DEP_STATE
            else
                echo -n "uninstalling... "
                $UNIVERSAL_HOME/Services/$SERVICE_DEP_NAME/$SERVICE_DEP_IMPL/run.sh $1 > /dev/null 2>&1 < /dev/null
                SERVICE_DEP_STATE=`$UNIVERSAL_HOME/Services/$SERVICE_DEP_NAME/$SERVICE_DEP_IMPL/run.sh state`
                echo $SERVICE_DEP_STATE
            fi;
        done;
    fi;

    if [ -e $CFENGINE2_CONFIG_DIRECTORY ]; then
        echo "Uninstalling generated configuration.."
        for CONFIG in $CFENGINE2_FILES_CONFIG; do
            if [ -e $CFENGINE2_CONFIG_DIRECTORY/$CONFIG.orig ]; then
                cp $CFENGINE2_CONFIG_DIRECTORY/$CONFIG.orig $CFENGINE2_CONFIG_DIRECTORY/$CONFIG
            else
                rm -f $CFENGINE2_CONFIG_DIRECTORY/$CONFIG
            fi;
        done;
        
        echo "Restoring cfengine data originals if any.."
        mv $CFENGINE2_DATA_DIRECTORY/orig/* $CFENGINE2_DATA_DIRECTORY
        rmdir $CFENGINE2_DATA_DIRECTORY/orig

        echo "Removing directories if empty.."
        rmdir $CFENGINE2_CONFIG_DIRECTORY
        rmdir $CFENGINE2_DATA_DIRECTORY
        echo ""

        if [ $? = 0 ]; then
            echo "SERVICE_STATE=uninstalled" > $TARGET_STATE/$SERVICE.state
        else
            echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE.state
        fi;
    else
        echo "No installed configuration directory found; Are you sure you've"
        echo "specified the right platform for this machine? Or did you"
        echo "forget to use \"install\" in the first place?"
    fi;
    ;;

    start)
    if [ $SERVICE_STATE != installed ]; then
        echo "SERVICE_STATE is not installed, aborting $1."
        exit 1
    fi;
    
    for PORT in $SERVICE_PORT; do
        CHECK=`echo $DETECTED_PORTS | grep -w $PORT`
        if [ ! -z "$CHECK" ];then
            echo "$PORT is detected as being in use, refusing to install."
            echo "I need port $PORT to function."
            echo "Please make sure any service using port $PORT is not running!"
            FOUND="1"
        fi;
    done;
    if [ ! -z "$FOUND" ]; then
        exit 1
    fi;
    unset PORT CHECK FOUND
    
    # Make sure our service dependencies configured for startup also.
    if [ $SERVICE_DEPS ]; then
        for SERVICE_DEP in $SERVICE_DEPS; do
            SERVICE_DEP_NAME=`echo $SERVICE_DEP | sed 's/(/ /g' \
                                                | sed 's/)/ /g' \
                                                | awk '{ print $1 }'`
            SERVICE_DEP_IMPL=`echo $SERVICE_DEP | sed 's/(/ /g' \
                                                | sed 's/)/ /g' \
                                                | awk '{ print $2 }'`
            $UNIVERSAL_HOME/Services/$SERVICE_DEP_NAME/$SERVICE_DEP_IMPL/run.sh $1
        done;
    fi;    

    # Make sure we've actually installed the service configuration.    
    if [ $SERVICE_STATE = "installed" ]; then
        
        # Configure service for startup.        
        echo "Configuring service for startup.."
        if [ "$CFENGINE2_INIT_STRING" ]; then
            if [ -e $CFENGINE2_INIT_CONFIG ]; then
                if [ ! -e $CFENGINE2_INIT_CONFIG.orig ]; then
                    cp $CFENGINE2_INIT_CONFIG $CFENGINE2_INIT_CONFIG.orig
                elif [ -e $CFENGINE2_INIT_CONFIG.orig ]; then  
                    cp $CFENGINE2_INIT_CONFIG.orig $CFENGINE2_INIT_CONFIG
                fi;
            else
                echo "Mmm, your specified config file does not seem to exist:"
                echo "CFENGINE2_INIT_CONFIG=$CFENGINE2_INIT_CONFIG"
                echo ""
                echo "I will create it for you using CFENGINE2_INIT_STRING as its content"
                echo "But I can't tell if this will be sufficient. Just so you know.."
                echo ""
            fi;
            echo "Appending CFENGINE2_INIT_STRING to $CFENGINE2_INIT_CONFIG.."
            echo "$CFENGINE2_INIT_STRING" >> $CFENGINE2_INIT_CONFIG
        fi;
        COUNT=$SERVICE_PRIO
        for CFENGINE2_INIT_NAME in $CFENGINE2_INIT_SCRIPTS; do
            if [ $PLATFORM_INIT_TYPE = "bsd" ]; then
                echo "Specified init type is $PLATFORM_INIT_TYPE. Acting accordingly.."
                if [ -e $PLATFORM_INIT_DIRECTORY/$CFENGINE2_INIT_NAME ]; then
                    echo "Enabling boot-time startup for $CFENGINE2_INIT_NAME in /etc/rc.conf."
                    echo "$CFENGINE2_INIT_NAME=YES" >> /etc/rc.conf
                else
                    echo "No $CFENGINE2_INIT_NAME init script(s) found!"
                    echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                    echo "init script(s): $CFENGINE2_INIT_SCRIPTS"
                fi;
            elif [ $PLATFORM_INIT_TYPE = "sysv" ]; then
                echo "Specified init type is $PLATFORM_INIT_TYPE. Acting accordingly.."
                if [ -e $PLATFORM_INIT_DIRECTORY/$CFENGINE2_INIT_NAME ]; then
                    echo "Enabling boot-time startup for $CFENGINE2_INIT_NAME in runlevels 2,3,4,5."
                    for RUNLEVEL in 2 3 4 5; do
                        rm -f $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/*$CFENGINE2_INIT_NAME
                        ln -s $PLATFORM_INIT_DIRECTORY/$CFENGINE2_INIT_NAME $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/S$COUNT$CFENGINE2_INIT_NAME
                    done;
                    for RUNLEVEL in 0 1 6; do
                        rm -f $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/*$CFENGINE2_INIT_NAME
                        ln -s $PLATFORM_INIT_DIRECTORY/$CFENGINE2_INIT_NAME $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/K$COUNT$CFENGINE2_INIT_NAME
                    done;
                else
                    echo "No $CFENGINE2_INIT_NAME init script(s) found!"
                    echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                    echo "init script(s): $CFENGINE2_INIT_SCRIPTS"
                fi;
            else
                echo "The variable PLATFORM_INIT_TYPE was set to \"$PLATFORM_INIT_TYPE\";"
                echo "This is currently not supported. It has to be either bsd or sysv!"
            fi;
        
            # And start it up.
            echo "Starting up $CFENGINE2_INIT_NAME.."
            if [ -e $PLATFORM_INIT_DIRECTORY/$CFENGINE2_INIT_NAME ]; then
                $PLATFORM_INIT_DIRECTORY/$CFENGINE2_INIT_NAME start
            else
                echo "No $CFENGINE2_INIT_NAME init script(s) found!"
                echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                echo "init script(s): $CFENGINE2_INIT_SCRIPTS"
            fi;
            COUNT=`expr $COUNT "+" 1`
        done;
    else
        echo "No installed configuration found; Are you sure you've"
        echo "specified the right platform for this machine? Or did you"
        echo "forget to use \"install\" in the first place?"
    fi;
    ;;
    
    stop)
    if [ $SERVICE_STATE != installed ]; then
        echo "SERVICE_STATE is not installed, but wil try to $1 anyway!"
    fi;
    
    # Make sure our service dependencies are stopped also.
    if [ $SERVICE_DEPS ]; then
        for SERVICE_DEP in $SERVICE_DEPS; do
            SERVICE_DEP_NAME=`echo $SERVICE_DEP | sed 's/(/ /g' \
                                                | sed 's/)/ /g' \
                                                | awk '{ print $1 }'`
            SERVICE_DEP_IMPL=`echo $SERVICE_DEP | sed 's/(/ /g' \
                                                | sed 's/)/ /g' \
                                                | awk '{ print $2 }'`
            $UNIVERSAL_HOME/Services/$SERVICE_DEP_NAME/$SERVICE_DEP_IMPL/run.sh $1
        done;
    fi;    

    # Unconfigure service for startup.        
    echo "Unconfiguring service for startup.."
    if [ "$CFENGINE2_INIT_STRING" ]; then
        if [ -e $CFENGINE2_INIT_CONFIG.orig ]; then
            cp $CFENGINE2_INIT_CONFIG.orig $CFENGINE2_INIT_CONFIG
        fi;
    fi;

    for CFENGINE2_INIT_NAME in $CFENGINE2_INIT_SCRIPTS; do
        if [ $PLATFORM_INIT_TYPE = "bsd" ]; then
            if [ -e $PLATFORM_INIT_DIRECTORY/$CFENGINE2_INIT_NAME ]; then
                echo "$CFENGINE2_INIT_NAME=NO" >> /etc/rc.conf
            else
                echo "No $CFENGINE2_INIT_NAME init script(s) found!"
                echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                echo "init script(s): $CFENGINE2_INIT_SCRIPTS"
            fi;
        elif [ $PLATFORM_INIT_TYPE = "sysv" ]; then
            if [ -e $PLATFORM_INIT_DIRECTORY/$CFENGINE2_INIT_NAME ]; then
                for RUNLEVEL in 0 1 2 3 4 5 6; do
                    rm -f $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/*$CFENGINE2_INIT_NAME
                done;
            else
                echo "No $CFENGINE2_INIT_NAME init script(s) found!"
                echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                echo "init script(s): $CFENGINE2_INIT_SCRIPTS"
            fi;
        else
            echo "The variable PLATFORM_INIT_TYPE was set to \"$PLATFORM_INIT_TYPE\";"
            echo "This is currently not supported. It has to be either bsd or sysv!"
        fi;

        # And stop the service.
        echo "Stopping $CFENGINE2_INIT_NAME.."
        if [ -e $PLATFORM_INIT_DIRECTORY/$CFENGINE2_INIT_NAME ]; then
            $PLATFORM_INIT_DIRECTORY/$CFENGINE2_INIT_NAME stop
        else
            echo "No $CFENGINE2_INIT_NAME init script(s) found!"
            echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
            echo "init script(s): $CFENGINE2_INIT_SCRIPTS"
        fi;
    done;
    ;;

    state)
    # Simply echo the last written state of this module.
    echo $SERVICE_STATE
    ;;
    
    deps)
    # Make sure our service dependencies are queried also.
    if [ $SERVICE_DEPS ]; then
        for SERVICE_DEP in $SERVICE_DEPS; do
            SERVICE_DEP_NAME=`echo $SERVICE_DEP | sed 's/(/ /g' \
                                                | sed 's/)/ /g' \
                                                | awk '{ print $1 }'`
            SERVICE_DEP_IMPL=`echo $SERVICE_DEP | sed 's/(/ /g' \
                                                | sed 's/)/ /g' \
                                                | awk '{ print $2 }'`
            $UNIVERSAL_HOME/Services/$SERVICE_DEP_NAME/$SERVICE_DEP_IMPL/run.sh $1
        done;
    fi;

    # Query this module's software dependencies if any.
    if [ "$CFENGINE2_PACKAGES_REQUIRED" = 1 ]; then
        echo "$CFENGINE2_PACKAGES_LISTING"
    fi;
    ;;

    *)
    echo $"Usage: $0 {build|clean|install|uninstall|start|stop|state|deps}"
    exit 1
esac
