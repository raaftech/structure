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

################################################################################
# Name:     isomnt.sh                                                          #
# Function: configuration file isomnt.conf                                     #
# Version:  0.2                                                                #
################################################################################
# Return Codes:                                                                #
#      0   Succesful                                                           #
#      1   Not executed uid with root                                          #
#      2   Error in configuration file                                         #
################################################################################

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

# settings
config=$UNIVERSAL_HOME/Tools/utilities/isomnt.conf

# check if the script is executed as root
uid=`id | cut -d"=" -f2 | cut -d"(" -f1`
if [ ${uid} -ne 0 ] ; then
    echo "Script can be executed as user root only"
    exit 1
fi

# check if configuration file exists, if not, create a default one
if [ ! -f ${config} ] ; then
    cat > ${config} << EOF
# This is the config file for the isomnt.sh utility
# Syntax for this file is:
# <iso_source>:<target_mountpoint>
# Lines commented out with # are ignored
EOF
chmod 644 ${config}
fi

# do actual stuff
case $1 in
	online)
		# mount all the ISOs in the configuration file
		mntcnt=0
		cat ${config} | grep -v "^#" | grep -v "^$" | while read line
		do
			source=`echo ${line} | cut -d ':' -f 1`
			target=`echo ${line} | cut -d ':' -f 2`

			if [ ! -n "${source}" ] ; then
				echo "Error in configuration file on line: ${line}"
				exit 2
			fi
			
			if [ ! -n "${target}" ] ; then
				echo "Error in configuration file on line: ${line}"
				exit 2
			fi

			# mount source onto target
			mkdir -p ${target}
			mount -o loop=/dev/loop${mntcnt} "${source}" "${target}"
			mntcnt=`expr ${mntcnt} "+" 1`
		done 
	;;
	offline)
		cat ${config} | grep -v "^#" | grep -v "^$" | while read line
		do
			source=`echo ${line} | cut -d ':' -f 1`
			target=`echo ${line} | cut -d ':' -f 2`
			if [ ! -n "${source}" ] ; then
				echo "Error in configuration file on line: ${line}"
				exit 2
			fi
			
			if [ ! -n "${target}" ] ; then
				echo "Error in configuration file on line: ${line}"
				exit 2
			fi

			# unmount source from target
			umount "${target}"
		done
	;;
    list)
        cat ${config} | grep -v "^#" | grep -v "^$" | cut -d ":" -f 2
    ;;
	*)
		echo $"Usage: $0 {online|offline|list}"
	;;
esac
