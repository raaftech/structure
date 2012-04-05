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
SERVICE_DEPS="$TRAC0_SERVICE_DEPS"
SERVICE_PRIO="25"
SERVICE_PORT=""

# Check if globals we care about are set.
NAMESPACE=TRAC0
CHECK="`set | grep ^${NAMESPACE}_`"
if [ -z "$CHECK" ]; then
    mkdir -p $TARGET_STATE
    echo "SERVICE_STATE=failed" > $TARGET_STATE/$SERVICE.state
fi;
unset CHECK
unset NAMESPACE

# What are we working with?
TRAC0_FILES_CONFIG="tracpasswd trac.cgi trac.fcgi trac.wsgi"
TRAC0_FILES_SITES="trac.DOMAIN_NAME.conf-$TRAC0_APACHE2_MODULE" 

# We only care about the first (tld) domain within DOMAIN_RANGES.
DOMAIN_NAME=`echo $DOMAIN_RANGES | cut -d "/" -f 1`
DOMAIN_NAME_PART_A=`echo $DOMAIN_NAME | cut -d "." -f 1`
DOMAIN_NAME_PART_B=`echo $DOMAIN_NAME | cut -d "." -f 2`

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
    mkdir -p $TARGET_ROOT/$TRAC0_CONFIG_DIRECTORY
    mkdir -p $TARGET_ROOT/$TRAC0_DATA_DIRECTORY/repository/apache
    mkdir -p $TARGET_ROOT/$TRAC0_DATA_DIRECTORY/repository/eggs
    for SITE in `echo $TRAC0_FILES_SITES | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" | sed "s|-$TRAC0_APACHE2_MODULE||" | sed "s|.conf||g"`; do
        mkdir -p $TARGET_ROOT/$APACHE2_SITES_DIRECTORY/$SITE
    done;

    # Make sure our service dependencies are built first.
    if [ "$SERVICE_DEPS" ]; then
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

    echo "Creating configuration files in \$TARGET_ROOT$TRAC0_CONFIG_DIRECTORY.."
    for FILE in $TRAC0_FILES_CONFIG; do
        cat $FILE   | sed "s|SERVER_NAME|$SERVER_NAME|g" \
                    | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" \
                    | sed "s|TRAC0_DATA_DIRECTORY|$TRAC0_DATA_DIRECTORY|g" \
                    > $TARGET_ROOT/$TRAC0_DATA_DIRECTORY/$FILE
    done;

    echo "Re-locating cgi, fastcgi and wsgi handler scripts.."
    mv $TARGET_ROOT/$TRAC0_DATA_DIRECTORY/trac.cgi $TARGET_ROOT/$TRAC0_DATA_DIRECTORY/repository/apache
    mv $TARGET_ROOT/$TRAC0_DATA_DIRECTORY/trac.fcgi $TARGET_ROOT/$TRAC0_DATA_DIRECTORY/repository/apache
    mv $TARGET_ROOT/$TRAC0_DATA_DIRECTORY/trac.wsgi $TARGET_ROOT/$TRAC0_DATA_DIRECTORY/repository/apache

    echo "Populating sites subdirectory with generated configuration files.."
    for FILE in $TRAC0_FILES_SITES; do
        NEWFILE="`echo $FILE | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" | sed "s|-$TRAC0_APACHE2_MODULE||g"`"
        cat $FILE   | sed "s|SERVER_NAME|$SERVER_NAME|g" \
                    | sed "s|DOMAIN_NAME_PART_A|$DOMAIN_NAME_PART_A|g" \
                    | sed "s|DOMAIN_NAME_PART_B|$DOMAIN_NAME_PART_B|g" \
                    | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" \
                    | sed "s|TRAC0_DATA_DIRECTORY|$TRAC0_DATA_DIRECTORY|g" \
                    | sed "s|APACHE2_CONFIG_DIRECTORY|$APACHE2_CONFIG_DIRECTORY|g" \
                    | sed "s|APACHE2_DATA_DIRECTORY|$APACHE2_DATA_DIRECTORY|g" \
                    | sed "s|APACHE2_LOGGING_DIRECTORY|$APACHE2_LOGGING_DIRECTORY|g" \
                    | sed "s|APACHE2_MODULES_DIRECTORY|$APACHE2_MODULES_DIRECTORY|g" \
                    | sed "s|APACHE2_SITES_DIRECTORY|$APACHE2_SITES_DIRECTORY|g" \
                    | sed "s|CERTIFICATES_DATA_DIRECTORY|$CERTIFICATES_DATA_DIRECTORY|g" \
                    > $TARGET_ROOT/$APACHE2_CONFIG_DIRECTORY/sites/$NEWFILE
    done;

    # Check to see if centralized auth is wanted, act accordingly.
    if [ "$AUTH" = "basic" ]; then
        echo "Basic authentication requested, leaving basic-auth in place."
    elif [ "$AUTH" = "centralized" ]; then
        echo "Centralized authentication requested, setting up ldap-auth!"
    else
        echo "Eek, selected an authentication option I don't know about!"
        echo "You selected: $AUTH (in $REQUESTED_ROLE)"
        false
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
    
    if [ "$SERVICE_DEPS" ]; then
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

    if [ -e $TARGET_ROOT/$TRAC0_CONFIG_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$TRAC0_CONFIG_DIRECTORY"
        cd $TARGET_ROOT/$TRAC0_CONFIG_DIRECTORY
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$TRAC0_CONFIG_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $TRAC0_CONFIG_DIRECTORY | sed 's|/| |g'`"
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
    
    if [ -e $TARGET_ROOT/$TRAC0_DATA_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$TRAC0_DATA_DIRECTORY"
        cd $TARGET_ROOT/$TRAC0_DATA_DIRECTORY
        rm -vf repository/apache/trac.cgi
        rm -vf repository/apache/trac.fcgi
        rm -vf repository/apache/trac.wsgi
        rmdir repository/apache
        rmdir repository/eggs
        rmdir repository
        rm -vf tracpasswd
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$TRAC0_DATA_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $TRAC0_DATA_DIRECTORY | sed 's|/| |g'`"
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
    
    if [ -e $TARGET_ROOT/$APACHE2_SITES_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$APACHE2_SITES_DIRECTORY"
        cd $TARGET_ROOT/$APACHE2_SITES_DIRECTORY
        rmdir `echo $TRAC0_FILES_SITES | sed "s|DOMAIN_NAME.conf|$DOMAIN_NAME|g" | sed "s|-$TRAC0_APACHE2_MODULE||g"`
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
    
    if [ "$SERVICE_DEPS" ]; then
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

    if [ -e $TARGET_ROOT/$TRAC0_CONFIG_DIRECTORY ]; then
        echo "Creating base config directory, if needed.."
        install -d -m 0755 -o root $TRAC0_CONFIG_DIRECTORY

        echo "Creating base data directory.."
        install -d -m 0755 -o root $TRAC0_DATA_DIRECTORY/repository

        echo "Creating site directories for virtual hosts.."
        for SITE in `echo $TRAC0_FILES_SITES | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" | sed "s|-$TRAC0_APACHE2_MODULE||g" | sed "s|.conf||g"`; do
            echo "* $SITE"
            install -d -m 0755 -o root $APACHE2_SITES_DIRECTORY/$SITE
        done;

        echo "Installing tracpasswd files.."
        install -m 0755 -o root $TARGET_ROOT/$TRAC0_DATA_DIRECTORY/tracpasswd $TRAC0_DATA_DIRECTORY/tracpasswd
        

        echo "Creating the trac repository.."
        trac-admin $TRAC0_DATA_DIRECTORY/repository initenv Trac sqlite:db/trac.db svn $SUBVERSION1_DATA_DIRECTORY/repository

        echo "Installing cgi, fastcgi and wsgi handlers.."
    mkdir -p $TRAC0_DATA_DIRECTORY/repository/apache
    mkdir -p $TRAC0_DATA_DIRECTORY/repository/eggs
        install -m 0755 -o root $TARGET_ROOT/$TRAC0_DATA_DIRECTORY/repository/apache/trac.cgi $TRAC0_DATA_DIRECTORY/repository/apache/trac.cgi
        install -m 0755 -o root $TARGET_ROOT/$TRAC0_DATA_DIRECTORY/repository/apache/trac.fcgi $TRAC0_DATA_DIRECTORY/repository/apache/trac.fcgi
        install -m 0755 -o root $TARGET_ROOT/$TRAC0_DATA_DIRECTORY/repository/apache/trac.wsgi $TRAC0_DATA_DIRECTORY/repository/apache/trac.wsgi

        echo "Installing site configuration files:"
        for CONFIG in `echo $TRAC0_FILES_SITES | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" | sed "s|-$TRAC0_APACHE2_MODULE||g"`; do
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

        
        echo "Setting correct ownership on $TRAC0_DATA_DIRECTORY/repository"
        chown -R $APACHE2_DAEMON_USER:$APACHE2_DAEMON_GROUP $TRAC0_DATA_DIRECTORY/tracpasswd
        chown -R $APACHE2_DAEMON_USER:$APACHE2_DAEMON_GROUP $TRAC0_DATA_DIRECTORY/repository

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
    
    if [ "$SERVICE_DEPS" ]; then
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
    for CONFIG in $TRAC0_FILES_CONFIG; do
        rm -f $TRAC0_CONFIG_DIRECTORY/$CONFIG
    done;
    
    echo "Removing data config.."
    rm $TRAC0_DATA_DIRECTORY/svnacl
    rm $TRAC0_DATA_DIRECTORY/svnpasswd
    rm -rf $TRAC0_DATA_DIRECTORY/repository/

    echo "Removing sites config.."
    for CONFIG in `echo $TRAC0_FILES_SITES | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g"`; do
        rm -f $APACHE2_CONFIG_DIRECTORY/sites/$CONFIG
    done;
     
    echo "Removing directories if empty.."
    rmdir $TRAC0_CONFIG_DIRECTORY > /dev/null 2>&1 < /dev/null
    rmdir $TRAC0_DATA_DIRECTORY > /dev/null 2>&1 < /dev/null

    for SITE in `echo $TRAC0_FILES_SITES | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" | sed "s|.conf||g"`; do
        rmdir $APACHE2_SITES_DIRECTORY/$SITE
    done;
    rmdir $APACHE2_SITES_DIRECTORY

    if [ $? = 0 ]; then
        echo "SERVICE_STATE=uninstalled" > $TARGET_STATE/$SERVICE.state
    else
        echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE.state
    fi;
    echo ""
    ;;

    start)
    if [ $SERVICE_STATE != installed ]; then
        echo "SERVICE_STATE is not installed, but wil try to $1 anyway!"
    fi;
    ;;
    
    stop)
    if [ $SERVICE_STATE != installed ]; then
        echo "SERVICE_STATE is not installed, but wil try to $1 anyway!"
    fi;
    ;;

    state)
    # Simply echo the last written state of this module.
    echo $SERVICE_STATE
    ;;
    
    deps)
    # Make sure our service dependencies are queried also.
    if [ "$SERVICE_DEPS" ]; then
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
    if [ "$TRAC0_PACKAGES_REQUIRED" = 1 ]; then
        echo "$TRAC0_PACKAGES_LISTING"
    fi;
    ;;

    *)
    echo $"Usage: $0 {build|clean|install|uninstall|start|stop|state|deps}"
    exit 1
esac
