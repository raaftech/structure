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
SERVICE_DEPS="$APACHE2_SERVICE_DEPS"
SERVICE_PRIO="24"
SERVICE_PORT="80 443"

# Check if globals we care about are set.
NAMESPACE=APACHE2
CHECK="`set | grep ^${NAMESPACE}_`"
if [ -z "$CHECK" ]; then
    mkdir -p $TARGET_STATE
    echo "SERVICE_STATE=failed" > $TARGET_STATE/$SERVICE.state
fi;
unset CHECK
unset NAMESPACE

# What are we working with?
APACHE2_FILES_CONFIG="httpd.conf ssl.conf mime.magic mime.types"
APACHE2_FILES_MODULES="modules.conf"
APACHE2_FILES_SITES="www.DOMAIN_NAME.conf repo.DOMAIN_NAME.conf" 

# We only care about the first (tld) domain within DOMAIN_RANGES.
DOMAIN_NAME=`echo $DOMAIN_RANGES | cut -d "/" -f 1`

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
    
    # Make sure our target directories exist.
    mkdir -p $TARGET_ROOT/$APACHE2_CONFIG_DIRECTORY
    mkdir -p $TARGET_ROOT/$APACHE2_CONFIG_DIRECTORY/sites
    mkdir -p $TARGET_ROOT/$APACHE2_DATA_DIRECTORY
    mkdir -p $TARGET_ROOT/$APACHE2_LOGGING_DIRECTORY
    mkdir -p $TARGET_ROOT/$APACHE2_STATE_DIRECTORY
    mkdir -p $TARGET_ROOT/$APACHE2_CGIBIN_DIRECTORY
    mkdir -p $TARGET_ROOT/$APACHE2_ICONS_DIRECTORY
    for SITE in `echo $APACHE2_FILES_SITES | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" | sed "s|.conf||g"`; do
        mkdir -p $TARGET_ROOT/$APACHE2_SITES_DIRECTORY/$SITE
    done;

    # Make sure our service dependencies are built first.
    if [ $SERVICE_DEPS ]; then
        echo "This service depends on other services to be configured first."
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
                $UNIVERSAL_HOME/Services/$SERVICE_DEP_NAME/$SERVICE_DEP_IMPL/run.sh build > /dev/null 2>&1 < /dev/null
                SERVICE_DEP_STATE=`$UNIVERSAL_HOME/Services/$SERVICE_DEP_NAME/$SERVICE_DEP_IMPL/run.sh state`
                echo $SERVICE_DEP_STATE
            fi;
        done;
    fi;

    cd $UNIVERSAL_HOME/Services/$SERVICE

    echo "Creating configuration files in \$TARGET_ROOT$APACHE2_CONFIG_DIRECTORY.."
    for FILE in $APACHE2_FILES_CONFIG; do
        cat $FILE   | sed "s|SERVER_NAME|$SERVER_NAME|g" \
                    | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" \
                    | sed "s|APACHE2_CONFIG_DIRECTORY|$APACHE2_CONFIG_DIRECTORY|g" \
                    | sed "s|APACHE2_DATA_DIRECTORY|$APACHE2_DATA_DIRECTORY|g" \
                    | sed "s|APACHE2_LOGGING_DIRECTORY|$APACHE2_LOGGING_DIRECTORY|g" \
                    | sed "s|APACHE2_MODULES_DIRECTORY|$APACHE2_MODULES_DIRECTORY|g" \
                    | sed "s|APACHE2_STATE_DIRECTORY|$APACHE2_STATE_DIRECTORY|g" \
                    | sed "s|APACHE2_CGIBIN_DIRECTORY|$APACHE2_CGIBIN_DIRECTORY|g" \
                    | sed "s|APACHE2_ICONS_DIRECTORY|$APACHE2_ICONS_DIRECTORY|g" \
                    | sed "s|APACHE2_SITES_DIRECTORY|$APACHE2_SITES_DIRECTORY|g" \
                    | sed "s|APACHE2_DAEMON_USER|$APACHE2_DAEMON_USER|g" \
                    | sed "s|APACHE2_DAEMON_GROUP|$APACHE2_DAEMON_GROUP|g" \
                    | sed "s|CERTIFICATES_DATA_DIRECTORY|$CERTIFICATES_DATA_DIRECTORY|g" \
                    > $TARGET_ROOT/$APACHE2_CONFIG_DIRECTORY/$FILE
    done;
    
    echo "Creating platform specific module configuration.."
    for FILE in $APACHE2_FILES_MODULES-`echo $REQUESTED_PLATFORM$REQUESTED_MAJOR`; do
        NEWFILE="`echo $FILE | sed "s|-$REQUESTED_PLATFORM$REQUESTED_MAJOR||g"`"
        cat $FILE   | sed "s|SERVER_NAME|$SERVER_NAME|g" \
                    | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" \
                    | sed "s|APACHE2_CONFIG_DIRECTORY|$APACHE2_CONFIG_DIRECTORY|g" \
                    | sed "s|APACHE2_DATA_DIRECTORY|$APACHE2_DATA_DIRECTORY|g" \
                    | sed "s|APACHE2_LOGGING_DIRECTORY|$APACHE2_LOGGING_DIRECTORY|g" \
                    | sed "s|APACHE2_MODULES_DIRECTORY|$APACHE2_MODULES_DIRECTORY|g" \
                    | sed "s|APACHE2_STATE_DIRECTORY|$APACHE2_STATE_DIRECTORY|g" \
                    | sed "s|APACHE2_CGIBIN_DIRECTORY|$APACHE2_CGIBIN_DIRECTORY|g" \
                    | sed "s|APACHE2_ICONS_DIRECTORY|$APACHE2_ICONS_DIRECTORY|g" \
                    | sed "s|APACHE2_SITES_DIRECTORY|$APACHE2_SITES_DIRECTORY|g" \
                    | sed "s|APACHE2_DAEMON_USER|$APACHE2_DAEMON_USER|g" \
                    | sed "s|APACHE2_DAEMON_GROUP|$APACHE2_DAEMON_GROUP|g" \
                    | sed "s|CERTIFICATES_DATA_DIRECTORY|$CERTIFICATES_DATA_DIRECTORY|g" \
                    > $TARGET_ROOT/$APACHE2_CONFIG_DIRECTORY/$NEWFILE
    done;

    echo "Populating sites subdirectory with generated coniguration files.."
    for FILE in $APACHE2_FILES_SITES; do
        NEWFILE="`echo $FILE | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g"`"
        cat $FILE   | sed "s|SERVER_NAME|$SERVER_NAME|g" \
                    | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" \
                    | sed "s|APACHE2_CONFIG_DIRECTORY|$APACHE2_CONFIG_DIRECTORY|g" \
                    | sed "s|APACHE2_DATA_DIRECTORY|$APACHE2_DATA_DIRECTORY|g" \
                    | sed "s|APACHE2_LOGGING_DIRECTORY|$APACHE2_LOGGING_DIRECTORY|g" \
                    | sed "s|APACHE2_MODULES_DIRECTORY|$APACHE2_MODULES_DIRECTORY|g" \
                    | sed "s|APACHE2_STATE_DIRECTORY|$APACHE2_STATE_DIRECTORY|g" \
                    | sed "s|APACHE2_CGIBIN_DIRECTORY|$APACHE2_CGIBIN_DIRECTORY|g" \
                    | sed "s|APACHE2_ICONS_DIRECTORY|$APACHE2_ICONS_DIRECTORY|g" \
                    | sed "s|APACHE2_SITES_DIRECTORY|$APACHE2_SITES_DIRECTORY|g" \
                    | sed "s|APACHE2_DAEMON_USER|$APACHE2_DAEMON_USER|g" \
                    | sed "s|APACHE2_DAEMON_GROUP|$APACHE2_DAEMON_GROUP|g" \
                    | sed "s|CERTIFICATES_DATA_DIRECTORY|$CERTIFICATES_DATA_DIRECTORY|g" \
                    > $TARGET_ROOT/$APACHE2_CONFIG_DIRECTORY/sites/$NEWFILE
    done;
    
    # Create a link to apache2.conf on Debian..
    if [ $REQUESTED_PLATFORM = debian ]; then
        echo "Creating a link to apache2.conf.."
        cd $TARGET_ROOT/$APACHE2_CONFIG_DIRECTORY
        ln -s httpd.conf apache2.conf
        cd -
    fi;

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

    if [ -e $TARGET_ROOT/$APACHE2_CONFIG_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$APACHE2_CONFIG_DIRECTORY"
        cd $TARGET_ROOT/$APACHE2_CONFIG_DIRECTORY
        rm -vf $APACHE2_FILES_CONFIG apache2.conf
        rm -vf $APACHE2_FILES_MODULES
        rm -vf sites/*
        rmdir sites
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$APACHE2_CONFIG_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $APACHE2_CONFIG_DIRECTORY | sed 's|/| |g'`"
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
    
    if [ -e $TARGET_ROOT/$APACHE2_DATA_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$APACHE2_DATA_DIRECTORY"
        cd $TARGET_ROOT/$APACHE2_DATA_DIRECTORY
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$APACHE2_DATA_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $APACHE2_DATA_DIRECTORY | sed 's|/| |g'`"
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
    
    if [ -e $TARGET_ROOT/$APACHE2_LOGGING_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$APACHE2_LOGGING_DIRECTORY"
        cd $TARGET_ROOT/$APACHE2_LOGGING_DIRECTORY
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$APACHE2_LOGGING_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $APACHE2_LOGGING_DIRECTORY | sed 's|/| |g'`"
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
        echo "No created logging directory found. use \"build\" first. "
    fi;

    if [ -e $TARGET_ROOT/$APACHE2_STATE_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$APACHE2_STATE_DIRECTORY"
        cd $TARGET_ROOT/$APACHE2_STATE_DIRECTORY
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$APACHE2_STATE_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $APACHE2_STATE_DIRECTORY | sed 's|/| |g'`"
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
        echo "No created state directory found. use \"build\" first. "
    fi;

    if [ -e $TARGET_ROOT/$APACHE2_CGIBIN_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$APACHE2_CGIBIN_DIRECTORY"
        cd $TARGET_ROOT/$APACHE2_CGIBIN_DIRECTORY
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$APACHE2_CGIBIN_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $APACHE2_CGIBIN_DIRECTORY | sed 's|/| |g'`"
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
        echo "No created cgi-bin directory found. use \"build\" first. "
    fi;

    if [ -e $TARGET_ROOT/$APACHE2_ICONS_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$APACHE2_ICONS_DIRECTORY"
        cd $TARGET_ROOT/$APACHE2_ICONS_DIRECTORY
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$APACHE2_ICONS_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $APACHE2_ICONS_DIRECTORY | sed 's|/| |g'`"
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
        echo "No created icons directory found. use \"build\" first. "
    fi;

    if [ -e $TARGET_ROOT/$APACHE2_SITES_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$APACHE2_SITES_DIRECTORY"
        cd $TARGET_ROOT/$APACHE2_SITES_DIRECTORY
    rmdir `echo $APACHE2_FILES_SITES | sed "s|DOMAIN_NAME.conf|$DOMAIN_NAME|g"`
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$APACHE2_SITES_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $APACHE2_SITES_DIRECTORY | sed 's|/| |g'`"
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
        echo "No created sites directory found. use \"build\" first. "
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

    if [ -e $TARGET_ROOT/$APACHE2_CONFIG_DIRECTORY ]; then
        echo "Creating base config directory, if needed.."
        install -d -m 0755 -o root $APACHE2_CONFIG_DIRECTORY
        install -d -m 0755 -o root $APACHE2_CONFIG_DIRECTORY/backup

        echo "Making sure the target config directory is empty, moving any existing files to backup.d.."
        mv $APACHE2_CONFIG_DIRECTORY/* $APACHE2_CONFIG_DIRECTORY/backup/

        echo "Creating base directory layout.."
        install -d -m 0755 -o root $APACHE2_CONFIG_DIRECTORY/sites
        install -d -m 0755 -o root $APACHE2_DATA_DIRECTORY
        install -d -m 0755 -o root $APACHE2_LOGGING_DIRECTORY
        install -d -m 0755 -o root $APACHE2_STATE_DIRECTORY
        install -d -m 0755 -o root $APACHE2_CGIBIN_DIRECTORY
        install -d -m 0755 -o root $APACHE2_ICONS_DIRECTORY

        echo "Creating sites directories for virtual hosts.."
        install -d -m 0755 -o root $APACHE2_SITES_DIRECTORY
        for SITE in `echo $APACHE2_FILES_SITES | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" | sed "s|.conf||g"`; do
            install -d -m 0755 -o root $APACHE2_SITES_DIRECTORY/$SITE
        done;

        echo "Installing main configuration files:"
        for CONFIG in $APACHE2_FILES_CONFIG; do
            echo -n " * $CONFIG: ";
            install -m 0755 -o root $TARGET_ROOT/$APACHE2_CONFIG_DIRECTORY/$CONFIG $APACHE2_CONFIG_DIRECTORY/$CONFIG

            # Disable LoadModule if _MODULAR_BACKEND=0.. Usually only sane for ssl.conf.
            if [ $APACHE2_MODULAR_BACKEND = "0" ]; then
                cp $APACHE2_CONFIG_DIRECTORY/$CONFIG /tmp/$CONFIG.tmp
                cat /tmp/$CONFIG.tmp | sed "s|^LoadModule|#LoadModule|g" > $APACHE2_CONFIG_DIRECTORY/$CONFIG
                rm /tmp/$CONFIG.tmp
            fi;

            if [ $? = 0 ]; then
                echo "success"
                echo "SERVICE_STATE=installed" > $TARGET_STATE/$SERVICE.state
            else
                STATE="fail"
                echo "$STATE"
                echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE.state
            fi;
        done;

        # Create a link to apache2.conf on Debian..
        if [ $REQUESTED_PLATFORM = debian ]; then
            echo "Creating a link to apache2.conf.."
            cd $APACHE2_CONFIG_DIRECTORY
            ln -s httpd.conf apache2.conf
            cd -
            if [ -e $APACHE2_CONFIG_DIRECTORY/backup/envvars ]; then
                echo "Found an envvars file, copying to $APACHE2_CONFIG_DIRECTORY"
                cp $APACHE2_CONFIG_DIRECTORY/backup/envvars $APACHE2_CONFIG_DIRECTORY/envvars
            fi;
        fi;


        echo "Installing platform specific module configuration files:"
        for CONFIG in $APACHE2_FILES_MODULES; do
            echo -n " * $CONFIG: ";
            install -m 0755 -o root $TARGET_ROOT/$APACHE2_CONFIG_DIRECTORY/$CONFIG $APACHE2_CONFIG_DIRECTORY/$CONFIG

            if [ $? = 0 ]; then
                echo "success"
                echo "SERVICE_STATE=installed" > $TARGET_STATE/$SERVICE.state
            else
                STATE="fail"
                echo "$STATE"
                echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE.state
            fi;
        done;

        echo "Installing site configuration files:"
        for CONFIG in `echo $APACHE2_FILES_SITES | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g"`; do
            echo -n " * $CONFIG: ";
            install -m 0755 -o root $TARGET_ROOT/$APACHE2_CONFIG_DIRECTORY/sites/$CONFIG $APACHE2_CONFIG_DIRECTORY/sites/$CONFIG

            if [ $? = 0 ]; then
                echo "success"
                echo "SERVICE_STATE=installed" > $TARGET_STATE/$SERVICE.state
            else
                STATE="fail"
                echo "$STATE"
                echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE.state
            fi;
        done;
    else
        echo "No generated configuration found. use \"build\" first.."
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

    echo "Removing main config.."
    for CONFIG in $APACHE2_FILES_CONFIG; do
        rm -f $APACHE2_CONFIG_DIRECTORY/$CONFIG
    done;

    if [ $REQUESTED_PLATFORM = debian ]; then
        rm -f $APACHE2_CONFIG_DIRECTORY/apache2.conf
    fi;

    echo "Removing module config.."
    for CONFIG in $APACHE2_FILES_MODULES; do
        rm -f $APACHE2_CONFIG_DIRECTORY/$CONFIG
    done;

    echo "Removing sites config.."
    for CONFIG in `echo $APACHE2_FILES_SITES | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g"`; do
        rm -f $APACHE2_CONFIG_DIRECTORY/sites/$CONFIG
    done;
    
    echo "Removing directories if empty.."
    rmdir $APACHE2_CONFIG_DIRECTORY/sites > /dev/null 2>&1 < /dev/null
    rmdir $APACHE2_CONFIG_DIRECTORY > /dev/null 2>&1 < /dev/null
    rmdir $APACHE2_DATA_DIRECTORY > /dev/null 2>&1 < /dev/null
    rmdir $APACHE2_LOGGING_DIRECTORY > /dev/null 2>&1 < /dev/null
    rmdir $APACHE2_STATE_DIRECTORY > /dev/null 2>&1 < /dev/null
    rmdir $APACHE2_CGIBIN_DIRECTORY > /dev/null 2>&1 < /dev/null
    rmdir $APACHE2_ICONS_DIRECTORY > /dev/null 2>&1 < /dev/null

    for SITE in `echo $APACHE2_FILES_SITES | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" | sed "s|.conf||g"`; do
        rmdir $APACHE2_SITES_DIRECTORY/$SITE
    done;
    rmdir $APACHE2_SITES_DIRECTORY

    echo "Restoring configuration file originals if any.."
    mv $APACHE2_CONFIG_DIRECTORY/backup/* $APACHE2_CONFIG_DIRECTORY
    rmdir $APACHE2_CONFIG_DIRECTORY/backup

    if [ $? = 0 ]; then
        echo "SERVICE_STATE=uninstalled" > $TARGET_STATE/$SERVICE.state
    else
        echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE.state
    fi;
    echo ""
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
        if [ "$APACHE2_INIT_STRING" ]; then
            if [ -e $APACHE2_INIT_CONFIG ]; then
                if [ ! -e $APACHE2_INIT_CONFIG.orig ]; then
                    cp $APACHE2_INIT_CONFIG $APACHE2_INIT_CONFIG.orig
                elif [ -e $APACHE2_INIT_CONFIG.orig ]; then  
                    cp $APACHE2_INIT_CONFIG.orig $APACHE2_INIT_CONFIG
                fi;
            else
                echo "Mmm, your specified config file does not seem to exist:"
                echo "APACHE2_INIT_CONFIG=$APACHE2_INIT_CONFIG"
                echo ""
                echo "I will create it for you using APACHE2_INIT_STRING as its content"
                echo "But I can't tell if this will be sufficient. Just so you know.."
                echo ""
            fi;
            echo "Appending APACHE2_INIT_STRING to $APACHE2_INIT_CONFIG.."
            echo "$APACHE2_INIT_STRING" >> $APACHE2_INIT_CONFIG
        fi;

        COUNT=$SERVICE_PRIO
        for APACHE2_INIT_NAME in $APACHE2_INIT_SCRIPTS; do
            if [ $PLATFORM_INIT_TYPE = "bsd" ]; then
                echo "Specified init type is $PLATFORM_INIT_TYPE. Acting accordingly.."
                if [ -e $PLATFORM_INIT_DIRECTORY/$APACHE2_INIT_NAME ]; then
                    echo "Enabling boot-time startup for $APACHE2_INIT_NAME in /etc/rc.conf."
                    echo "$APACHE2_INIT_NAME=YES" >> /etc/rc.conf
                else
                    echo "No $APACHE2_INIT_NAME init script(s) found!"
                    echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                    echo "init script(s): $APACHE2_INIT_SCRIPTS"
                fi;
            elif [ $PLATFORM_INIT_TYPE = "sysv" ]; then
                echo "Specified init type is $PLATFORM_INIT_TYPE. Acting accordingly.."
                if [ -e $PLATFORM_INIT_DIRECTORY/$APACHE2_INIT_NAME ]; then
                    echo "Enabling boot-time startup for $APACHE2_INIT_NAME in runlevels 2,3,4,5."
                    for RUNLEVEL in 2 3 4 5; do
                        rm -f $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/*$APACHE2_INIT_NAME
                        ln -s $PLATFORM_INIT_DIRECTORY/$APACHE2_INIT_NAME $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/S$COUNT$APACHE2_INIT_NAME
                    done;
                    for RUNLEVEL in 0 1 6; do
                        rm -f $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/*$APACHE2_INIT_NAME
                        ln -s $PLATFORM_INIT_DIRECTORY/$APACHE2_INIT_NAME $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/K$COUNT$APACHE2_INIT_NAME
                    done;
                else
                    echo "No $APACHE2_INIT_NAME init script(s) found!"
                    echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                    echo "init script(s): $APACHE2_INIT_SCRIPTS"
                fi;
            else
                echo "The variable PLATFORM_INIT_TYPE was set to \"$PLATFORM_INIT_TYPE\";"
                echo "This is currently not supported. It has to be either bsd or sysv!"
            fi;
        
            # And start it up.
            echo "Starting up $APACHE2_INIT_NAME.."
            if [ -e $PLATFORM_INIT_DIRECTORY/$APACHE2_INIT_NAME ]; then
                $PLATFORM_INIT_DIRECTORY/$APACHE2_INIT_NAME start
            else
                echo "No $APACHE2_INIT_NAME init script(s) found!"
                echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                echo "init script(s): $APACHE2_INIT_SCRIPTS"
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
    if [ "$APACHE2_INIT_STRING" ]; then
        if [ -e $APACHE2_INIT_CONFIG.orig ]; then
            cp $APACHE2_INIT_CONFIG.orig $APACHE2_INIT_CONFIG
        fi;
    fi;

    for APACHE2_INIT_NAME in $APACHE2_INIT_SCRIPTS; do
        if [ $PLATFORM_INIT_TYPE = "bsd" ]; then
            if [ -e $PLATFORM_INIT_DIRECTORY/$APACHE2_INIT_NAME ]; then
                echo "$APACHE2_INIT_NAME=NO" >> /etc/rc.conf
            else
                echo "No $APACHE2_INIT_NAME init script(s) found!"
                echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                echo "init script(s): $APACHE2_INIT_SCRIPTS"
            fi;
        elif [ $PLATFORM_INIT_TYPE = "sysv" ]; then
            if [ -e $PLATFORM_INIT_DIRECTORY/$APACHE2_INIT_NAME ]; then
                for RUNLEVEL in 0 1 2 3 4 5 6; do
                    rm -f $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/*$APACHE2_INIT_NAME
                done;
            else
                echo "No $APACHE2_INIT_NAME init script(s) found!"
                echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                echo "init script(s): $APACHE2_INIT_SCRIPTS"
            fi;
        else
            echo "The variable PLATFORM_INIT_TYPE was set to \"$PLATFORM_INIT_TYPE\";"
            echo "This is currently not supported. It has to be either bsd or sysv!"
        fi;

        # And stop the service.
        echo "Stopping $APACHE2_INIT_NAME.."
        if [ -e $PLATFORM_INIT_DIRECTORY/$APACHE2_INIT_NAME ]; then
            $PLATFORM_INIT_DIRECTORY/$APACHE2_INIT_NAME stop
        else
            echo "No $APACHE2_INIT_NAME init script(s) found!"
            echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
            echo "init script(s): $APACHE2_INIT_SCRIPTS"
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
    if [ "$APACHE2_PACKAGES_REQUIRED" = 1 ]; then
        echo "$APACHE2_PACKAGES_LISTING"
    fi;
    ;;

    *)
    echo $"Usage: $0 {build|clean|install|uninstall|start|stop|state|deps}"
    exit 1
esac
