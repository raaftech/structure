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
SERVICE_NAME="ldap"
SERVICE_IMPL="openldap2"
SERVICE_DEPS="$OPENLDAP2_SERVICE_DEPS"
SERVICE_PRIO="16"
SERVICE_PORT="389 636"

# Check if globals we care about are set.
NAMESPACE=OPENLDAP2
CHECK="`set | grep ^${NAMESPACE}_`"
if [ -z "$CHECK" ]; then
    mkdir -p $TARGET_STATE
    echo "SERVICE_STATE=failed" > $TARGET_STATE/$SERVICE_IMPL.state
fi;
unset CHECK
unset NAMESPACE

# What are we working with?
OPENLDAP2_FILES_CONFIG="ldap.conf slapd.conf"
OPENLDAP2_FILES_LDIF="root.ldif groups.ldif people.ldif"
OPENLDAP2_FILES_SCHEMA="samba.schema"

# We only care about the first (tld) domain name.
DOMAIN_NAME=`echo $DOMAIN_RANGES | cut -d "/" -f 1`
DOMAIN_NAME_PART_A=`echo $DOMAIN_NAME | cut -d "." -f 1`
DOMAIN_NAME_PART_B=`echo $DOMAIN_NAME | cut -d "." -f 2`

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
                $UNIVERSAL_HOME/Services/$SERVICE_DEP_NAME/$SERVICE_DEP_IMPL/run.sh $1 > /dev/null 2>&1 < /dev/null
                SERVICE_DEP_STATE=`$UNIVERSAL_HOME/Services/$SERVICE_DEP_NAME/$SERVICE_DEP_IMPL/run.sh state`
                echo $SERVICE_DEP_STATE
            fi;
        done;
    fi;

    # Enter this module's home directory before starting real work.
    cd $UNIVERSAL_HOME/Services/$SERVICE_NAME/$SERVICE_IMPL

    # Make sure our target directories exist.
    mkdir -p $TARGET_ROOT/$OPENLDAP2_CONFIG_DIRECTORY/cacerts
    mkdir -p $TARGET_ROOT/$OPENLDAP2_CONFIG_DIRECTORY/import
    mkdir -p $TARGET_ROOT/$OPENLDAP2_CONFIG_DIRECTORY/schema
    mkdir -p $TARGET_ROOT/$OPENLDAP2_DATA_DIRECTORY
    mkdir -p $TARGET_ROOT/$OPENLDAP2_MODULES_DIRECTORY
    mkdir -p $TARGET_ROOT/$OPENLDAP2_STATE_DIRECTORY

    # Creating target files for OPENLDAP2_FILES_CONFIG.
    echo "Creating configuration files.."
    for FILE in $OPENLDAP2_FILES_CONFIG; do
        cat $FILE   | sed "s|OPENLDAP2_CONFIG_DIRECTORY|$OPENLDAP2_CONFIG_DIRECTORY|g" \
                    | sed "s|OPENLDAP2_DATA_DIRECTORY|$OPENLDAP2_DATA_DIRECTORY|g" \
                    | sed "s|OPENLDAP2_MODULES_DIRECTORY|$OPENLDAP2_MODULES_DIRECTORY|g" \
                    | sed "s|OPENLDAP2_STATE_DIRECTORY|$OPENLDAP2_STATE_DIRECTORY|g" \
                    | sed "s|OPENLDAP2_DAEMON_USER|$OPENLDAP2_DAEMON_USER|g" \
                    | sed "s|OPENLDAP2_DAEMON_GROUP|$OPENLDAP2_DAEMON_GROUP|g" \
                    | sed "s|OPENLDAP2_BACKEND_TYPE|$OPENLDAP2_BACKEND_TYPE|g" \
                    | sed "s|CERTIFICATES_CONFIG_DIRECTORY|$CERTIFICATES_CONFIG_DIRECTORY|g" \
                    | sed "s|CERTIFICATES_DATA_DIRECTORY|$CERTIFICATES_DATA_DIRECTORY|g" \
                    | sed "s|DOMAIN_NAME_PART_A|$DOMAIN_NAME_PART_A|g" \
                    | sed "s|DOMAIN_NAME_PART_B|$DOMAIN_NAME_PART_B|g" \
                    | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" \
                    | sed "s|SERVER_NAME|$SERVER_NAME|g" \
                    | sed "s|ORGANIZATION_NAME|$ORGANIZATION_NAME|g" \
                    | sed "s|ORGANIZATION_DEPARTMENT|$ORGANIZATION_DEPARTMENT|g" \
                    | sed "s|ORGANIZATION_CITY|$ORGANIZATION_CITY|g" \
                    | sed "s|ORGANIZATION_STATE|$ORGANIZATION_STATE|g" \
                    | sed "s|ORGANIZATION_COUNTRY|$ORGANIZATION_COUNTRY|g" \
                    > $TARGET_ROOT/$OPENLDAP2_CONFIG_DIRECTORY/$FILE
    done;

    # Creating target files for OPENLDAP2_FILES_LDIF.
    echo "Creating ldif files.."
    for FILE in $OPENLDAP2_FILES_LDIF; do
        cat $FILE   | sed "s|OPENLDAP2_CONFIG_DIRECTORY|$OPENLDAP2_CONFIG_DIRECTORY|g" \
                    | sed "s|OPENLDAP2_DATA_DIRECTORY|$OPENLDAP2_DATA_DIRECTORY|g" \
                    | sed "s|OPENLDAP2_MODULES_DIRECTORY|$OPENLDAP2_MODULES_DIRECTORY|g" \
                    | sed "s|OPENLDAP2_STATE_DIRECTORY|$OPENLDAP2_STATE_DIRECTORY|g" \
                    | sed "s|OPENLDAP2_DAEMON_USER|$OPENLDAP2_DAEMON_USER|g" \
                    | sed "s|OPENLDAP2_DAEMON_GROUP|$OPENLDAP2_DAEMON_GROUP|g" \
                    | sed "s|OPENLDAP2_BACKEND_TYPE|$OPENLDAP2_BACKEND_TYPE|g" \
                    | sed "s|CERTIFICATES_CONFIG_DIRECTORY|$CERTIFICATES_CONFIG_DIRECTORY|g" \
                    | sed "s|CERTIFICATES_DATA_DIRECTORY|$CERTIFICATES_DATA_DIRECTORY|g" \
                    | sed "s|DOMAIN_NAME_PART_A|$DOMAIN_NAME_PART_A|g" \
                    | sed "s|DOMAIN_NAME_PART_B|$DOMAIN_NAME_PART_B|g" \
                    | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" \
                    | sed "s|ORGANIZATION_NAME|$ORGANIZATION_NAME|g" \
                    | sed "s|ORGANIZATION_DEPARTMENT|$ORGANIZATION_DEPARTMENT|g" \
                    | sed "s|ORGANIZATION_CITY|$ORGANIZATION_CITY|g" \
                    | sed "s|ORGANIZATION_STATE|$ORGANIZATION_STATE|g" \
                    | sed "s|ORGANIZATION_COUNTRY|$ORGANIZATION_COUNTRY|g" \
                    > $TARGET_ROOT/$OPENLDAP2_CONFIG_DIRECTORY/import/$FILE
    done;

    # Copying files for OPENLDAP2_FILES_SCHEMA.
    echo "Copying extra schema files.."
    for FILE in $OPENLDAP2_FILES_SCHEMA; do
        cp $FILE $TARGET_ROOT/$OPENLDAP2_CONFIG_DIRECTORY/schema/$FILE
    done;

    # Create a hash named version of the ldap certificate.
    if [ -e $TARGET_ROOT/$CERTIFICATES_DATA_DIRECTORY/ldap.crt ]; then
        echo "Creating hash named certificate file.."
        cp $TARGET_ROOT/$CERTIFICATES_DATA_DIRECTORY/ldap.crt $TARGET_ROOT/$OPENLDAP2_CONFIG_DIRECTORY/cacerts/`openssl x509 -noout -hash < $TARGET_ROOT/$CERTIFICATES_DATA_DIRECTORY/ldap.crt`.0
    else
        echo "No ldap certificate found; This means that the certificate module has not been run yet."
        echo "Please run the certificate module first.. Skipping creation of hash named certificate file in:"
        echo "$TARGET_ROOT/$OPENLDAP2_CONFIG_DIRECTORY/cacerts"
    fi;

    # Check to see if centralized auth is wanted, act accordingly.
    if [ "$AUTH" = "basic" ]; then
        echo "Basic authentication requested, leaving pam and nss alone."
    elif [ "$AUTH" = "centralized" ]; then
        echo "Centralized authentication requested, setting up pam and nss!"
        # Place files matching $REQUESTED_PLATFORM$REQUESTED_MAJOR_[pam/nss]
        # Set up links rhel  : system-auth-uc -> system-auth
        # Set up links debian: libnss-ldap.conf -> ldap.conf
        # Set up links debian: pam_ldap.conf -> ldap.conf
        # Set up links netbsd: usr/pkg/lib/security/pam_ldap.so -> usr/lib/security/pam_ldap.so.0
        # Read in the domain SID and insert in the groups.ldif!
    else
        echo "Eek, selected an authentication option I don't know about!"
        echo "You selected: $AUTH (in $REQUESTED_ROLE)"
        false
    fi;

    # Check if the last action exited correctly. Needs to be fixed.
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
    
    # Make sure our service dependencies are cleaned first.
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

    # Cleaning for OPENLDAP2_CONFIG_DIRECTORY.
    if [ -e $TARGET_ROOT/$OPENLDAP2_CONFIG_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$OPENLDAP2_CONFIG_DIRECTORY"
        cd $TARGET_ROOT/$OPENLDAP2_CONFIG_DIRECTORY
        rm -vf $OPENLDAP2_FILES_CONFIG
        rm -vf cacerts/*
        rmdir cacerts
        rm -vf import/*
        rmdir import
        rm -vf schema/*
        rmdir schema
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$OPENLDAP2_CONFIG_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $OPENLDAP2_CONFIG_DIRECTORY | sed 's|/| |g'`"
        while [ "$CLEAN_DIRS_EMPTY" != "$TARGET_ROOT" ]; do
            cd $CLEAN_DIRS_EMPTY/..
            rmdir $CLEAN_DIRS_NAMES > /dev/null 2>&1 < /dev/null
            CLEAN_DIRS_EMPTY=`pwd`
        done;
    else
        echo "No created config directory found. use \"build\" first. "
    fi;

    # Cleaning for OPENLDAP2_DATA_DIRECTORY.
    if [ -e $TARGET_ROOT/$OPENLDAP2_DATA_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$OPENLDAP2_DATA_DIRECTORY"
        cd $TARGET_ROOT/$OPENLDAP2_DATA_DIRECTORY
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$OPENLDAP2_DATA_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $OPENLDAP2_DATA_DIRECTORY | sed 's|/| |g'`"
        while [ "$CLEAN_DIRS_EMPTY" != "$TARGET_ROOT" ]; do
            cd $CLEAN_DIRS_EMPTY/..
            rmdir $CLEAN_DIRS_NAMES > /dev/null 2>&1 < /dev/null
            CLEAN_DIRS_EMPTY=`pwd`
        done;
    else
        echo "No created data directory found. use \"build\" first. "
    fi;

    # Cleaning for OPENLDAP2_MODULES_DIRECTORY.
    if [ -e $TARGET_ROOT/$OPENLDAP2_MODULES_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$OPENLDAP2_MODULES_DIRECTORY"
        cd $TARGET_ROOT/$OPENLDAP2_MODULES_DIRECTORY
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$OPENLDAP2_MODULES_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $OPENLDAP2_MODULES_DIRECTORY | sed 's|/| |g'`"
        while [ "$CLEAN_DIRS_EMPTY" != "$TARGET_ROOT" ]; do
            cd $CLEAN_DIRS_EMPTY/..
            rmdir $CLEAN_DIRS_NAMES > /dev/null 2>&1 < /dev/null
            CLEAN_DIRS_EMPTY=`pwd`
        done;
    else
        echo "No created modules directory found. use \"build\" first. "
    fi;

    # Cleaning for OPENLDAP2_STATE_DIRECTORY.
    if [ -e $TARGET_ROOT/$OPENLDAP2_STATE_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$OPENLDAP2_STATE_DIRECTORY"
        cd $TARGET_ROOT/$OPENLDAP2_STATE_DIRECTORY
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$OPENLDAP2_STATE_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $OPENLDAP2_STATE_DIRECTORY | sed 's|/| |g'`"
        while [ "$CLEAN_DIRS_EMPTY" != "$TARGET_ROOT" ]; do
            cd $CLEAN_DIRS_EMPTY/..
            rmdir $CLEAN_DIRS_NAMES > /dev/null 2>&1 < /dev/null
            CLEAN_DIRS_EMPTY=`pwd`
        done;
    else
        echo "No created state directory found. use \"build\" first. "
    fi;

    # Check if the last action exited correctly. Needs to be fixed.
    if [ $? = 0 ]; then
        echo "SERVICE_STATE=cleaned" > $TARGET_STATE/$SERVICE_IMPL.state
    else
        echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE_IMPL.state
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

    # Enter this module's home directory before starting real work.
    cd $UNIVERSAL_HOME/Services/$SERVICE_NAME/$SERVICE_IMPL

    # Make sure our target directories exist.
    mkdir -p $OPENLDAP2_CONFIG_DIRECTORY/cacerts
    mkdir -p $OPENLDAP2_CONFIG_DIRECTORY/import
    mkdir -p $OPENLDAP2_CONFIG_DIRECTORY/schema
    mkdir -p $OPENLDAP2_DATA_DIRECTORY
    mkdir -p $OPENLDAP2_MODULES_DIRECTORY
    mkdir -p $OPENLDAP2_STATE_DIRECTORY

    # Installing target files for OPENLDAP2_FILES_CONFIG.
    if [ -e $TARGET_ROOT/$OPENLDAP2_CONFIG_DIRECTORY ]; then
        echo "Installing generated configuration files:"
        for FILE in $OPENLDAP2_FILES_CONFIG; do
            if [ -e $OPENLDAP2_CONFIG_DIRECTORY/$FILE ]; then
                if [ ! -e $OPENLDAP2_CONFIG_DIRECTORY/$FILE.orig ]; then
                    cp $OPENLDAP2_CONFIG_DIRECTORY/$FILE $OPENLDAP2_CONFIG_DIRECTORY/$FILE.orig
                fi;
            fi;
            echo -n " * $FILE: ";
            install -m 0755 -o root $TARGET_ROOT/$OPENLDAP2_CONFIG_DIRECTORY/$FILE $OPENLDAP2_CONFIG_DIRECTORY/$FILE

            # Check if the last loop iteration exited correctly.
            if [ $? = 0 ]; then
                echo "success"
                echo "SERVICE_STATE=installed" > $TARGET_STATE/$SERVICE_IMPL.state
            else
                STATE="fail"
                echo "$STATE"
            fi;
        done;
    else
         echo "No generated configuration found. use \"build\" first. "
    fi;

    # Installing generated hash named certificate file for OPENLDAP2_FILES_CONFIG/cacerts.
    echo "Copying generated hash named certificate file.."
    cp $TARGET_ROOT/$OPENLDAP2_CONFIG_DIRECTORY/cacerts/* $OPENLDAP2_CONFIG_DIRECTORY/cacerts/

    # Installing target files for OPENLDAP2_FILES_CONFIG/import.
    if [ -e $TARGET_ROOT/$OPENLDAP2_CONFIG_DIRECTORY/import ]; then
        echo "Installing generated ldif import files:"
        for FILE in $OPENLDAP2_FILES_LDIF; do
            if [ -e $OPENLDAP2_CONFIG_DIRECTORY/import/$FILE ]; then
                if [ ! -e $OPENLDAP2_CONFIG_DIRECTORY/import/$FILE.orig ]; then
                    cp $OPENLDAP2_CONFIG_DIRECTORY/import/$FILE $OPENLDAP2_CONFIG_DIRECTORY/import/$FILE.orig
                fi;
            fi;
            echo -n " * $FILE: ";
            install -m 0755 -o root $TARGET_ROOT/$OPENLDAP2_CONFIG_DIRECTORY/import/$FILE \
                                    $OPENLDAP2_CONFIG_DIRECTORY/import/$FILE

            # Check if the last loop iteration exited correctly.
            if [ $? = 0 ]; then
                echo "success"
                echo "SERVICE_STATE=installed" > $TARGET_STATE/$SERVICE_IMPL.state
            else
                STATE="fail"
                echo "$STATE"
            fi;
        done;
    else
         echo "No generated ldif import files found. use \"build\" first. "
    fi;

    # Installing target files for OPENLDAP2_FILES_CONFIG/schema.
    if [ -e $TARGET_ROOT/$OPENLDAP2_CONFIG_DIRECTORY/schema ]; then
        echo "Installing schema files:"
        for FILE in $OPENLDAP2_FILES_SCHEMA; do
            if [ -e $OPENLDAP2_CONFIG_DIRECTORY/schema/$FILE ]; then
                if [ ! -e $OPENLDAP2_CONFIG_DIRECTORY/schema/$FILE.orig ]; then
                    cp $OPENLDAP2_CONFIG_DIRECTORY/schema/$FILE $OPENLDAP2_CONFIG_DIRECTORY/schema/$FILE.orig
                fi;
            fi;
            echo -n " * $FILE: ";
            install -m 0755 -o root $TARGET_ROOT/$OPENLDAP2_CONFIG_DIRECTORY/schema/$FILE $OPENLDAP2_CONFIG_DIRECTORY/schema/$FILE

            # Check if the last loop iteration exited correctly.
            if [ $? = 0 ]; then
                echo "success"
                echo "SERVICE_STATE=installed" > $TARGET_STATE/$SERVICE_IMPL.state
            else
                STATE="fail"
                echo "$STATE"
            fi;
        done;
    else
         echo "No generated ldif import files found. use \"build\" first. "
    fi;

    # Disable modulepath and moduleload settings if _MODULAR_BACKEND=0.
    if [ $OPENLDAP2_MODULAR_BACKEND = "0" ]; then
        echo "Disabling modulepath and moduleload in slapd.conf.."
	    cp $OPENLDAP2_CONFIG_DIRECTORY/slapd.conf /tmp/slapd.conf.tmp
	    cat /tmp/slapd.conf.tmp | sed "s|^module|#module|g" > $OPENLDAP2_CONFIG_DIRECTORY/slapd.conf
	    rm /tmp/slapd.conf.tmp
    else
	    echo "OPENLDAP2_MODULAR_BACKEND=$OPENLDAP2_MODULAR_BACKEND, nice.."
    fi;
    # Creating slapd databases using installed configuration.
    echo "Cleaning up default database if any.."
    rm -f $OPENLDAP2_DATA_DIRECTORY/alock
    rm -f $OPENLDAP2_DATA_DIRECTORY/*.bdb
    rm -f $OPENLDAP2_DATA_DIRECTORY/__db*.*
    rm -f $OPENLDAP2_DATA_DIRECTORY/log.00000000*
    echo "Creating new database using ldif files from import.."
    for FILE in $OPENLDAP2_FILES_LDIF; do
       slapadd -v -l $OPENLDAP2_CONFIG_DIRECTORY/import/$FILE
    done;
    echo "Setting correct permissions.."
    chown -R $OPENLDAP2_DAEMON_USER:$OPENLDAP2_DAEMON_GROUP $OPENLDAP2_DATA_DIRECTORY
    chown -R $OPENLDAP2_DAEMON_USER:$OPENLDAP2_DAEMON_GROUP $OPENLDAP2_STATE_DIRECTORY
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
    
    # Make sure our service dependencies are uninstalled first.
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

    # Enter this module's home directory before starting real work.
    cd $UNIVERSAL_HOME/Services/$SERVICE_NAME/$SERVICE_IMPL

    # Attempt to remove directories which might be empty.
    rmdir $OPENLDAP2_DATA_DIRECTORY
    rmdir $OPENLDAP2_MODULES_DIRECTORY
    rmdir $OPENLDAP2_STATE_DIRECTORY

    # Uninstalling target files for OPENLDAP2_FILES_CONFIG.
    if [ -e $OPENLDAP2_CONFIG_DIRECTORY ]; then
        echo "Uninstalling generated configuration.."
        for FILE in $OPENLDAP2_FILES_CONFIG; do
            if [ -e $OPENLDAP2_CONFIG_DIRECTORY/$FILE.orig ]; then
                cp $OPENLDAP2_CONFIG_DIRECTORY/$FILE.orig $OPENLDAP2_CONFIG_DIRECTORY/$FILE
	        else
                rm -f $OPENLDAP2_CONFIG_DIRECTORY/$FILE
            fi;
        done;

        # Check if the last loop iteration exited correctly.
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

    # Uninstalling generated hash named certificate file for OPENLDAP2_FILES_CONFIG/cacerts.
    echo "Removing generated hash named certificate file.."
    rm $OPENLDAP2_CONFIG_DIRECTORY/cacerts/*
    rmdir $OPENLDAP2_CONFIG_DIRECTORY/cacerts

    # Uninstalling target files for OPENLDAP2_FILES_CONFIG/import.
    if [ -e $OPENLDAP2_CONFIG_DIRECTORY/import ]; then
        echo "Uninstalling generated ldif import files.."
        for FILE in $OPENLDAP2_FILES_LDIF; do
            if [ -e $OPENLDAP2_CONFIG_DIRECTORY/import/$FILE.orig ]; then
                cp $OPENLDAP2_CONFIG_DIRECTORY/import/$FILE.orig $OPENLDAP2_CONFIG_DIRECTORY/import/$FILE
            else
                rm -f $OPENLDAP2_CONFIG_DIRECTORY/import/$FILE
            fi;
        done;

        # Check if the last loop iteration exited correctly.
        if [ $? = 0 ]; then
            echo "SERVICE_STATE=uninstalled" > $TARGET_STATE/$SERVICE_IMPL.state
        else
            echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE_IMPL.state
        fi;
    else
        echo "No installed import directory found; Are you sure you've"
        echo "specified the right platform for this machine? Or did you"
        echo "forget to use \"install\" in the first place?"
    fi;

    # Uninstalling target files for OPENLDAP2_FILES_CONFIG/schema.
    if [ -e $OPENLDAP2_CONFIG_DIRECTORY/schema ]; then
        echo "Uninstalling schema files.."
        for FILE in $OPENLDAP2_FILES_SCHEMA; do
            if [ -e $OPENLDAP2_CONFIG_DIRECTORY/schema/$FILE.orig ]; then
                cp $OPENLDAP2_CONFIG_DIRECTORY/schema/$FILE.orig $OPENLDAP2_CONFIG_DIRECTORY/schema/$FILE
            else
                rm -f $OPENLDAP2_CONFIG_DIRECTORY/schema/$FILE
            fi;
        done;

        # Check if the last loop iteration exited correctly.
        if [ $? = 0 ]; then
            echo "SERVICE_STATE=uninstalled" > $TARGET_STATE/$SERVICE_IMPL.state
        else
            echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE_IMPL.state
        fi;    
    else
        echo "No installed schema directory found; Are you sure you've"
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
        if [ "$OPENLDAP2_INIT_STRING" ]; then
            if [ -e $OPENLDAP2_INIT_CONFIG ]; then
                if [ ! -e $OPENLDAP2_INIT_CONFIG.orig ]; then
                    cp $OPENLDAP2_INIT_CONFIG $OPENLDAP2_INIT_CONFIG.orig
                elif [ -e $OPENLDAP2_INIT_CONFIG.orig ]; then  
                    cp $OPENLDAP2_INIT_CONFIG.orig $OPENLDAP2_INIT_CONFIG
                fi;
            else
                echo "Mmm, your specified config file does not seem to exist:"
                echo "OPENLDAP2_INIT_CONFIG=$OPENLDAP2_INIT_CONFIG"
                echo ""
                echo "I will create it for you using OPENLDAP2_INIT_STRING as its content"
                echo "But I can't tell if this will be sufficient. Just so you know.."
                echo ""
            fi;
            echo "Appending OPENLDAP2_INIT_STRING to $OPENLDAP2_INIT_CONFIG.."
            echo "$OPENLDAP2_INIT_STRING" >> $OPENLDAP2_INIT_CONFIG
        fi;

        COUNT=$SERVICE_PRIO
        for OPENLDAP2_INIT_NAME in $OPENLDAP2_INIT_SCRIPTS; do
            if [ $PLATFORM_INIT_TYPE = "bsd" ]; then
                echo "Specified init type is $PLATFORM_INIT_TYPE. Acting accordingly.."
                if [ -e $PLATFORM_INIT_DIRECTORY/$OPENLDAP2_INIT_NAME ]; then
                    echo "Enabling boot-time startup for $OPENLDAP2_INIT_NAME in /etc/rc.conf."
                    echo "$OPENLDAP2_INIT_NAME=YES" >> /etc/rc.conf
                else
                    echo "No $OPENLDAP2_INIT_NAME init script(s) found!"
                    echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                    echo "init script(s): $OPENLDAP2_INIT_SCRIPTS"
                fi;
            elif [ $PLATFORM_INIT_TYPE = "sysv" ]; then
                echo "Specified init type is $PLATFORM_INIT_TYPE. Acting accordingly.."
                if [ -e $PLATFORM_INIT_DIRECTORY/$OPENLDAP2_INIT_NAME ]; then
                    echo "Enabling boot-time startup for $OPENLDAP2_INIT_NAME in runlevels 2,3,4,5."
                    for RUNLEVEL in 2 3 4 5; do
                        rm -f $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/*$OPENLDAP2_INIT_NAME
                        ln -s $PLATFORM_INIT_DIRECTORY/$OPENLDAP2_INIT_NAME $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/S$COUNT$OPENLDAP2_INIT_NAME
                    done;
                    for RUNLEVEL in 0 1 6; do
                        rm -f $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/*$OPENLDAP2_INIT_NAME
                        ln -s $PLATFORM_INIT_DIRECTORY/$OPENLDAP2_INIT_NAME $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/K$COUNT$OPENLDAP2_INIT_NAME
                    done;
                else
                    echo "No $OPENLDAP2_INIT_NAME init script(s) found!"
                    echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                    echo "init script(s): $OPENLDAP2_INIT_SCRIPTS"
                fi;
            else
                echo "The variable PLATFORM_INIT_TYPE was set to \"$PLATFORM_INIT_TYPE\";"
                echo "This is currently not supported. It has to be either bsd or sysv!"
            fi;
        
            # And start it up.
            echo "Starting up $OPENLDAP2_INIT_NAME.."
            if [ -e $PLATFORM_INIT_DIRECTORY/$OPENLDAP2_INIT_NAME ]; then
                $PLATFORM_INIT_DIRECTORY/$OPENLDAP2_INIT_NAME start
            else
                echo "No $OPENLDAP2_INIT_NAME init script(s) found!"
                echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                echo "init script(s): $OPENLDAP2_INIT_SCRIPTS"
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
    if [ "$OPENLDAP2_INIT_STRING" ]; then
        if [ -e $OPENLDAP2_INIT_CONFIG.orig ]; then
            cp $OPENLDAP2_INIT_CONFIG.orig $OPENLDAP2_INIT_CONFIG
        fi;
    fi;

    for OPENLDAP2_INIT_NAME in $OPENLDAP2_INIT_SCRIPTS; do
        if [ $PLATFORM_INIT_TYPE = "bsd" ]; then
            if [ -e $PLATFORM_INIT_DIRECTORY/$OPENLDAP2_INIT_NAME ]; then
                echo "$OPENLDAP2_INIT_NAME=NO" >> /etc/rc.conf
            else
                echo "No $OPENLDAP2_INIT_NAME init script(s) found!"
                echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                echo "init script(s): $OPENLDAP2_INIT_SCRIPTS"
            fi;
        elif [ $PLATFORM_INIT_TYPE = "sysv" ]; then
            if [ -e $PLATFORM_INIT_DIRECTORY/$OPENLDAP2_INIT_NAME ]; then
                for RUNLEVEL in 0 1 2 3 4 5 6; do
                    rm -f $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/*$OPENLDAP2_INIT_NAME
                done;
            else
                echo "No $OPENLDAP2_INIT_NAME init script(s) found!"
                echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                echo "init script(s): $OPENLDAP2_INIT_SCRIPTS"
            fi;
        else
            echo "The variable PLATFORM_INIT_TYPE was set to \"$PLATFORM_INIT_TYPE\";"
            echo "This is currently not supported. It has to be either bsd or sysv!"
        fi;

        # And stop the service.
        echo "Stopping $OPENLDAP2_INIT_NAME.."
        if [ -e $PLATFORM_INIT_DIRECTORY/$OPENLDAP2_INIT_NAME ]; then
            $PLATFORM_INIT_DIRECTORY/$OPENLDAP2_INIT_NAME stop
        else
            echo "No $OPENLDAP2_INIT_NAME init script(s) found!"
            echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
            echo "init script(s): $OPENLDAP2_INIT_SCRIPTS"
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
    if [ "$OPENLDAP2_PACKAGES_REQUIRED" = 1 ]; then
        echo "$OPENLDAP2_PACKAGES_LISTING"
    fi;
    ;;

    *)
    echo $"Usage: $0 {build|clean|install|uninstall|start|stop|state|deps}"
    exit 1
esac
