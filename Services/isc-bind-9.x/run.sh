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
SERVICE_NAME="dns"
SERVICE_IMPL="bind9"
SERVICE_DEPS="$BIND9_SERVICE_DEPS"
SERVICE_PRIO="15"
SERVICE_PORT="53"

# Check if globals we care about are set.
NAMESPACE=BIND9
CHECK="`set | grep ^${NAMESPACE}_`"
if [ -z "$CHECK" ]; then
    mkdir -p $TARGET_STATE
    echo "SERVICE_STATE=failed" > $TARGET_STATE/$SERVICE_IMPL.state
fi;
unset CHECK
unset NAMESPACE

# What are we working with?
BIND9_FILES_OTHERS="resolv.conf hosts"
BIND9_FILES_CONFIG="named.conf named.conf.options named.conf.master named.conf.slave rndc.key"
BIND9_FILES_DEFAULT="db.0 db.127 db.255 db.empty db.local db.root zones.rfc1918"


# Create ALLOWED_DOMAINS
COUNT=0
for RANGE in $DOMAIN_RANGES; do
    if [ "$COUNT" -eq 0 ]; then
        DOMAIN_NAME=`echo $RANGE | cut -d "/" -f 1`
        DOMAIN_TLD=`echo $RANGE | cut -d "/" -f 1`
    else
        DOMAIN_NAME=`echo $RANGE | cut -d "/" -f 1`.$DOMAIN_TLD
    fi;
    ALLOWED_DOMAINS="$ALLOWED_DOMAINS $DOMAIN_NAME"
    COUNT=`expr $COUNT "+" 1`
done;

# Create ALLOWED_RANGES and ALLOWED_IPS.
COUNT=0
for RANGE in $DOMAIN_RANGES; do
    COUNT=`expr $COUNT "+" 1`
done;
if [ "$COUNT" -eq 1 ]; then
    for RANGE in $DOMAIN_RANGES; do
        SERVER_ADDR=`echo $RANGE | cut -d "/" -f 2 | sed "s|.0$|.$SERVER_SUFFIX|g"`
        DOMAIN_ADDR=`echo $RANGE | cut -d "/" -f 2`
        DOMAIN_CIDR=`echo $RANGE | cut -d "/" -f 3`
        ALLOWED_RANGES="$ALLOWED_RANGES $DOMAIN_ADDR/$DOMAIN_CIDR;"
        ALLOWED_IPS="$ALLOWED_IPS $SERVER_ADDR;"
    done;
else
    COUNT=0
    for RANGE in $DOMAIN_RANGES; do
        SERVER_ADDR=`echo $RANGE | cut -d "/" -f 2 | sed "s|.0$|.$SERVER_SUFFIX|g"`
        DOMAIN_ADDR=`echo $RANGE | cut -d "/" -f 2`
        DOMAIN_CIDR=`echo $RANGE | cut -d "/" -f 3`
        if [ "$COUNT" -ne 0 ]; then
            ALLOWED_RANGES="$ALLOWED_RANGES $DOMAIN_ADDR/$DOMAIN_CIDR;"
            ALLOWED_IPS="$ALLOWED_IPS $SERVER_ADDR;"
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
    
    # Make sure our target directories exist.
    mkdir -p $TARGET_ROOT/$BIND9_CONFIG_DIRECTORY
    mkdir -p $TARGET_ROOT/$BIND9_DATA_DIRECTORY/default
    mkdir -p $TARGET_ROOT/$BIND9_DATA_DIRECTORY/master
    mkdir -p $TARGET_ROOT/$BIND9_DATA_DIRECTORY/slave

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

    echo "Populating default subdirectory with default files.."
    for FILE in $BIND9_FILES_DEFAULT; do
        cp $FILE $TARGET_ROOT/$BIND9_DATA_DIRECTORY/default/
    done;

    echo "Creating a default resolv.conf and hosts file.."
    for FILE in $BIND9_FILES_OTHERS; do
        cat $FILE   | sed "s|ALLOWED_DOMAINS|$ALLOWED_DOMAINS|g" \
                    > $TARGET_ROOT/etc/$FILE
    done;

    echo "Creating main named.conf.* files.."
    for FILE in $BIND9_FILES_CONFIG; do
        cat $FILE   | sed "s|BIND9_DATA_DIRECTORY|$BIND9_DATA_DIRECTORY|g" \
                    | sed "s|ALLOWED_RANGES|$ALLOWED_RANGES|g" \
                    | sed "s|ALLOWED_IPS|$ALLOWED_IPS|g" \
                    > $TARGET_ROOT/$BIND9_DATA_DIRECTORY/$FILE
    done;
    
    echo "Creating named.conf.master and named.conf.slave.."
    WORKLIST="named.conf.master named.conf.slave"
    for FILE in $WORKLIST; do
        rm $TARGET_ROOT/$BIND9_DATA_DIRECTORY/$FILE
        COUNT=0
        for RANGE in $DOMAIN_RANGES; do
            if [ "$COUNT" -eq 0 ]; then
                DOMAIN_NAME=`echo $RANGE | cut -d "/" -f 1`
            else
                DOMAIN_NAME=`echo $RANGE | cut -d "/" -f 1`.`echo $DOMAIN_RANGES | cut -d "/" -f 1`
            fi;
            DOMAIN_NAME_REVERSED="`echo $DOMAIN_NAME | awk 'BEGIN {FS="."}{for (i = NF; i > 0; --i) printf $i"."}' | sed "s|.$||g"`"
            DOMAIN_ADDR="`echo $RANGE | cut -d "/" -f 2 | sed "s|\.0$||g" | sed "s|\.0$||g"`"
            DOMAIN_ADDR_REVERSED="`echo $DOMAIN_ADDR | awk 'BEGIN {FS="."}{for (i = NF; i > 0; --i) printf $i"."}' | sed "s|.$||g"`"
            cat $FILE   | sed "s|DOMAIN_NAME_REVERSED|$DOMAIN_NAME_REVERSED|g" \
                        | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" \
                        | sed "s|DOMAIN_ADDR_REVERSED|$DOMAIN_ADDR_REVERSED|g" \
                        | sed "s|DOMAIN_ADDR|$DOMAIN_ADDR|g" \
                        | sed "s|ALLOWED_IPS|$ALLOWED_IPS|g" \
                        >> $TARGET_ROOT/$BIND9_DATA_DIRECTORY/$FILE

            COUNT=`expr $COUNT "+" 1`
        done;
    done;

    echo "Creating links for rndc.key, named.conf.local and named.conf.."
    cd $TARGET_ROOT/$BIND9_DATA_DIRECTORY
    if [ ! -e named.conf.local ]; then
        ln -s named.conf.master named.conf.local
    fi;
    cd -
    cd $TARGET_ROOT/$BIND9_CONFIG_DIRECTORY
    if [ ! -e named.conf ]; then
        ln -s $BIND9_DATA_DIRECTORY/named.conf named.conf
    fi;
    if [ ! -e rndc.key ]; then
        ln -s $BIND9_DATA_DIRECTORY/rndc.key rndc.key
    fi;
    cd -

    echo "Populating master subdirectory with generated databases.."
    COUNT=0
    for RANGE in $DOMAIN_RANGES; do
        if [ "$COUNT" -eq 1 ]; then
            # Set SERVER_ADDR here so we can use an existing IP for possibly overlapping ranges.. confusing huh.
            SERVER_ADDR=`echo $RANGE | cut -d "/" -f 2 | sed "s|.0$|.$SERVER_SUFFIX|g"`
        fi;
        COUNT=`expr $COUNT "+" 1`
    done;
    if [ "$COUNT" -eq 1 ]; then
        echo "One network in DOMAIN_RANGES, acting accordingly.."
        for RANGE in $DOMAIN_RANGES; do
            DOMAIN_NAME=`echo $RANGE | cut -d "/" -f 1`
            DOMAIN_NAME_REVERSED="`echo $DOMAIN_NAME | awk 'BEGIN {FS="."}{for (i = NF; i > 0; --i) printf $i"."}' | sed "s|.$||g"`"
            DOMAIN_ADDR="`echo $RANGE | cut -d "/" -f 2 | sed "s|\.0$||g" | sed "s|\.0$||g"`"
            DOMAIN_ADDR_REVERSED="`echo $DOMAIN_ADDR | awk 'BEGIN {FS="."}{for (i = NF; i > 0; --i) printf $i"."}' | sed "s|.$||g"`"
            DOMAIN_CIDR=`echo $RANGE | cut -d "/" -f 3`

            FILE=db.fwd_$DOMAIN_NAME_REVERSED
            cat db.fwd_DOMAIN_NAME_REVERSED-header  | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" \
                                                    | sed "s|DOMAIN_ADDR|$DOMAIN_ADDR|g" \
                                                    | sed "s|DOMAIN_CIDR|$DOMAIN_CIDR|g" \
                                                    | sed "s|SERVER_NAME|$SERVER_NAME|g" \
                                                    | sed "s|SERVER_ADDR|$SERVER_ADDR|g" \
                                                    > $TARGET_ROOT/$BIND9_DATA_DIRECTORY/master/$FILE

            cat db.fwd_DOMAIN_NAME_REVERSED-body    | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" \
                                                    | sed "s|DOMAIN_ADDR|$DOMAIN_ADDR|g" \
                                                    | sed "s|DOMAIN_CIDR|$DOMAIN_CIDR|g" \
                                                    | sed "s|SERVER_NAME|$SERVER_NAME|g" \
                                                    | sed "s|SERVER_ADDR|$SERVER_ADDR|g" \
                                                    >> $TARGET_ROOT/$BIND9_DATA_DIRECTORY/master/$FILE

            for ALIAS in $ALIASES; do
                echo "$ALIAS CNAME $SERVER_NAME" >> $TARGET_ROOT/$BIND9_DATA_DIRECTORY/master/$FILE
            done;

            FILE=db.rev_$DOMAIN_NAME_REVERSED
            cat db.rev_DOMAIN_NAME_REVERSED-header  | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g"                   \
                                                    | sed "s|DOMAIN_ADDR_REVERSED|$DOMAIN_ADDR_REVERSED|g" \
                                                    | sed "s|DOMAIN_ADDR|$DOMAIN_ADDR|g"                   \
                                                    | sed "s|DOMAIN_CIDR|$DOMAIN_CIDR|g"                   \
                                                    | sed "s|SERVER_NAME|$SERVER_NAME|g"                   \
                                                    | sed "s|SERVER_SUFFIX|$SERVER_SUFFIX|g"               \
                                                    > $TARGET_ROOT/$BIND9_DATA_DIRECTORY/master/$FILE

            cat db.rev_DOMAIN_NAME_REVERSED-sub     | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g"                   \
                                                    | sed "s|DOMAIN_ADDR_REVERSED|$DOMAIN_ADDR_REVERSED|g" \
                                                    | sed "s|DOMAIN_ADDR|$DOMAIN_ADDR|g"                   \
                                                    | sed "s|DOMAIN_CIDR|$DOMAIN_CIDR|g"                   \
                                                    | sed "s|SERVER_NAME|$SERVER_NAME|g"                   \
                                                    | sed "s|SERVER_SUFFIX|$SERVER_SUFFIX|g"               \
                                                    >> $TARGET_ROOT/$BIND9_DATA_DIRECTORY/master/$FILE
        done;
    else
        echo "More than one network in DOMAIN_RANGES, acting accordingly.."
        COUNT=0
        for RANGE in $DOMAIN_RANGES; do
            if [ "$COUNT" -eq 0 ]; then
                DOMAIN_NAME=`echo $RANGE | cut -d "/" -f 1`
                DOMAIN_NAME_REVERSED="`echo $DOMAIN_NAME | awk 'BEGIN {FS="."}{for (i = NF; i > 0; --i) printf $i"."}' | sed "s|.$||g"`"
                DOMAIN_ADDR="`echo $RANGE | cut -d "/" -f 2 | sed "s|\.0$||g" | sed "s|\.0$||g"`"
                DOMAIN_ADDR_REVERSED="`echo $DOMAIN_ADDR | awk 'BEGIN {FS="."}{for (i = NF; i > 0; --i) printf $i"."}' | sed "s|.$||g"`"
                DOMAIN_CIDR=`echo $RANGE | cut -d "/" -f 3`

                FILE=db.fwd_$DOMAIN_NAME_REVERSED
                cat db.fwd_DOMAIN_NAME_REVERSED-header  | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" \
                                                        | sed "s|DOMAIN_ADDR|$DOMAIN_ADDR|g" \
                                                        | sed "s|DOMAIN_CIDR|$DOMAIN_CIDR|g" \
                                                        | sed "s|SERVER_NAME|$SERVER_NAME|g" \
                                                        | sed "s|SERVER_ADDR|$SERVER_ADDR|g" \
                                                        > $TARGET_ROOT/$BIND9_DATA_DIRECTORY/master/$FILE

                cat db.fwd_DOMAIN_NAME_REVERSED-body    | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" \
                                                        | sed "s|DOMAIN_ADDR|$DOMAIN_ADDR|g" \
                                                        | sed "s|DOMAIN_CIDR|$DOMAIN_CIDR|g" \
                                                        | sed "s|SERVER_NAME|$SERVER_NAME|g" \
                                                        | sed "s|SERVER_ADDR|$SERVER_ADDR|g" \
                                                        >> $TARGET_ROOT/$BIND9_DATA_DIRECTORY/master/$FILE

                for ALIAS in $ALIASES; do
                    echo "$ALIAS CNAME $SERVER_NAME" >> $TARGET_ROOT/$BIND9_DATA_DIRECTORY/master/$FILE
                done;

                FILE=db.rev_$DOMAIN_NAME_REVERSED
                cat db.rev_DOMAIN_NAME_REVERSED-header  | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g"                   \
                                                        | sed "s|DOMAIN_ADDR_REVERSED|$DOMAIN_ADDR_REVERSED|g" \
                                                        | sed "s|DOMAIN_ADDR|$DOMAIN_ADDR|g"                   \
                                                        | sed "s|DOMAIN_CIDR|$DOMAIN_CIDR|g"                   \
                                                        | sed "s|SERVER_NAME|$SERVER_NAME|g"                   \
                                                        | sed "s|SERVER_SUFFIX|$SERVER_SUFFIX|g"               \
                                                        > $TARGET_ROOT/$BIND9_DATA_DIRECTORY/master/$FILE

                # Add subnet redirections only for embedded subnets and not for the first.
                INCREMENT=0
                for SUBNET in $DOMAIN_RANGES; do
                    if [ "$INCREMENT" -ne 0 ]; then
                        DOMAIN_NAME=`echo $SUBNET | cut -d "/" -f 1`.`echo $DOMAIN_RANGES | cut -d "/" -f 1`
                        DOMAIN_NAME_REVERSED="`echo $DOMAIN_NAME | awk 'BEGIN {FS="."}{for (i = NF; i > 0; --i) printf $i"."}' | sed "s|.$||g"`"
                        DOMAIN_ADDR="`echo $SUBNET | cut -d "/" -f 2 | sed "s|\.0$||g" | sed "s|\.0$||g"`"
                        DOMAIN_ADDR_REVERSED="`echo $DOMAIN_ADDR | awk 'BEGIN {FS="."}{for (i = NF; i > 0; --i) printf $i"."}' | sed "s|.$||g"`"
                        DOMAIN_CIDR=`echo $SUBNET | cut -d "/" -f 3`
                        cat db.rev_DOMAIN_NAME_REVERSED-tld     | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g"                   \
                                                                | sed "s|DOMAIN_ADDR_REVERSED|$DOMAIN_ADDR_REVERSED|g" \
                                                                | sed "s|DOMAIN_ADDR|$DOMAIN_ADDR|g"                   \
                                                                | sed "s|DOMAIN_CIDR|$DOMAIN_CIDR|g"                   \
                                                                | sed "s|SERVER_NAME|$SERVER_NAME|g"                   \
                                                                | sed "s|SERVER_SUFFIX|$SERVER_SUFFIX|g"               \
                                                                >> $TARGET_ROOT/$BIND9_DATA_DIRECTORY/master/$FILE
                    fi;
                    INCREMENT=`expr $INCREMENT "+" 1`
                done;
            else
                DOMAIN_NAME=`echo $RANGE | cut -d "/" -f 1`.`echo $DOMAIN_RANGES | cut -d "/" -f 1`
                DOMAIN_NAME_REVERSED="`echo $DOMAIN_NAME | awk 'BEGIN {FS="."}{for (i = NF; i > 0; --i) printf $i"."}' | sed "s|.$||g"`"
                DOMAIN_ADDR="`echo $RANGE | cut -d "/" -f 2 | sed "s|\.0$||g" | sed "s|\.0$||g" | sed "s|\.0$||g"`"
                DOMAIN_ADDR_REVERSED="`echo $DOMAIN_ADDR | awk 'BEGIN {FS="."}{for (i = NF; i > 0; --i) printf $i"."}' | sed "s|.$||g"`"
                DOMAIN_CIDR=`echo $RANGE | cut -d "/" -f 3`
                SERVER_ADDR=`echo $RANGE | cut -d "/" -f 2 | sed "s|.0$|.$SERVER_SUFFIX|g"`

                FILE=db.fwd_$DOMAIN_NAME_REVERSED
                cat db.fwd_DOMAIN_NAME_REVERSED-header  | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" \
                                                        | sed "s|DOMAIN_ADDR|$DOMAIN_ADDR|g" \
                                                        | sed "s|DOMAIN_CIDR|$DOMAIN_CIDR|g" \
                                                        | sed "s|SERVER_NAME|$SERVER_NAME|g" \
                                                        | sed "s|SERVER_ADDR|$SERVER_ADDR|g" \
                                                        > $TARGET_ROOT/$BIND9_DATA_DIRECTORY/master/$FILE

                cat db.fwd_DOMAIN_NAME_REVERSED-body    | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g" \
                                                        | sed "s|DOMAIN_ADDR|$DOMAIN_ADDR|g" \
                                                        | sed "s|DOMAIN_CIDR|$DOMAIN_CIDR|g" \
                                                        | sed "s|SERVER_NAME|$SERVER_NAME|g" \
                                                        | sed "s|SERVER_ADDR|$SERVER_ADDR|g" \
                                                        >> $TARGET_ROOT/$BIND9_DATA_DIRECTORY/master/$FILE
                for ALIAS in $ALIASES; do
                    echo "$ALIAS CNAME $SERVER_NAME" >> $TARGET_ROOT/$BIND9_DATA_DIRECTORY/master/$FILE
                done;

                FILE=db.rev_$DOMAIN_NAME_REVERSED
                cat db.rev_DOMAIN_NAME_REVERSED-header  | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g"                   \
                                                        | sed "s|DOMAIN_ADDR_REVERSED|$DOMAIN_ADDR_REVERSED|g" \
                                                        | sed "s|DOMAIN_ADDR|$DOMAIN_ADDR|g"                   \
                                                        | sed "s|DOMAIN_CIDR|$DOMAIN_CIDR|g"                   \
                                                        | sed "s|SERVER_NAME|$SERVER_NAME|g"                   \
                                                        | sed "s|SERVER_SUFFIX|$SERVER_SUFFIX|g"               \
                                                        > $TARGET_ROOT/$BIND9_DATA_DIRECTORY/master/$FILE

                cat db.rev_DOMAIN_NAME_REVERSED-sub     | sed "s|DOMAIN_NAME|$DOMAIN_NAME|g"                   \
                                                        | sed "s|DOMAIN_ADDR_REVERSED|$DOMAIN_ADDR_REVERSED|g" \
                                                        | sed "s|DOMAIN_ADDR|$DOMAIN_ADDR|g"                   \
                                                        | sed "s|DOMAIN_CIDR|$DOMAIN_CIDR|g"                   \
                                                        | sed "s|SERVER_NAME|$SERVER_NAME|g"                   \
                                                        | sed "s|SERVER_SUFFIX|$SERVER_SUFFIX|g"               \
                                                        >> $TARGET_ROOT/$BIND9_DATA_DIRECTORY/master/$FILE
            fi;
            COUNT=`expr $COUNT "+" 1`
        done;
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

    if [ -e $TARGET_ROOT/$BIND9_CONFIG_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$BIND9_CONFIG_DIRECTORY"
        cd $TARGET_ROOT/$BIND9_CONFIG_DIRECTORY
        rm -vf $BIND9_FILES_OTHERS named.conf rndc.key
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$BIND9_CONFIG_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $BIND9_CONFIG_DIRECTORY | sed 's|/| |g'`"
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
    if [ -e $TARGET_ROOT/$BIND9_DATA_DIRECTORY ]; then
        echo "Cleaning \$TARGET_ROOT$BIND9_DATA_DIRECTORY"
        cd $TARGET_ROOT/$BIND9_DATA_DIRECTORY
        rm -vf $BIND9_FILES_CONFIG named.conf.local
        rm -vf default/*
        rmdir default
        rm -vf master/*
        rmdir master
        rm -vf slave/*
        rmdir slave
        CLEAN_DIRS_EMPTY="$TARGET_ROOT/$BIND9_DATA_DIRECTORY"
        CLEAN_DIRS_NAMES="`echo $BIND9_DATA_DIRECTORY | sed 's|/| |g'`"
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

    if [ -e $TARGET_ROOT/$BIND9_CONFIG_DIRECTORY ]; then
        echo "Installing main configuration files:"
        install -d -m 0755 -o root $BIND9_CONFIG_DIRECTORY
        install -d -m 0755 -o root $BIND9_DATA_DIRECTORY
        for CONFIG in $BIND9_FILES_CONFIG; do
            echo -n " * $CONFIG: ";
            install -m 0755 -o root $TARGET_ROOT/$BIND9_DATA_DIRECTORY/$CONFIG $BIND9_DATA_DIRECTORY/$CONFIG

            if [ $? = 0 ]; then
                echo "success"
            else
                STATE="fail"
                echo "$STATE"
            fi;
        done;
        for CONFIG in $BIND9_FILES_OTHERS; do
            if [ -e $BIND9_CONFIG_DIRECTORY/$CONFIG ]; then
	            if [ ! -e $BIND9_CONFIG_DIRECTORY/$CONFIG.orig ]; then
                    cp $BIND9_CONFIG_DIRECTORY/$CONFIG $BIND9_CONFIG_DIRECTORY/$CONFIG.orig
		        fi;
            fi;
         
            echo -n " * $CONFIG: ";
            install -m 0755 -o root $TARGET_ROOT/$BIND9_CONFIG_DIRECTORY/$CONFIG $BIND9_CONFIG_DIRECTORY/$CONFIG

            if [ $? = 0 ]; then
                echo "success"
                echo "SERVICE_STATE=installed" > $TARGET_STATE/$SERVICE_IMPL.state
            else
                STATE="fail"
                echo "$STATE"
                echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE_IMPL.state
            fi;
        done;
        echo "Installing default named databases:"
        install -d -m 0755 -o root $BIND9_DATA_DIRECTORY/default
        for CONFIG in $BIND9_FILES_DEFAULT; do
            echo -n " * $CONFIG: ";
            install -m 0644 -o root $TARGET_ROOT/$BIND9_DATA_DIRECTORY/default/$CONFIG $BIND9_DATA_DIRECTORY/default/$CONFIG

            if [ $? = 0 ]; then
                echo "success"
                echo "SERVICE_STATE=installed" > $TARGET_STATE/$SERVICE_IMPL.state
            else
                STATE="fail"
                echo "$STATE"
                echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE_IMPL.state
            fi;
        done;
	echo "Installing generated master/slave databases:"
        install -d -m 0755 -o root $BIND9_DATA_DIRECTORY/master
        install -d -m 0755 -o root $BIND9_DATA_DIRECTORY/slave
        echo -n " * masterdb: ";
        install -m 0644 -o root $TARGET_ROOT/$BIND9_DATA_DIRECTORY/master/* $BIND9_DATA_DIRECTORY/master/

        if [ $? = 0 ]; then
            echo "success"
            echo "SERVICE_STATE=installed" > $TARGET_STATE/$SERVICE_IMPL.state
        else
            STATE="fail"
            echo "$STATE"
            echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE_IMPL.state
        fi;
	echo "Creating links for named.conf.local, named.conf, rndc.key:"
        echo -n " * links: ";
	cd $BIND9_DATA_DIRECTORY
	rm -f named.conf.local
	ln -s named.conf.master named.conf.local
	cd $BIND9_CONFIG_DIRECTORY
	rm -f named.conf
	ln -s $BIND9_DATA_DIRECTORY/named.conf named.conf
	rm -f rndc.key 
	ln -s $BIND9_DATA_DIRECTORY/rndc.key rndc.key
        if [ $? = 0 ]; then
            echo "success"
            echo "SERVICE_STATE=installed" > $TARGET_STATE/$SERVICE_IMPL.state
        else
            STATE="fail"
            echo "$STATE"
            echo "SERVICE_STATE=unknown" > $TARGET_STATE/$SERVICE_IMPL.state
        fi;
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

    echo "Removing links.."
    rm -f $BIND9_DATA_DIRECTORY/named.conf.local
    rm -f $BIND9_CONFIG_DIRECTORY/named.conf
    rm -f $BIND9_CONFIG_DIRECTORY/rndc.key
    echo "Removing config.."
    for CONFIG in $BIND9_FILES_CONFIG; do
        rm -f $BIND9_DATA_DIRECTORY/$CONFIG
    done;
    echo "Removing databases.."
    rm -f $BIND9_DATA_DIRECTORY/default/*
    rm -f $BIND9_DATA_DIRECTORY/master/*
    rm -f $BIND9_DATA_DIRECTORY/slave/*
    rmdir $BIND9_DATA_DIRECTORY/default
    rmdir $BIND9_DATA_DIRECTORY/master
    rmdir $BIND9_DATA_DIRECTORY/slave
    echo "Removing empty directories.."
    rmdir $BIND9_CONFIG_DIRECTORY > /dev/null 2>&1 < /dev/null
    rmdir $BIND9_DATA_DIRECTORY > /dev/null 2>&1 < /dev/null
    echo "Restoring hosts and resolv.conf originals if any.."
    for CONFIG in $BIND9_FILES_OTHERS; do
        if [ -e $BIND9_CONFIG_DIRECTORY/$CONFIG.orig ]; then
            cp $BIND9_CONFIG_DIRECTORY/$CONFIG.orig $BIND9_CONFIG_DIRECTORY/$CONFIG
        fi;
    done;
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
        if [ "$BIND9_INIT_STRING" ]; then
            if [ -e $BIND9_INIT_CONFIG ]; then
                if [ ! -e $BIND9_INIT_CONFIG.orig ]; then
                    cp $BIND9_INIT_CONFIG $BIND9_INIT_CONFIG.orig
                elif [ -e $BIND9_INIT_CONFIG.orig ]; then  
                    cp $BIND9_INIT_CONFIG.orig $BIND9_INIT_CONFIG
                fi;
            else
                echo "Mmm, your specified config file does not seem to exist:"
                echo "BIND9_INIT_CONFIG=$BIND9_INIT_CONFIG"
                echo ""
                echo "I will create it for you using BIND9_INIT_STRING as its content"
                echo "But I can't tell if this will be sufficient. Just so you know.."
                echo ""
            fi;
            echo "Appending BIND9_INIT_STRING to $BIND9_INIT_CONFIG.."
            echo "$BIND9_INIT_STRING" >> $BIND9_INIT_CONFIG
        fi;

        COUNT=$SERVICE_PRIO
        for BIND9_INIT_NAME in $BIND9_INIT_SCRIPTS; do
            if [ $PLATFORM_INIT_TYPE = "bsd" ]; then
                echo "Specified init type is $PLATFORM_INIT_TYPE. Acting accordingly.."
                if [ -e $PLATFORM_INIT_DIRECTORY/$BIND9_INIT_NAME ]; then
                    echo "Enabling boot-time startup for $BIND9_INIT_NAME in /etc/rc.conf."
                    echo "$BIND9_INIT_NAME=YES" >> /etc/rc.conf
                else
                    echo "No $BIND9_INIT_NAME init script(s) found!"
                    echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                    echo "init script(s): $BIND9_INIT_SCRIPTS"
                fi;
            elif [ $PLATFORM_INIT_TYPE = "sysv" ]; then
                echo "Specified init type is $PLATFORM_INIT_TYPE. Acting accordingly.."
                if [ -e $PLATFORM_INIT_DIRECTORY/$BIND9_INIT_NAME ]; then
                    echo "Enabling boot-time startup for $BIND9_INIT_NAME in runlevels 2,3,4,5."
                    for RUNLEVEL in 2 3 4 5; do
                        rm -f $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/*$BIND9_INIT_NAME
                        ln -s $PLATFORM_INIT_DIRECTORY/$BIND9_INIT_NAME $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/S$COUNT$BIND9_INIT_NAME
                    done;
                    for RUNLEVEL in 0 1 6; do
                        rm -f $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/*$BIND9_INIT_NAME
                        ln -s $PLATFORM_INIT_DIRECTORY/$BIND9_INIT_NAME $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/K$COUNT$BIND9_INIT_NAME
                    done;
                else
                    echo "No $BIND9_INIT_NAME init script(s) found!"
                    echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                    echo "init script(s): $BIND9_INIT_SCRIPTS"
                fi;
            else
                echo "The variable PLATFORM_INIT_TYPE was set to \"$PLATFORM_INIT_TYPE\";"
                echo "This is currently not supported. It has to be either bsd or sysv!"
            fi;
        
            # And start it up.
            echo "Starting up $BIND9_INIT_NAME.."
            if [ -e $PLATFORM_INIT_DIRECTORY/$BIND9_INIT_NAME ]; then
                $PLATFORM_INIT_DIRECTORY/$BIND9_INIT_NAME start
            else
                echo "No $BIND9_INIT_NAME init script(s) found!"
                echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                echo "init script(s): $BIND9_INIT_SCRIPTS"
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
    if [ "$BIND9_INIT_STRING" ]; then
        if [ -e $BIND9_INIT_CONFIG.orig ]; then
            cp $BIND9_INIT_CONFIG.orig $BIND9_INIT_CONFIG
        fi;
    fi;

    for BIND9_INIT_NAME in $BIND9_INIT_SCRIPTS; do
        if [ $PLATFORM_INIT_TYPE = "bsd" ]; then
            if [ -e $PLATFORM_INIT_DIRECTORY/$BIND9_INIT_NAME ]; then
                echo "$BIND9_INIT_NAME=NO" >> /etc/rc.conf
            else
                echo "No $BIND9_INIT_NAME init script(s) found!"
                echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                echo "init script(s): $BIND9_INIT_SCRIPTS"
            fi;
        elif [ $PLATFORM_INIT_TYPE = "sysv" ]; then
            if [ -e $PLATFORM_INIT_DIRECTORY/$BIND9_INIT_NAME ]; then
                for RUNLEVEL in 0 1 2 3 4 5 6; do
                    rm -f $PLATFORM_INIT_DIRECTORY/../rc$RUNLEVEL.d/*$BIND9_INIT_NAME
                done;
            else
                echo "No $BIND9_INIT_NAME init script(s) found!"
                echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
                echo "init script(s): $BIND9_INIT_SCRIPTS"
            fi;
        else
            echo "The variable PLATFORM_INIT_TYPE was set to \"$PLATFORM_INIT_TYPE\";"
            echo "This is currently not supported. It has to be either bsd or sysv!"
        fi;

        # And stop the service.
        echo "Stopping $BIND9_INIT_NAME.."
        if [ -e $PLATFORM_INIT_DIRECTORY/$BIND9_INIT_NAME ]; then
            $PLATFORM_INIT_DIRECTORY/$BIND9_INIT_NAME stop
        else
            echo "No $BIND9_INIT_NAME init script(s) found!"
            echo "I searched $PLATFORM_INIT_DIRECTORY for the following"
            echo "init script(s): $BIND9_INIT_SCRIPTS"
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
    if [ "$BIND9_PACKAGES_REQUIRED" = 1 ]; then
        echo "$BIND9_PACKAGES_LISTING"
    fi;
    ;;

    *)
    echo $"Usage: $0 {build|clean|install|uninstall|start|stop|state|deps}"
    exit 1
esac
