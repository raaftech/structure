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
SERVICE_NAME="vcs"
SERVICE_IMPL="subversion1"
SERVICE_DEPS="$SUBVERSION1_SERVICE_DEPS"
SERVICE_PRIO="25"
SERVICE_PORT=""

# Check if globals we care about are set.
NAMESPACE=SUBVERSION1
CHECK="`set | grep ^${NAMESPACE}_`"
if [ -z "$CHECK" ]; then
    mkdir -p $TARGET_STATE
    echo "SERVICE_STATE=failed" > $TARGET_STATE/$SERVICE_IMPL.state
fi;
unset CHECK
unset NAMESPACE

# What are we working with?
SUBVERSION1_FILES_CONFIG=""
SUBVERSION1_FILES_SITES="svn.DOMAIN_NAME.conf" 

# We only care about the first (tld) domain within DOMAIN_RANGES.
DOMAIN_NAME=`echo $DOMAIN_RANGES | cut -d "/" -f 1`

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
    
    # Make sure our target directories exist.
    mkdir -p $TARGET_ROOT/$SUBVERSION1_CONFIG_DIRECTORY
    mkdir -p $TARGET_ROOT/$SUBVERSION1_DATA_DIRECTORY/repository
    for SITE in `echo $SUBVERSION1_FILES_SITES | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" | sed "s|.conf||g"`; do
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

    cd $UNIVERSAL_HOME/Services/$SERVICE_NAME/$SERVICE_IMPL

    echo "Creating configuration files in \$TARGET_ROOT$SUBVERSION1_CONFIG_DIRECTORY.."
    for FILE in $SUBVERSION1_FILES_CONFIG; do
        cat $FILE   | sed "s|SERVER_NAME|$SERVER_NAME|g" \
                    | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" \
                    | sed "s|SUBVERSION1_CONFIG_DIRECTORY|$SUBVERSION1_CONFIG_DIRECTORY|g" \
                    | sed "s|SUBVERSION1_DATA_DIRECTORY|$SUBVERSION1_DATA_DIRECTORY|g" \
                    | sed "s|APACHE2_CONFIG_DIRECTORY|$APACHE2_CONFIG_DIRECTORY|g" \
                    | sed "s|APACHE2_DATA_DIRECTORY|$APACHE2_DATA_DIRECTORY|g" \
                    | sed "s|APACHE2_LOGGING_DIRECTORY|$APACHE2_LOGGING_DIRECTORY|g" \
                    | sed "s|APACHE2_MODULES_DIRECTORY|$APACHE2_MODULES_DIRECTORY|g" \
                    | sed "s|APACHE2_SITES_DIRECTORY|$APACHE2_SITES_DIRECTORY|g" \
                    | sed "s|CERTIFICATES_DATA_DIRECTORY|$CERTIFICATES_DATA_DIRECTORY|g" \
                    > $TARGET_ROOT/$SUBVERSION1_CONFIG_DIRECTORY/$FILE
    done;
    
    echo "Populating sites subdirectory with generated coniguration files.."
    for FILE in $SUBVERSION1_FILES_SITES; do
        NEWFILE="`echo $FILE | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g"`"
        cat $FILE   | sed "s|SERVER_NAME|$SERVER_NAME|g" \
                    | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" \
                    | sed "s|SUBVERSION1_CONFIG_DIRECTORY|$SUBVERSION1_CONFIG_DIRECTORY|g" \
                    | sed "s|SUBVERSION1_DATA_DIRECTORY|$SUBVERSION1_DATA_DIRECTORY|g" \
                    | sed "s|APACHE2_CONFIG_DIRECTORY|$APACHE2_CONFIG_DIRECTORY|g" \
                    | sed "s|APACHE2_DATA_DIRECTORY|$APACHE2_DATA_DIRECTORY|g" \
                    | sed "s|APACHE2_LOGGING_DIRECTORY|$APACHE2_LOGGING_DIRECTORY|g" \
                    | sed "s|APACHE2_MODULES_DIRECTORY|$APACHE2_MODULES_DIRECTORY|g" \
                    | sed "s|APACHE2_SITES_DIRECTORY|$APACHE2_SITES_DIRECTORY|g" \
                    | sed "s|CERTIFICATES_DATA_DIRECTORY|$CERTIFICATES_DATA_DIRECTORY|g" \
                    > $TARGET_ROOT/$APACHE2_CONFIG_DIRECTORY/sites/$NEWFILE
    done;

    echo "Placing a default svnpasswd and svnacl file.."
    cp svnacl $TARGET_ROOT/$SUBVERSION1_DATA_DIRECTORY/svnacl
    cp svnpasswd $TARGET_ROOT/$SUBVERSION1_DATA_DIRECTORY/svnpasswd

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

    if [ -e $TARGET_ROOT/$SUBVERSION1_CONFIG_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$SUBVERSION1_CONFIG_DIRECTORY"
        cd $TARGET_ROOT/$SUBVERSION1_CONFIG_DIRECTORY
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$SUBVERSION1_CONFIG_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $SUBVERSION1_CONFIG_DIRECTORY | sed 's|/| |g'`"
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
    
    if [ -e $TARGET_ROOT/$SUBVERSION1_DATA_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$SUBVERSION1_DATA_DIRECTORY"
        cd $TARGET_ROOT/$SUBVERSION1_DATA_DIRECTORY
        rmdir repository
        rm -f svnacl
        rm -f svnpasswd
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$SUBVERSION1_DATA_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $SUBVERSION1_DATA_DIRECTORY | sed 's|/| |g'`"
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
    

    if [ -e $TARGET_ROOT/$APACHE2_SITES_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$APACHE2_SITES_DIRECTORY"
        cd $TARGET_ROOT/$APACHE2_SITES_DIRECTORY
        rmdir `echo $SUBVERSION1_FILES_SITES | sed "s|DOMAIN_NAME.conf|$DOMAIN_NAME|g"`
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$APACHE2_SITES_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $APACHE2_SITES_DIRECTORY | sed 's|/| |g'`"
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

    if [ -e $TARGET_ROOT/$SUBVERSION1_CONFIG_DIRECTORY ]; then
        echo "Creating base config directory, if needed.."
        install -d -m 0755 -o root $SUBVERSION1_CONFIG_DIRECTORY

        echo "Creating base data directory.."
        install -d -m 0755 -o root $SUBVERSION1_DATA_DIRECTORY/repository

        echo "Creating site directories for virtual hosts.."
        for SITE in `echo $SUBVERSION1_FILES_SITES | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" | sed "s|.conf||g"`; do
            install -d -m 0755 -o root $APACHE2_SITES_DIRECTORY/$SITE
        done;

        echo "Installing main configuration files:"
        for CONFIG in $SUBVERSION1_FILES_CONFIG; do
            echo -n " * $CONFIG: ";
            install -m 0755 -o root $TARGET_ROOT/$SUBVERSION1_CONFIG_DIRECTORY/$CONFIG $SUBVERSION1_CONFIG_DIRECTORY/$CONFIG

            if [ $? = 0 ]; then
                echo "success"
                echo "SERVICE_STATE=installed" > $TARGET_STATE/$SERVICE_IMPL.state
            else
                STATE="fail"
                echo "$STATE"
                echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE_IMPL.state
            fi;
        done;

        echo "Installing site configuration files:"
        for CONFIG in `echo $SUBVERSION1_FILES_SITES | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g"`; do
            echo -n " * $CONFIG: ";
            install -m 0755 -o root $TARGET_ROOT/$APACHE2_CONFIG_DIRECTORY/sites/$CONFIG $APACHE2_CONFIG_DIRECTORY/sites/$CONFIG

            if [ $? = 0 ]; then
                echo "success"
                echo "SERVICE_STATE=installed" > $TARGET_STATE/$SERVICE_IMPL.state
            else
                STATE="fail"
                echo "$STATE"
                echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE_IMPL.state
            fi;
        done;

        echo "Placing a default svnpasswd and svnacl file.."
        cp $TARGET_ROOT/$SUBVERSION1_DATA_DIRECTORY/svnacl $SUBVERSION1_DATA_DIRECTORY/svnacl
        cp $TARGET_ROOT/$SUBVERSION1_DATA_DIRECTORY/svnpasswd $SUBVERSION1_DATA_DIRECTORY/svnpasswd

        echo "Creating the subversion repository.."
        svnadmin create $SUBVERSION1_DATA_DIRECTORY/repository --fs-type fsfs
        
        echo "Setting correct ownership on $SUBVERSION1_DATA_DIRECTORY/repository"
        chown -R $APACHE2_DAEMON_USER:$APACHE2_DAEMON_GROUP $SUBVERSION1_DATA_DIRECTORY/svnacl
        chown -R $APACHE2_DAEMON_USER:$APACHE2_DAEMON_GROUP $SUBVERSION1_DATA_DIRECTORY/svnpasswd
        chown -R $APACHE2_DAEMON_USER:$APACHE2_DAEMON_GROUP $SUBVERSION1_DATA_DIRECTORY/repository

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
    for CONFIG in $SUBVERSION1_FILES_CONFIG; do
        rm -f $SUBVERSION1_CONFIG_DIRECTORY/$CONFIG
    done;
    
    echo "Removing data config.."
    rm $SUBVERSION1_DATA_DIRECTORY/svnacl
    rm $SUBVERSION1_DATA_DIRECTORY/svnpasswd
    rm -rf $SUBVERSION1_DATA_DIRECTORY/repository/

    echo "Removing sites config.."
    for CONFIG in `echo $SUBVERSION1_FILES_SITES | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g"`; do
        rm -f $APACHE2_CONFIG_DIRECTORY/sites/$CONFIG
    done;
     
    echo "Removing directories if empty.."
    rmdir $SUBVERSION1_CONFIG_DIRECTORY > /dev/null 2>&1 < /dev/null
    rmdir $SUBVERSION1_DATA_DIRECTORY > /dev/null 2>&1 < /dev/null

    for SITE in `echo $SUBVERSION1_FILES_SITES | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" | sed "s|.conf||g"`; do
        rmdir $APACHE2_SITES_DIRECTORY/$SITE
    done;
    rmdir $APACHE2_SITES_DIRECTORY

    if [ $? = 0 ]; then
        echo "SERVICE_STATE=uninstalled" > $TARGET_STATE/$SERVICE_IMPL.state
    else
        echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE_IMPL.state
    fi;
    echo ""
    ;;

    start)
    if [ $SERVICE_STATE != installed ]; then
        echo "SERVICE_STATE is not installed, aborting $1."
        exit 1
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
    if [ "$SUBVERSION1_PACKAGES_REQUIRED" = 1 ]; then
        echo "$SUBVERSION1_PACKAGES_LISTING"
    fi;
    ;;

    *)
    echo $"Usage: $0 {build|clean|install|uninstall|start|stop|state|deps}"
    exit 1
esac
