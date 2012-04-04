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
    OFFSET="/../../../"
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
SERVICE_NAME="smb"
SERVICE_IMPL="samba3"
SERVICE_DEPS="$SAMBA3_SERVICE_DEPS"
SERVICE_PRIO="23"
SERVICE_PORT="137 138 139 445"

# Check if globals we care about are set.
NAMESPACE=SAMBA3
CHECK="`set | grep ^${NAMESPACE}_`"
if [ -z "$CHECK" ]; then
    mkdir -p $TARGET_STATE
    echo "SERVICE_STATE=failed" > $TARGET_STATE/$SERVICE_IMPL.state
fi;
unset CHECK
unset NAMESPACE

# What are we working with?
SAMBA3_FILES_CONFIG="smb.conf smbusers"

# We only care about the first (tld) domain name.
DOMAIN_NAME=`echo $DOMAIN_RANGES | cut -d "/" -f 1`
DOMAIN_NAME_PART_A=`echo $DOMAIN_NAME | cut -d "." -f 1`
DOMAIN_NAME_PART_B=`echo $DOMAIN_NAME | cut -d "." -f 2`

# Create ALLOWED_RANGES.
COUNT=0
for RANGE in $DOMAIN_RANGES; do
    COUNT=`expr $COUNT "+" 1`
done;
if [ "$COUNT" -eq 1 ]; then
    for RANGE in $DOMAIN_RANGES; do
        DOMAIN_ADDR=`echo $RANGE | cut -d "/" -f 2`
        DOMAIN_CIDR=`echo $RANGE | cut -d "/" -f 3`
        SAMBA3_ALLOWED_RANGES="$SAMBA3_ALLOWED_RANGES $DOMAIN_ADDR/$DOMAIN_CIDR"
    done;
else
    COUNT=0
    for RANGE in $DOMAIN_RANGES; do
        DOMAIN_ADDR=`echo $RANGE | cut -d "/" -f 2`
        DOMAIN_CIDR=`echo $RANGE | cut -d "/" -f 3`
        if [ "$COUNT" -ne 0 ]; then
            SAMBA3_ALLOWED_RANGES="$SAMBA3_ALLOWED_RANGES $DOMAIN_ADDR/$DOMAIN_CIDR"
        fi;
        COUNT=`expr $COUNT "+" 1`
    done;
fi;

# Include state for this specific module.
if [ -e $TARGET_STATE/$SERVICE_IMPL.state ]; then
    . $TARGET_STATE/$SERVICE_IMPL.state
else
    mkdir -p $TARGET_STATE
    echo "SERVICE_STATE=uninitialized" > $TARGET_STATE/$SERVICE_IMPL.state
    . $TARGET_STATE/$SERVICE_IMPL.state
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
    mkdir -p $TARGET_ROOT/$SAMBA3_CONFIG_DIRECTORY
    mkdir -p $TARGET_ROOT/$SAMBA3_DATA_DIRECTORY
    mkdir -p $TARGET_ROOT/$SAMBA3_LOGGING_DIRECTORY
    mkdir -p $TARGET_ROOT/$SAMBA3_SPOOLER_DIRECTORY
    mkdir -p $TARGET_ROOT/$SAMBA3_NETLOGON_DIRECTORY
    mkdir -p $TARGET_ROOT/$SAMBA3_PROFILES_DIRECTORY
    mkdir -p $TARGET_ROOT/$SAMBA3_PASSDB_DIRECTORY

    cd $UNIVERSAL_HOME/Services/$SERVICE_NAME/$SERVICE_IMPL

    echo "Generating samba configuration files.."
    for FILE in $SAMBA3_FILES_CONFIG; do
        cat $FILE   | sed "s|SERVER_NAME|$SERVER_NAME|g" \
                    | sed "s|SERVER_MASK_FRONT|$SERVER_MASK_FRONT|g" \
                    | sed "s|SERVER_MASK_BACK|$SERVER_MASK_BACK|g" \
                    | sed "s|DOMAIN_NAME_PART_A|$DOMAIN_NAME_PART_A|g" \
                    | sed "s|DOMAIN_NAME_PART_B|$DOMAIN_NAME_PART_B|g" \
                    | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" \
                    | sed "s|SAMBA3_ALLOWED_RANGES|$SAMBA3_ALLOWED_RANGES|g" \
                    | sed "s|SAMBA3_CONFIG_DIRECTORY|$SAMBA3_CONFIG_DIRECTORY|g" \
                    | sed "s|SAMBA3_DATA_DIRECTORY|$SAMBA3_DATA_DIRECTORY|g" \
                    | sed "s|SAMBA3_LOGGING_DIRECTORY|$SAMBA3_LOGGING_DIRECTORY|g" \
                    | sed "s|SAMBA3_NETLOGON_DIRECTORY|$SAMBA3_NETLOGON_DIRECTORY|g" \
                    | sed "s|SAMBA3_PROFILES_DIRECTORY|$SAMBA3_PROFILES_DIRECTORY|g" \
                    | sed "s|SAMBA3_SPOOLER_DIRECTORY|$SAMBA3_SPOOLER_DIRECTORY|g" \
                    | sed "s|SAMBA3_PASSDB_BACKEND|$SAMBA3_PASSDB_BACKEND|g" \
                    > $TARGET_ROOT/$SAMBA3_CONFIG_DIRECTORY/$FILE
    done;

    # Check to see if centralized auth is wanted, act accordingly.
    if [ "$AUTH" = "basic" ]; then
        echo "Basic authentication requested, leaving smbldap-tools alone"
    elif [ "$AUTH" = "centralized" ]; then
        echo "Centralized authentication requested, setting up smbldap-tools!"
        # Do the 'net getlocalsid' using the generated smb.conf
        # Place the smbldap-tools templates
        # Replace passwd with smbldap-tools variant
        # Replace user[add/mod/del/info] and group[add/mod/del/info] /w smbldap-tools variants
        # During install phase, run 'smbldap-populate' using our configs.
    else
        echo "Eek, selected an authentication option I don't know about!"
        echo "You selected: $AUTH (in $REQUESTED_ROLE)"
        false
    fi;

    if [ $? = 0 ]; then
        echo "SERVICE_STATE=built" > $TARGET_STATE/$SERVICE_IMPL.state
    else
        echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE_IMPL.state
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

    if [ -e $TARGET_ROOT/$SAMBA3_CONFIG_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$SAMBA3_CONFIG_DIRECTORY"
        cd $TARGET_ROOT/$SAMBA3_CONFIG_DIRECTORY
        rm -vf $SAMBA3_FILES_CONFIG
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$SAMBA3_CONFIG_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $SAMBA3_CONFIG_DIRECTORY | sed 's|/| |g'`"
        while [ "$CLEAN_DIRS_EMPTY" != "$TARGET_ROOT" ]; do
            cd $CLEAN_DIRS_EMPTY/..
            rmdir $CLEAN_DIRS_NAMES > /dev/null 2>&1 < /dev/null
            CLEAN_DIRS_EMPTY=`pwd`
        done;

        if [ $? = 0 ]; then
            echo "SERVICE_STATE=cleaned" > $TARGET_STATE/$SERVICE_IMPL.state
        else
            echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE_IMPL.state
        fi;
    else
        echo "No created config directory found. use \"build\" first. "
    fi;

    if [ -e $TARGET_ROOT/$SAMBA3_DATA_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$SAMBA3_DATA_DIRECTORY"
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$SAMBA3_DATA_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $SAMBA3_DATA_DIRECTORY | sed 's|/| |g'`"
        while [ "$CLEAN_DIRS_EMPTY" != "$TARGET_ROOT" ]; do
            cd $CLEAN_DIRS_EMPTY/..
            rmdir $CLEAN_DIRS_NAMES > /dev/null 2>&1 < /dev/null
            CLEAN_DIRS_EMPTY=`pwd`
        done;

        if [ $? = 0 ]; then
            echo "SERVICE_STATE=cleaned" > $TARGET_STATE/$SERVICE_IMPL.state
        else
            echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE_IMPL.state
        fi;
    else
        echo "No created data directory found. use \"build\" first. "
    fi;

    if [ -e $TARGET_ROOT/$SAMBA3_NETLOGON_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$SAMBA3_NETLOGON_DIRECTORY"
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$SAMBA3_NETLOGON_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $SAMBA3_NETLOGON_DIRECTORY | sed 's|/| |g'`"
        while [ "$CLEAN_DIRS_EMPTY" != "$TARGET_ROOT" ]; do
            cd $CLEAN_DIRS_EMPTY/..
            rmdir $CLEAN_DIRS_NAMES > /dev/null 2>&1 < /dev/null
            CLEAN_DIRS_EMPTY=`pwd`
        done;

        if [ $? = 0 ]; then
            echo "SERVICE_STATE=cleaned" > $TARGET_STATE/$SERVICE_IMPL.state
        else
            echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE_IMPL.state
        fi;
    else
        echo "No created netlogon directory found. use \"build\" first. "
    fi;

    if [ -e $TARGET_ROOT/$SAMBA3_LOGGING_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$SAMBA3_LOGGING_DIRECTORY"
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$SAMBA3_LOGGING_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $SAMBA3_LOGGING_DIRECTORY | sed 's|/| |g'`"
        while [ "$CLEAN_DIRS_EMPTY" != "$TARGET_ROOT" ]; do
            cd $CLEAN_DIRS_EMPTY/..
            rmdir $CLEAN_DIRS_NAMES > /dev/null 2>&1 < /dev/null
            CLEAN_DIRS_EMPTY=`pwd`
        done;

        if [ $? = 0 ]; then
            echo "SERVICE_STATE=cleaned" > $TARGET_STATE/$SERVICE_IMPL.state
        else
            echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE_IMPL.state
        fi;
    else
        echo "No created logging directory found. use \"build\" first. "
    fi;

    if [ -e $TARGET_ROOT/$SAMBA3_PROFILES_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$SAMBA3_PROFILES_DIRECTORY"
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$SAMBA3_PROFILES_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $SAMBA3_PROFILES_DIRECTORY | sed 's|/| |g'`"
        while [ "$CLEAN_DIRS_EMPTY" != "$TARGET_ROOT" ]; do
            cd $CLEAN_DIRS_EMPTY/..
            rmdir $CLEAN_DIRS_NAMES > /dev/null 2>&1 < /dev/null
            CLEAN_DIRS_EMPTY=`pwd`
        done;

        if [ $? = 0 ]; then
            echo "SERVICE_STATE=cleaned" > $TARGET_STATE/$SERVICE_IMPL.state
        else
            echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE_IMPL.state
        fi;
    else
        echo "No created profiles directory found. use \"build\" first. "
    fi;

    if [ -e $TARGET_ROOT/$SAMBA3_SPOOLER_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$SAMBA3_SPOOLER_DIRECTORY"
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$SAMBA3_SPOOLER_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $SAMBA3_SPOOLER_DIRECTORY | sed 's|/| |g'`"
        while [ "$CLEAN_DIRS_EMPTY" != "$TARGET_ROOT" ]; do
            cd $CLEAN_DIRS_EMPTY/..
            rmdir $CLEAN_DIRS_NAMES > /dev/null 2>&1 < /dev/null
            CLEAN_DIRS_EMPTY=`pwd`
        done;

        if [ $? = 0 ]; then
            echo "SERVICE_STATE=cleaned" > $TARGET_STATE/$SERVICE_IMPL.state
        else
            echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE_IMPL.state
        fi;
    else
        echo "No created spooler directory found. use \"build\" first. "
    fi;

    if [ -e $TARGET_ROOT/$SAMBA3_PASSDB_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$SAMBA3_PASSDB_DIRECTORY"
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$SAMBA3_PASSDB_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $SAMBA3_PASSDB_DIRECTORY | sed 's|/| |g'`"
        while [ "$CLEAN_DIRS_EMPTY" != "$TARGET_ROOT" ]; do
            cd $CLEAN_DIRS_EMPTY/..
            rmdir $CLEAN_DIRS_NAMES > /dev/null 2>&1 < /dev/null
            CLEAN_DIRS_EMPTY=`pwd`
        done;

        if [ $? = 0 ]; then
            echo "SERVICE_STATE=cleaned" > $TARGET_STATE/$SERVICE_IMPL.state
        else
            echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE_IMPL.state
        fi;
    else
        echo "No created passdb directory found. use \"build\" first. "
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

    if [ -e $TARGET_ROOT/$SAMBA3_CONFIG_DIRECTORY ]; then
        echo "Making sure our samba directories exist.."
        mkdir -p $SAMBA3_CONFIG_DIRECTORY
        mkdir -p $SAMBA3_DATA_DIRECTORY
        mkdir -p $SAMBA3_LOGGING_DIRECTORY
        mkdir -p $SAMBA3_SPOOLER_DIRECTORY
        mkdir -p $SAMBA3_NETLOGON_DIRECTORY
        mkdir -p $SAMBA3_PROFILES_DIRECTORY
        mkdir -p $SAMBA3_PASSDB_DIRECTORY

        echo "Removing install-time generated tdb databases if any.."
        rm -v $SAMBA3_PASSDB_DIRECTORY/*.tdb

        echo "Installing generated configuration:"
        for CONFIG in $SAMBA3_FILES_CONFIG; do
            if [ -e $SAMBA3_CONFIG_DIRECTORY/$CONFIG ]; then
                if [ ! -e $SAMBA3_CONFIG_DIRECTORY/$CONFIG.orig ]; then
                    cp $SAMBA3_CONFIG_DIRECTORY/$CONFIG $SAMBA3_CONFIG_DIRECTORY/$CONFIG.orig
                fi;
            fi;
            echo -n " * $CONFIG: ";
            install -m 0755 -o root $TARGET_ROOT/$SAMBA3_CONFIG_DIRECTORY/$CONFIG $SAMBA3_CONFIG_DIRECTORY/$CONFIG

            if [ $? = 0 ]; then
                echo "success"
                echo "SERVICE_STATE=installed" > $TARGET_STATE/$SERVICE_IMPL.state
            else
                STATE="fail"
                echo "$STATE"
                echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE_IMPL.state
            fi;
        done;
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

    if [ -e $SAMBA3_CONFIG_DIRECTORY ]; then
        echo "Uninstalling generated configuration.."
        for CONFIG in $SAMBA3_FILES_CONFIG; do
            if [ -e $SAMBA3_CONFIG_DIRECTORY/$CONFIG.orig ]; then
		cp $SAMBA3_CONFIG_DIRECTORY/$CONFIG.orig $SAMBA3_CONFIG_DIRECTORY/$CONFIG
            else
                rm -f $SAMBA3_CONFIG_DIRECTORY/$CONFIG
            fi;
        done;
        
        echo "Removing samba directories if empty.."
        rmdir $SAMBA3_CONFIG_DIRECTORY
        rmdir $SAMBA3_DATA_DIRECTORY
        rmdir $SAMBA3_LOGGING_DIRECTORY
        rmdir $SAMBA3_SPOOLER_DIRECTORY
        rmdir $SAMBA3_NETLOGON_DIRECTORY
        rmdir $SAMBA3_PROFILES_DIRECTORY
        rmdir $SAMBA3_PASSDB_DIRECTORY

        if [ $? = 0 ]; then
            echo "SERVICE_STATE=uninstalled" > $TARGET_STATE/$SERVICE_IMPL.state
        else
            echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE_IMPL.state
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
        if [ "$SAMBA3_INIT_STRING" ]; then
            if [ -e $SAMBA3_INIT_CONFIG ]; then
                if [ ! -e $SAMBA3_INIT_CONFIG.orig ]; then
                    cp $SAMBA3_INIT_CONFIG $SAMBA3_INIT_CONFIG.orig
                elif [ -e $SAMBA3_INIT_CONFIG.orig ]; then  
                    cp $SAMBA3_INIT_CONFIG.orig $SAMBA3_INIT_CONFIG
                fi;
            else
                echo "Mmm, your specified config file does not seem to exist:"
                echo "SAMBA3_INIT_CONFIG=$SAMBA3_INIT_CONFIG"
                echo ""
                echo "I will create it for you using SAMBA3_INIT_STRING as its content"
                echo "But I can't tell if this will be sufficient. Just so you know.."
                echo ""
            fi;
            echo "Appending SAMBA3_INIT_STRING to $SAMBA3_INIT_CONFIG.."
            echo "$SAMBA3_INIT_STRING" >> $SAMBA3_INIT_CONFIG
        fi;

        COUNT=$SERVICE_PRIO
        for SAMBA3_INIT_NAME in $SAMBA3_INIT_SCRIPTS; do
            if [ $PLATFORM_INIT_TYPE = "bsd" ]; then
                echo "Specified init type is $PLATFORM_INIT_TYPE. Acting accordingly.."
                if [ -e $PLATFORM_INIT_DIRECTORY/$SAMBA3_INIT_NAME ]; then
                    echo "Enabling boot-time startup for $SAMBA3_INIT_NAME in /etc/rc.conf."
                    echo "$SAMBA3_INIT_NAME=YES" >> /etc/rc.conf
                else
                    echo "No $SAMBA3_INIT_NAME init script(s) found!"
                    echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                    echo "init script(s): $SAMBA3_INIT_SCRIPTS"
                fi;
            elif [ $PLATFORM_INIT_TYPE = "sysv" ]; then
                echo "Specified init type is $PLATFORM_INIT_TYPE. Acting accordingly.."
                if [ -e $PLATFORM_INIT_DIRECTORY/$SAMBA3_INIT_NAME ]; then
                    echo "Enabling boot-time startup for $SAMBA3_INIT_NAME in runlevels 2,3,4,5."
                    
                    for RUNLEVEL in 2 3 4 5; do
                        rm -f $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/*$SAMBA3_INIT_NAME
                        ln -s $PLATFORM_INIT_DIRECTORY/$SAMBA3_INIT_NAME $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/S$COUNT$SAMBA3_INIT_NAME
                    done;
                    for RUNLEVEL in 0 1 6; do
                        rm -f $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/*$SAMBA3_INIT_NAME
                        ln -s $PLATFORM_INIT_DIRECTORY/$SAMBA3_INIT_NAME $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/K$COUNT$SAMBA3_INIT_NAME
                    done;
                else
                    echo "No $SAMBA3_INIT_NAME init script(s) found!"
                    echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                    echo "init script(s): $SAMBA3_INIT_SCRIPTS"
                fi;
            else
                echo "The variable PLATFORM_INIT_TYPE was set to \"$PLATFORM_INIT_TYPE\";"
                echo "This is currently not supported. It has to be either bsd or sysv!"
            fi;
        
            # And start it up.
            echo "Starting up $SAMBA3_INIT_NAME.."
            if [ -e $PLATFORM_INIT_DIRECTORY/$SAMBA3_INIT_NAME ]; then
                $PLATFORM_INIT_DIRECTORY/$SAMBA3_INIT_NAME start
            else
                echo "No $SAMBA3_INIT_NAME init script(s) found!"
                echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                echo "init script(s): $SAMBA3_INIT_SCRIPTS"
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
    if [ "$SAMBA3_INIT_STRING" ]; then
        if [ -e $SAMBA3_INIT_CONFIG.orig ]; then
            cp $SAMBA3_INIT_CONFIG.orig $SAMBA3_INIT_CONFIG
        fi;
    fi;

    for SAMBA3_INIT_NAME in $SAMBA3_INIT_SCRIPTS; do
        if [ $PLATFORM_INIT_TYPE = "bsd" ]; then
            if [ -e $PLATFORM_INIT_DIRECTORY/$SAMBA3_INIT_NAME ]; then
                echo "$SAMBA3_INIT_NAME=NO" >> /etc/rc.conf
            else
                echo "No $SAMBA3_INIT_NAME init script(s) found!"
                echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                echo "init script(s): $SAMBA3_INIT_SCRIPTS"
            fi;
        elif [ $PLATFORM_INIT_TYPE = "sysv" ]; then
            if [ -e $PLATFORM_INIT_DIRECTORY/$SAMBA3_INIT_NAME ]; then
                for RUNLEVEL in 0 1 2 3 4 5 6; do
                    rm -f $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/*$SAMBA3_INIT_NAME
                done;
            else
                echo "No $SAMBA3_INIT_NAME init script(s) found!"
                echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                echo "init script(s): $SAMBA3_INIT_SCRIPTS"
            fi;
        else
            echo "The variable PLATFORM_INIT_TYPE was set to \"$PLATFORM_INIT_TYPE\";"
            echo "This is currently not supported. It has to be either bsd or sysv!"
        fi;

        # And stop the service.
        echo "Stopping $SAMBA3_INIT_NAME.."
        if [ -e $PLATFORM_INIT_DIRECTORY/$SAMBA3_INIT_NAME ]; then
            $PLATFORM_INIT_DIRECTORY/$SAMBA3_INIT_NAME stop
        else
            echo "No $SAMBA3_INIT_NAME init script(s) found!"
            echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
            echo "init script(s): $SAMBA3_INIT_SCRIPTS"
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
    if [ "$SAMBA3_PACKAGES_REQUIRED" = 1 ]; then
        echo "$SAMBA3_PACKAGES_LISTING"
    fi;
    ;;

    *)
    echo $"Usage: $0 {build|clean|install|uninstall|start|stop|state|deps}"
    exit 1
esac
