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


# First see if 'which' is available. Bail out if not.
echo "Checking if which is available:"
STATE="success"
WHICH_LIST="/bin/which /sbin/which /usr/bin/which /usr/sbin/which"
echo -n " * which: ";
for RESULT in $WHICH_LIST; do
    if [ -x $RESULT ]; then
        # The which executable was found, STATE stays "success".
        export WHICH=$RESULT
        echo -n "$WHICH "
    fi;
done;
echo ""
echo ""

if [ ! $WHICH ]; then
    # The which executable was not found, set STATE to "fail".
    STATE=fail
	echo " * which: $STATE";
fi;

if [ $STATE = fail ]; then
    echo "The which program is not available on this system. This means the"
    echo "sanity checker cannot locate mandatory tools needed to run Universal."
    echo "Make sure to install which on this system."
    echo ""
    exit 1;
fi;


# Determine if mandatory tools are available.
echo "Checking if mandatory tools are available:"
STATE="success"
for TOOL in $MANDATORY_TOOLS; do
    echo -n " * $TOOL: ";
    if [ "`which $TOOL`" ]; then
        # The tool was found, STATE stays "success".
        echo "success"
    else
        # The tool was NOT found, set STATE to "fail".
        STATE="fail"
        echo "$STATE"
    fi;
done;
echo ""

if [ $STATE = fail ]; then
    echo "Some of the mandatory applications required to run this framework"
    echo "are not available. You need to review the list above and at least"
    echo "make sure that all these tools are available."
    echo ""
    exit 1;
fi;


# Test for existence of configuration file(s).
echo "Checking if configuration files are available:"
STATE="success"
    echo -n " * configuration: ";
    if [ -e "$CONFIG" ]; then
        # The file was found, STATE stays "success".
        echo `basename $CONFIG`
    else
        # The file was NOT found, set STATE to "fail".
        STATE="fail"
        echo "$STATE"
    fi;
echo ""

if [ $STATE = fail ]; then
    echo "Some of the required configuration files needed to run this framework"
    echo "are not available. You need to review the list above and at least"
    echo "make sure that all these files are available."
    echo ""
    exit 1;
fi;


# Test for existence of requested platform descriptor file
echo "Checking for an associated platform descriptor file:"
echo -n " * $REQUESTED_DESCRIPTOR: ";
STATE="success"
if [ -e "$UNIVERSAL_HOME/Platforms/$REQUESTED_DESCRIPTOR" ]; then
    # The file was found, STATE stays "success".
    echo "success"
else
    # The file was NOT found, set STATE to "fail".
    STATE="fail"
    echo "$STATE"
fi;
 echo ""

if [ $STATE = fail ]; then
    echo "The platform/version/architecture that you requested in setup.conf"
    echo "has no associated platform descriptor. You either need to create one"
    echo "yourself using one of the other platform descriptor files as an"
    echo "example or you need to change your platform/version/architecture"
    echo "settings in setup.conf. Take a look in the \"Platforms\" directory"
    echo "to get an idea of the platforms currently available."
    echo ""
    exit 1;
fi;


# Test for existence of requested role file
echo "Checking for an associated role file:"
echo -n " * $REQUESTED_ROLE: ";
STATE="success"
if [ -e "$UNIVERSAL_HOME/Roles/$REQUESTED_ROLE" ]; then
    # The file was found, STATE stays "success".
    echo "success"
else
    # The file was NOT found, set STATE to "fail".
    STATE="fail"
    echo "$STATE"
fi;
 echo ""

if [ $STATE = fail ]; then
    echo "The role that you requested in setup.conf has no associated role file"
    echo "in the \"Roles\" directory. You either need to create one yourself or"
    echo "or you need to change the role of the server-to-be to an existing one."
    echo ""
    exit 1;
fi;


# Compare requested and detected platforms.
echo "Checking configured platform, release and architecture:"
echo " * requested: $REQUESTED_PLATFORM $REQUESTED_RELEASE $REQUESTED_ARCHITECTURE"
echo " * detected: $DETECTED_PLATFORM $DETECTED_RELEASE $DETECTED_ARCHITECTURE"
echo ""

# TODO: Create platform, release detection routines

# TODO: Advise about differences between detected and requested

# Network configuration verification.
echo "Checking whether network interfaces are up"
echo -n " * listening on: ";
STATE="success"
if [ $REQUESTED_PLATFORM = cygwin -o $REQUESTED_PLATFORM = mingw ]; then
	netstat -na | grep $(getServerIp) >/dev/null 2>&1
else
	ifconfig -a | grep $(getServerIp) >/dev/null 2>&1
fi

if [ $? -eq 0 ] ; then
	echo $(getServerIp)
else
	STATE="fail"
	echo "${STATE}"
fi
echo ""

if [ $STATE = fail ] ; then
	echo "The ip address specified in setup.conf is not available."
	echo "Please make sure network configuration is correct."
	echo ""
	exit 1;
fi


# Attempt to create a list of open ports on the system.
echo "Attempting to list open ports on this system:"
echo -n " * open ports: ";
STATE="success"
if [ ! -e "$UNIVERSAL_HOME/Tools/sanity/netstat.$REQUESTED_PLATFORM$REQUESTED_RELEASE" ]; then
    echo "missing netstat.$REQUESTED_PLATFORM$REQUESTED_RELEASE invocation!"
    STATE="fail"
else
    # The environment.sh file calls netstat scripts too, so lets see if we have a ports.out first.
    if [ -e $UNIVERSAL_HOME/Includes/ports.out ] ; then
    	. $UNIVERSAL_HOME/Includes/ports.out
        if [ -z "$DETECTED_PORTS" ]; then
        	# We found a ports.out but with an empty DETECTED_PORTS. Retry.
        	echo "DETECTED_PORTS=\"`sh $UNIVERSAL_HOME/Tools/sanity/netstat.$REQUESTED_PLATFORM$REQUESTED_RELEASE`\"" > $UNIVERSAL_HOME/Includes/ports.out
        	. $UNIVERSAL_HOME/Includes/ports.out
        fi;
    fi;
    if [ -e $UNIVERSAL_HOME/Includes/ports.out ] ; then
        . $UNIVERSAL_HOME/Includes/ports.out
        if [ -z "$DETECTED_PORTS" ]; then
            STATE="fail"
            echo "no open ports detected? does netstat.$REQUESTED_PLATFORM$REQUESTED_RELEASE work?"
        else
            echo $DETECTED_PORTS
        fi;
    else
        STATE="fail"
        echo "${STATE}"
    fi;
fi;
echo ""

if [ $STATE = fail ] ; then
    echo "Not able to create a list of open ports on this system."
    if [ ! -e "netstat.$REQUESTED_PLATFORM$REQUESTED_RELEASE" ]; then
        echo "The port detection command for this platform should be:"
        echo ""  
        echo "$UNIVERSAL_HOME/Tools/sanity/netstat.$REQUESTED_PLATFORM$REQUESTED_RELEASE"
    fi;
    echo ""
    echo "I will continue anyway, but keep in mind that I cannot"
    echo "determine if a service currently running on your system"
    echo "conflicts with a service you're setting up with Universal."
    echo ""
fi


# Handle firewall checks.
echo "Firewall configuration:"
if [ ! -z $PLATFORM_FW_TYPE ]; then
    echo -n " * $PLATFORM_FW_TYPE: ";
    STATE="success"
    $UNIVERSAL_HOME/Tools/firewall/run.sh state
    if [ $? != 0 ] ; then
	    STATE="fail"
    fi;
    echo ""

    if [ $STATE = fail ] ; then
        echo "The $PLATFORM_FW_TYPE firewall is available on this platform."
        echo "If you're using $PLATFORM_FW_TYPE, please make sure it's"
        echo "configured to handle the services you're setting up!"
        echo ""
    fi
else
    echo  " * not applicable"
    echo ""
fi;

# Handle mandatory access control checks.
echo "Mandatory Access Control configuration:"
if [ ! -z $PLATFORM_MAC_TYPE ]; then
    echo -n " * $PLATFORM_MAC_TYPE: ";
    STATE="success"
    $UNIVERSAL_HOME/Tools/security/run.sh state
    if [ $? != 0 ] ; then
	    STATE="fail"
    fi;
    echo ""

    if [ $STATE = fail ] ; then
        echo "The $PLATFORM_MAC_TYPE mandatory access control framework is"
        echo "available on this platform. If you're using $PLATFORM_MAC_TYPE,"
        echo "please make sure it's configured to handle the services"
        echo "you're setting up!"
        echo ""
    fi
else
    echo  " * not applicable"
    echo ""
fi;



# Tell us if additional repositories need to be connected.
echo "Checking if additional repositories are configured:"
STATE="installed"
if [ ! -z "$PACKAGING_REPOSITORY_LIST" ]; then
    for REPO in $PACKAGING_REPOSITORY_LIST; do
        echo -n " * $REPO: ";
        STATE="`$UNIVERSAL_HOME/Tools/packaging/run.sh state`"
        echo $STATE
    done;
else
    echo  " * none required"
fi;
echo ""

if [ "$STATE" != installed ] ; then
    echo "This platform specifies that additional repositories"
    echo "are required. The packaging module of Universal knows"
    echo "about this and returns the \"$STATE\" state for the"
    echo "required repositories."
    echo ""
    echo "If you have not already connected to the mentioned"
    echo "repositories, try running the following commands:"
    echo ""
    echo "$UNIVERSAL_HOME/Tools/packaging/run.sh build"
    echo "$UNIVERSAL_HOME/Tools/packaging/run.sh install"
    echo ""
    echo "If you have connected to the required repositories"
    echo "without using Universal, you can ignore this message."
    echo ""
fi
