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
CERTTEMP="$UNIVERSAL_HOME/Tools/certificates/configuration"
CERTLIST="$SERVER_NAME $ALIASES"
CERTLOG="$TARGET_LOGS/certificates.log"

DOMAIN_NAME=`echo $DOMAIN_RANGES | cut -d "/" -f 1`

# Include state for this specific module.
if [ -e $TARGET_STATE/certificates.state ]; then
    . $TARGET_STATE/certificates.state
else
    mkdir -p $TARGET_STATE
    echo "SERVICE_STATE=uninitialized" > $TARGET_STATE/certificates.state
    . $TARGET_STATE/certificates.state
fi;


# Main case statement.
case "$1" in
    build)
    # Make sure our target directory exists.
    mkdir -p $TARGET_ROOT/$CERTIFICATES_DATA_DIRECTORY
    rm -f $TARGET_ROOT/$CERTIFICATES_DATA_DIRECTORY/*
    
    # Make sure our logging directory exists.
    mkdir -p $TARGET_LOGS

    rm -f $CERTLOG
    echo "$0 invoked at `date`. Will output below." > $CERTLOG
    echo "" >> $CERTLOG
    echo "Generating service certificate/key pairs:"
    for CERTNAME in $CERTLIST; do

        # Count number of DOMAIN_RANGES.
        COUNT=0
        for RANGE in $DOMAIN_RANGES; do
            COUNT=`expr $COUNT "+" 1`
        done;

        if [ "$COUNT" -eq 1 ]; then
            # There's only 1 network defined, only adding IP to subjectAltName.
            for RANGE in $DOMAIN_RANGES; do
                SERVER_ADDR=`echo $RANGE | cut -d "/" -f 2 | sed "s|.0$|.$SERVER_SUFFIX|g"`
                SUBJECTALTNAME_LIST="IP:$SERVER_ADDR"
            done;
        else
            # There are multiple networks. adding IP and DNS to subjectAltName.
            COUNT=0
            for RANGE in $DOMAIN_RANGES; do
                SERVER_ADDR=`echo $RANGE | cut -d "/" -f 2 | sed "s|.0$|.$SERVER_SUFFIX|g"`
                DOMAIN_ALIAS=`echo $RANGE | cut -d "/" -f 1`.`echo $DOMAIN_RANGES | cut -d "/" -f 1`

                if [ "$COUNT" -ne 0 ]; then
                    ADDITION="IP:$SERVER_ADDR,DNS:$CERTNAME.$DOMAIN_ALIAS"
                    SUBJECTALTNAME_LIST="`echo $SUBJECTALTNAME_LIST,$ADDITION | sed "s|^,||g"`" 
                fi;

                COUNT=`expr $COUNT "+" 1`
            done;
        fi;

        echo -n " * $CERTNAME: ";
        echo "$CERTNAME certificate creation invoked at `date`, output follows:" >> $CERTLOG
        echo "------------------------------------------------------------------------" >> $CERTLOG
        
            cat $CERTTEMP               | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" \
                                        | sed "s|CERTNAME|$CERTNAME|g" \
                                        | sed "s|ORGANIZATION_COUNTRY|$ORGANIZATION_COUNTRY|g" \
                                        | sed "s|ORGANIZATION_STATE|$ORGANIZATION_STATE|g" \
                                        | sed "s|ORGANIZATION_CITY|$ORGANIZATION_CITY|g" \
                                        | sed "s|ORGANIZATION_DEPARTMENT|$ORGANIZATION_DEPARTMENT|g" \
                                        | sed "s|ORGANIZATION_NAME|$ORGANIZATION_NAME|g" \
                                        | sed "s|SUBJECTALTNAME_LIST|$SUBJECTALTNAME_LIST|g" \
                                        > $TARGET_ROOT/$CERTIFICATES_DATA_DIRECTORY/$CERTNAME.cfg; 

            openssl req -days 3653 -config $TARGET_ROOT/$CERTIFICATES_DATA_DIRECTORY/$CERTNAME.cfg \
                        -new -text -out    $TARGET_ROOT/$CERTIFICATES_DATA_DIRECTORY/$CERTNAME.req \
                        >> $CERTLOG 2>&1 < /dev/null

            mv $CERTNAME.pem $TARGET_ROOT/$CERTIFICATES_DATA_DIRECTORY

            openssl rsa -passin pass:system -in $TARGET_ROOT/$CERTIFICATES_DATA_DIRECTORY/$CERTNAME.pem \
                                           -out $TARGET_ROOT/$CERTIFICATES_DATA_DIRECTORY/$CERTNAME.key \
                        >> $CERTLOG 2>&1 < /dev/null

            openssl req -days 3653 -x509    -in $TARGET_ROOT/$CERTIFICATES_DATA_DIRECTORY/$CERTNAME.req \
                                     -text -key $TARGET_ROOT/$CERTIFICATES_DATA_DIRECTORY/$CERTNAME.key \
                                           -out $TARGET_ROOT/$CERTIFICATES_DATA_DIRECTORY/$CERTNAME.crt \
                        >> $CERTLOG 2>&1 < /dev/null
            unset SUBJECTALTNAME_LIST

            if [ $? = 0 ]; then
                echo "success"
                echo "SERVICE_STATE=built" > $TARGET_STATE/certificates.state
            else
                STATE="fail"
                echo "$STATE"
                echo "SERVICE_STATE=unknown" > $TARGET_STATE/certificates.state
            fi;
    done;
    echo ""
    ;;

    clean)
    if [ -e $TARGET_ROOT/$CERTIFICATES_DATA_DIRECTORY ]; then
        echo "Cleaning up generated certificates:"
        for CERTNAME in $CERTLIST; do
            echo -n " * $CERTNAME: ";
            rm $TARGET_ROOT/$CERTIFICATES_DATA_DIRECTORY/$CERTNAME.*

            if [ $? = 0 ]; then
                echo "success"
                echo "SERVICE_STATE=cleaned" > $TARGET_STATE/certificates.state
            else
                STATE="fail"
                echo "$STATE"
                echo "SERVICE_STATE=unknown" > $TARGET_STATE/certificates.state
            fi;
        done;
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$CERTIFICATES_DATA_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $CERTIFICATES_DATA_DIRECTORY | sed 's|/| |g'`"
        while [ "$CLEAN_DIRS_EMPTY" != "$TARGET_ROOT" ]; do
            cd $CLEAN_DIRS_EMPTY/..
            rmdir $CLEAN_DIRS_NAMES > /dev/null 2>&1 < /dev/null
            CLEAN_DIRS_EMPTY=`pwd`
        done;
    else
        echo "No created certificates found. use \"build\" first. "
    fi;
    echo ""
    ;;

    install)
    if [ -e $TARGET_ROOT/$CERTIFICATES_DATA_DIRECTORY ]; then
        echo "Installing generated certificates:"
        install -d -m 0755 -o root $CERTIFICATES_DATA_DIRECTORY
        for CERTNAME in $CERTLIST; do
            if [ -e $CERTIFICATES_DATA_DIRECTORY/$CERTNAME.crt ]; then
                echo "Old certificates found. Exiting.."
                exit 1
            else
                echo -n " * $CERTNAME: ";
                install -m 0644 -o root $TARGET_ROOT/$CERTIFICATES_DATA_DIRECTORY/$CERTNAME.* $CERTIFICATES_DATA_DIRECTORY

                if [ $? = 0 ]; then
                    echo "success"
                    echo "SERVICE_STATE=installed" > $TARGET_STATE/certificates.state
                else
                    STATE="fail"
                    echo "$STATE"
                    echo "SERVICE_STATE=unknown" > $TARGET_STATE/certificates.state
                fi;
            fi;
        done;
    else
         echo "No created certificates found. use \"build\" first. "
    fi;
    echo ""
    ;;
  
    uninstall)
    if [ -e $CERTIFICATES_DATA_DIRECTORY ]; then
        echo "Uninstalling generated certificates:"
        for CERTNAME in $CERTLIST; do
            echo -n " * $CERTNAME: ";
            rm $CERTIFICATES_DATA_DIRECTORY/$CERTNAME.*
            
            if [ $? = 0 ]; then
                echo "success"
                echo "SERVICE_STATE=uninstalled" > $TARGET_STATE/certificates.state
            else
                STATE="fail"
                echo "$STATE"
                echo "SERVICE_STATE=unknown" > $TARGET_STATE/certificates.state
            fi;
        done;
        rmdir $CERTIFICATES_DATA_DIRECTORY > /dev/null 2>&1 < /dev/null
    else
        echo "No installed certificate directory found; Are you sure you've"
        echo "specified the right platform for this machine? Or did you"
        echo "forget to use \"install\" in the first place?"
    fi
    echo ""
    ;;

    state)
    echo $SERVICE_STATE
    ;;

    deps)
    if [ $CERTIFICATES_PACKAGES_REQUIRED = 1 ]; then
        echo "$CERTIFICATES_PACKAGES_LISTING"
    fi;
    ;;

    *)
    echo $"Usage: $0 {build|clean|install|uninstall|state|deps}"
    exit 1
esac
