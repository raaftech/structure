#!/bin/bash

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
            fi
        fi
    done;
    unset DIR
    unset IFS
    OFFSET="/../../"
    if [ -e "$(dirname "$ABS")$OFFSET" ]; then
        # Found, probably called relatively.
        cd "$(dirname "$ABS")$OFFSET"
        UNIVERSAL_HOME="$(pwd)"
        unset ABS
    elif [ -e "$(dirname "$0")$OFFSET" ]; then
        # Found, probably called absolutely.
        cd "$(dirname "$0")$OFFSET"
        UNIVERSAL_HOME="$(pwd)"
    fi
fi

# Workaround OS being set on Windows by default.
unset OS

# Include global setup, environment and functions.
if [ ! $GLOBAL_RUN ]; then
    . $UNIVERSAL_HOME/Includes/environment.sh
    . $UNIVERSAL_HOME/Includes/functions.sh
fi


# Local variables.
DHCP3_ENTRY="$UNIVERSAL_HOME/Tools/utilities/dhcp3.entry"
BIND9_FWD_ENTRY="$UNIVERSAL_HOME/Tools/utilities/bind9-fwd.entry"
BIND9_REV_ENTRY="$UNIVERSAL_HOME/Tools/utilities/bind9-rev.entry"

# Configuration.
. $UNIVERSAL_HOME/Tools/utilities/hostctl.conf

COUNT=0
for RANGE in $DOMAIN_RANGES; do
    if [ "$COUNT" -eq 0 ]; then
        DOMAIN_NAME=$(echo $RANGE | cut -d "/" -f 1)
        DOMAIN_TLD=$(echo $RANGE | cut -d "/" -f 1)
    else
        DOMAIN_NAME=$(echo $RANGE | cut -d "/" -f 1).$DOMAIN_TLD
    fi
    DOMAIN_REVERSED=$(echo $DOMAIN_NAME | awk 'BEGIN {FS="."}{for (i = NF; i > 0; --i) printf $i"."}' | sed "s|.$||g")
    ALLOWED_DOMAINS="$ALLOWED_DOMAINS $DOMAIN_NAME"
    ALLOWED_DOMAINS_REVERSED="$ALLOWED_DOMAINS_REVERSED $DOMAIN_REVERSED"
    COUNT=$(expr $COUNT "+" 1)
done;
unset COUNT

for RANGE in $DOMAIN_RANGES; do
    SERVER_ADDR=$(echo $RANGE | cut -d "/" -f 2 | sed "s|.0$|.$SERVER_SUFFIX|g")
    DOMAIN_ADDR=$(echo $RANGE | cut -d "/" -f 2)
    DOMAIN_CIDR=$(echo $RANGE | cut -d "/" -f 3)
    ALLOWED_RANGES="$ALLOWED_RANGES $DOMAIN_ADDR/$DOMAIN_CIDR;"
    ALLOWED_IPS="$ALLOWED_IPS $SERVER_ADDR;"
done;


# revIp(): Reverse an IP address
revIp() {
    local IP="$1"
    echo "$IP" | awk 'BEGIN {FS="."}{for (i = NF; i > 0; --i) printf $i"."}' | sed 's|.$||g'
}

revNet() {
    local NET="$1"
    echo "$NET" | awk 'BEGIN {FS="."}{for (i = NF; i > 0; --i) printf $i"."}' | sed 's|.$||g'
}

# getBootFiles(): Scan tftp bootarea for known bootfiles and display them.
getBootFiles() {
    for KBF in $TFTP_KNOWN_BOOTFILES; do
        find $TFTP1_ROOT -name $KBF 
    done
}

# getBootFile(): Return correct bootfile for specific OS.
getBootFile() {
    local OS="$1"
    getBootFiles | grep -w "$OS" | sed -e "s|$TFTP1_ROOT/||g" 
}

# getOs(): Get OS for given host in given network.
getOs() {
    local NET="$1"
    local HOST="$2"
    grep -A4 -w "^host ${HOST}_on_network_$NET" "$DHCP3_ROOT/dhcpd.conf" | grep 'filename' | tail -n1  | sed 's/^[ \t]*//;s/[ \t]*$//' | cut -d '=' -f 2 | sed 's|;||g' | sed 's|"||g' | awk -F / '{print $1 "/" $2 "/" $3}'
}

# getOsList(): List all available operating systems.
getOsList(){
    getBootFiles | sed -e "s|$TFTP1_ROOT/||g;s|/[^\/]*$||g" | grep -ve debian-installer -ve ubuntu-installer | sort -u
}

# getMac(): Get MAC for given host in given network.
getMac() {
    local NET="$1"
    local HOST="$2"
    grep -A4 -w "^host ${HOST}_on_network_$NET" "$DHCP3_ROOT/dhcpd.conf" | grep 'hardware ethernet' | tail -n1 | sed 's/^[ \t]*//;s/[ \t]*$//' | cut -d ' ' -f 3 | sed 's|;||g'
}

# getMacList(): List all used MAC addresses.
getMacList() {
    grep 'hardware ethernet' "$DHCP3_ROOT/dhcpd.conf" | sed 's/^[ \t]*//;s/[ \t]*$//' | cut -d ' ' -f 3 | sed 's|;||g'
}

# getIp(): Get IP for given host in given network.
getIp() {
    local NET="$1"
    local HOST="$2"
    local NET_REVERSED="$(revNet "$NET")"
    cat "$BIND9_ROOT/master/db.fwd_$NET_REVERSED" | grep -w "IN A" | grep -w "^$HOST" | cut -d ' ' -f 4
}

# getIpList(): List all used IP numbers.
getIpList() {
    cat "$BIND9_ROOT/master/db.fwd_"* | grep -w "IN A" | cut -d ' ' -f 4 | sort -u
}

# getHost(): Verify a host exists.
getHost() {
    local NET="$1"
    local HOST="$2"
    local NET_REVERSED="$(revNet "$NET")"
    cat $BIND9_ROOT/master/db.fwd_$NET_REVERSED | grep -w "IN A" | grep -w "^$HOST" | cut -d ' ' -f 1
}

# getHostList(): List all hosts.
getHostList() {
    local NET="$1"
    local NET_REVERSED="$(revNet "$NET")"

    if [ "$NET" ]; then
        cat "$BIND9_ROOT/master/db.fwd_$NET_REVERSED"* | grep -w "IN A" | cut -d ' ' -f 1 | sort -u
    else
        cat "$BIND9_ROOT/master/db.fwd_"* | grep -w "IN A" | cut -d ' ' -f 1 | sort -u
    fi
}

# getNet(): Verify a network exists.
getNet() {
    local NET="$1"
    if [[ "$ALLOWED_DOMAINS" =~ "$NET" ]]; then
        echo "$NET"
    fi
}

# getNetList(): List all networks.
getNetList() {
    local NET
    for NET in $ALLOWED_DOMAINS; do
        echo "$NET"
    done
}

# getRange(): Lookup a range for a given network.
getRange() {
    local NET="$1"
    local DOMAIN
    local RANGE

    local COUNT_DOMAIN=0
    for DOMAIN in $ALLOWED_DOMAINS; do
        local COUNT_RANGE=0
        for RANGE in $ALLOWED_RANGES; do
            if [ "$NET" = "$DOMAIN" -a "$COUNT_DOMAIN" = "$COUNT_RANGE" ]; then echo $RANGE | sed 's|;||g' ; fi
            COUNT_RANGE=$(expr $COUNT_RANGE + 1)
        done
        COUNT_DOMAIN=$(expr $COUNT_DOMAIN + 1)
    done
}

# getRangeList(): List all network ip ranges.
getRangeList() {
    local RANGE 
    for RANGE in $ALLOWED_RANGES; do
        echo "$RANGE" | sed 's|;||g'
    done
}

# verifyNetEntry(): Verify a network.
verifyNetEntry() {
    local NET="$1"
    local INVERT="$2"

    local CHECK_INPUT="$NET"
    if [ -z "$CHECK_INPUT" ]; then 
        echo "No network passed."
        return 1
    fi

    local CHECK_FORMAT="$(checkNetFormat $NET)"
    if [ ! "$CHECK_FORMAT" = "$NET" ]; then
        echo "Invalid network name: $NET"
        return 1
    fi

    local CHECK_VALID_DOMAIN=$(echo $ALLOWED_DOMAINS | grep -w "$NET")
    if [ -z "$CHECK_VALID_DOMAIN" ]; then
        echo "Invalid net name: $NET not in ($ALLOWED_DOMAINS)"
        return 1
    fi

    local CHECK_EXISTS="$(grep -w "$NET" "$DHCP3_ROOT/dhcpd.conf")"
    if [ ! "$INVERT" ]; then
        if [ ! -z "$CHECK_EXISTS" ]; then
            echo "Existing network: $NET"
            return 1
        fi
    else
        if [ -z "$CHECK_EXISTS" ]; then
            echo "Non-existing network: $NET"
            return 1
        fi
    fi
}

# verifyHostEntry(): Verify a hostname.
verifyHostEntry() {
    local HOST="$1"
    local INVERT="$2"

    local CHECK_INPUT="$HOST"
    if [ -z "$CHECK_INPUT" ]; then 
        echo "No hostname passed."
        return 1
    fi

    local CHECK_FORMAT="$(checkHostFormat $HOST)"
    if [ ! "$CHECK_FORMAT" = "$HOST" ]; then
        echo "Invalid hostname: $HOST"
        return 1
    fi

    local CHECK_EXISTS="$(cat $BIND9_ROOT/master/db.fwd_$NET_REVERSED* | grep -w "IN A" | grep -w "^$HOST")"
    if [ "$CHECK_EXISTS" ]; then
        # Existing host name
        if [ ! "$INVERT" ]; then RC=1; else RC=0; fi
    else
        # Non-existing host name
        if [ ! "$INVERT" ]; then RC=0; else RC=1; fi
    fi

    return $RC
}

# verifyIpEntry(): Verify an ip address.
verifyIpEntry() {
    local IP="$1"
    local INVERT="$2"

    local CHECK_INPUT="$IP"
    if [ -z "$CHECK_INPUT" ]; then 
        echo "No ip address passed."
        return 1
    fi

    local CHECK_FORMAT="$(checkIpFormat $IP)"
    if [ ! "$CHECK_FORMAT" = "$IP" ]; then
        echo "Invalid ip address: $IP"
        return 1
    fi

    local CHECK_IN_RANGE

    CHECK_IN_RANGE=0
    local IP_INT=$(ip2Int $IP)
    for AR in $ALLOWED_RANGES; do
        local IP_MIN=$(echo $AR|cut -d'/' -f 1)
        local IP_MIN_INT=$(ip2Int $IP_MIN)
        local NO_OF_IPS=$(echo $AR|awk -F '/' '{ print 2^(32-$NF) }')
        local IP_MAX_INT=$(expr $IP_MIN_INT + $NO_OF_IPS)
        if [ $IP_MIN_INT -lt $IP_INT -a $IP_MAX_INT -gt $IP_INT ] ; then
            CHECK_IN_RANGE=1
        fi
    done
    if [ $CHECK_IN_RANGE -ne 1 ]; then
        echo "Invalid IP address: $IP is not in ($ALLOWED_RANGES)"
        return 1
    fi

    local CHECK_EXISTS="$(getIpList | grep -w "$IP")"
    if [ ! "$INVERT" ]; then
        if [ ! -z "$CHECK_EXISTS" ]; then
            echo "Existing ip address: $IP"
            return 1
        fi
    else
        if [ -z "$CHECK_EXISTS" ]; then
            echo "Non-existing ip address: $IP"
            return 1
        fi
    fi
}

# verifyMacEntry(): Verify a mac address.
verifyMacEntry() {
    local MAC="$1"
    local INVERT="$2"

    local CHECK_INPUT="$MAC"
    if [ -z "$CHECK_INPUT" ]; then 
        echo "No mac address passed."
        return 1
    fi

    local CHECK_FORMAT="$(checkMacFormat $MAC)"
    if [ ! "$CHECK_FORMAT" = "$MAC" ]; then
        echo "Invalid mac address: $MAC"
        return 1
    fi

    local CHECK_EXISTS="$(getMacList | grep -w "$MAC")"
    if [ ! "$INVERT" ]; then
        if [ ! -z "$CHECK_EXISTS" ]; then
            echo "Existing mac address: $MAC"
            return 1
        fi
    else
        if [ -z "$CHECK_EXISTS" ]; then
            echo "Non-existing mac address: $MAC"
            return 1
        fi
    fi
}

# verifyOsEntry(): Verify an os.
verifyOsEntry() {
    local OS="$1"
    local INVERT="$2"

    local CHECK_INPUT="$OS"
    if [ -z "$CHECK_INPUT" ]; then 
        echo "No operating system passed."
        return 1
    fi

    local CHECK_FORMAT="$(checkOsFormat $OS)"
    if [ ! "$CHECK_FORMAT" = "$OS" ]; then
        echo "Invalid operating system name: $OS"
        return 1
    fi

    local CHECK_EXISTS=$(getOsList | grep -w "$OS")
    if [ ! "$1" ]; then
        if [ ! -z "$CHECK_EXISTS" ]; then
            echo "Existing operating system: $OS"
            return 1
        fi
    else
        if [ -z "$CHECK_EXISTS" ]; then
            echo "Non-existing operating system: $OS"
            return 1
        fi
    fi
}

# addHost(): Add a host to the system.
addHost() {
    local NET="$1"
    local HOST="$2"
    local IP="$3"
    local MAC="$4"
    local OS="$5"

    # Mandatory values.
    verifyNetEntry "$NET" invert || return 1
    verifyHostEntry "$HOST" || return 1
    verifyIpEntry "$IP" || return 1

    # Optional values.
    if [ "$MAC" ]; then verifyMacEntry "$MAC" || return 1; fi
    if [ "$OS" ]; then verifyOsEntry "$OS" invert || return 1; fi

    # Where we at.
    local IP_LAST=$(echo $IP | cut -d "." -f 4)
    local IP_ADDRARPA="$(echo $IP_REVERSED | sed  "s|$IP_LAST\.||").in-addr.arpa"
    local POINT_ORIGIN="$(cat $BIND9_ROOT/master/db.rev_$NET_REVERSED | grep -n "ORIGIN $IP_ADDRARPA"| cut -d ":" -f 1)"

    # Something went wrong.
    if [ -z "$POINT_ORIGIN" ];then
        echo "Could not find an origin statement which covers the IP range for: $IP"
        return 1;
    elif [ $POINT_ORIGIN -le 0 ]; then
        echo "Somehow point origin ended up as: $POINT_ORIGIN"
        return 1;
    fi

    # We now know where to write.
    local POINT_INSERT=$(expr $POINT_ORIGIN "+" 1)
    if [ "$OS" ]; then
        if [ -e "$TFTP1_ROOT/$OS/root-path.txt" ]; then
            local ROOTPATH="$(cat "$TFTP1_ROOT/$OS/root-path.txt")"
            local ROOTPATH_LINE="    option root-path=\"$ROOTPATH\";"
        fi
        local BOOTFILE_LINE="    filename=\"$(getBootFile $OS)\";\n"
    else
        local ROOTPATH_LINE=""
        local BOOTFILE_LINE=""
    fi

    # If MAC address was set, do DHCPd handling.
    if [ "$MAC" ]; then
        cat $DHCP3_ENTRY    | sed "s|MAC|$MAC|g" \
                            | sed "s|IP|$IP|g" \
                            | sed "s|HOST|$HOST|g" \
                            | sed "s|NET|$NET|g" \
                            | sed "s|OS|$OS|g" \
                            | sed "s|ROOTPATH_LINE|$ROOTPATH_LINE|g" \
                            | sed "s|BOOTFILE_LINE|$BOOTFILE_LINE|g" \
                            | sed '/^$/d' \
                            >> $DHCP3_ROOT/dhcpd.conf
    fi

    cat $BIND9_FWD_ENTRY    | sed "s|IP|$IP|g" \
                            | sed "s|HOST|$HOST|g" \
                            | sed "s|NET|$NET|g" \
                            | sed "s|OS|$OS|g" \
                            >> $BIND9_ROOT/master/db.fwd_$NET_REVERSED

    cat $BIND9_REV_ENTRY    | sed "s|IP_LAST|$IP_LAST|g" \
                            | sed "s|HOST|$HOST|g" \
                            | sed "s|NET|$NET|g" \
                            | sed "s|OS|$OS|g" \
                            > /tmp/tmprev.entry

    cat $BIND9_ROOT/master/db.rev_$NET_REVERSED | sed "${POINT_INSERT}i\\$(cat /tmp/tmprev.entry)" \
                            > /tmp/db.rev_$NET_REVERSED

    mv /tmp/db.rev_$NET_REVERSED $BIND9_ROOT/master/db.rev_$NET_REVERSED
    rm /tmp/tmprev.entry
}

# modHost(): Modify a host on the system.
modHost() {
    local NET="$1"
    local HOST="$2"
    local IP="$3"
    local MAC="$4"
    local OS="$5"
    local NET_NEW="$6"
    local HOST_NEW="$7"

    # Check given network(s).
    if [ "$NET" -a "$NET_NEW" -a "$NET" != "$NET_NEW" ]; then 
        verifyNetEntry "$NET" invert || return 1
        verifyNetEntry "$NET_NEW" invert || return 1
    elif [ "$NET" -a "$NET_NEW" -a "$NET" = "$NET_NEW" ]; then 
        verifyNetEntry "$NET" invert || return 1
    elif [ "$NET_NEW" ]; then 
        verifyNetEntry "$NET_NEW" invert || return 1
        local NET="$NET_NEW"
        local NET_REVERSED="$NET_NEW_REVERSED"
    elif [ "$NET" ]; then 
        verifyNetEntry "$NET" invert || return 1
        local NET_NEW="$NET"
        local NET_NEW_REVERSED="$NET_REVERSED"
    elif [ ! "$HOST" ]; then
        echo "Please specify network to edit in (-n and/or -nn)"
        return 1
    fi

    # Check given hostname(s).
    if [ "$HOST" -a "$HOST_NEW" -a "$HOST" != "$HOST_NEW" ]; then 
        verifyHostEntry "$HOST" invert || return 1
        verifyHostEntry "$HOST_NEW" || return 1
    elif [ "$HOST" -a "$HOST_NEW" -a "$HOST" = "$HOST_NEW" ]; then 
        verifyHostEntry "$HOST" invert || return 1
    elif [ "$HOST_NEW" ]; then 
        verifyHostEntry "$HOST_NEW" invert || return 1
        local HOST="$HOST_NEW"
    elif [ "$HOST" ]; then 
        verifyHostEntry "$HOST" invert || return 1
        local HOST_NEW="$HOST"
    elif [ ! "$HOST" ]; then
        echo "Please specify host to edit (-h and/or -hh)"
        return 1
    fi

    # Check IP number.
    local CURRENT_IP="$(getIp "$NET" "$HOST")"
    if [ "$IP" -a "$IP" != "$CURRENT_IP" ]; then 
        verifyIpEntry "$IP" || return 1
    else
        IP="$CURRENT_IP"
        IP_REVERSED="$(revIp "$IP")"
    fi

    # Check MAC address.
    local CURRENT_MAC="$(getMac "$NET" "$HOST")"
    if [ "$MAC" -a  "$MAC" != "$CURRENT_MAC" ]; then 
        verifyMacEntry "$MAC" || return 1
    else
        MAC="$CURRENT_MAC"
    fi

    # Check current OS.
    local CURRENT_OS="$(getOs "$NET" "$HOST")"
    if [ "$OS" -a  "$OS" != "$CURRENT_OS" ]; then 
        verifyOsEntry "$OS" invert || return 1
    else
        OS="$CURRENT_OS"
    fi

    delHost "$NET" "$HOST"
    if [ "$NET" != "$NET_NEW" ]; then NET="$NET_NEW"; fi
    if [ "$NET_REVERSED" != "$NET_NEW_REVERSED" ]; then NET_REVERSED="$NET_NEW_REVERSED"; fi
    if [ "$HOST" != "$HOST_NEW" ]; then HOST="$HOST_NEW"; fi
    addHost "$NET" "$HOST" "$IP" "$MAC" "$OS"
}

# delHost(): Remove a host from the system.
delHost() {
    local NET="$1"
    local NET_REVERSED="$(revNet "$NET")"
    local HOST="$2"
    local IP="$(getIp "$NET" "$HOST")"
    local IP_REVERSED="$(revIp "$IP")"

    # Verify entry exists.
    verifyNetEntry "$NET" invert || return 1
    verifyHostEntry "$HOST" invert || return 1

    # Where we at.
    local IP_LAST=$(echo $IP | cut -d "." -f 4)
    local IP_ADDRARPA="$(echo $IP_REVERSED | sed  "s|$IP_LAST\.||").in-addr.arpa"
    local POINT_ORIGIN="$(cat $BIND9_ROOT/master/db.rev_$NET_REVERSED | grep -n "ORIGIN $IP_ADDRARPA"| cut -d ":" -f 1)"

    # Something went wrong.
    if [ -z "$POINT_ORIGIN" ];then
        echo "Could not find an origin statement which covers the IP range for: $IP"
        return 1;
    elif [ $POINT_ORIGIN -le 0 ]; then
        echo "Somehow point origin ended up as: $POINT_ORIGIN"
        return 1;
    fi

    # If we have a MAC address, clean up DHCPd entry too.
    local MAC="$(getMac "$NET" "$HOST")"
    if [ "$MAC" ]; then
        cat $DHCP3_ROOT/dhcpd.conf  | sed -e "/# Reservation for $HOST.$NET/,/^\}/d" > /tmp/dhcpd.conf.tmp 
        mv /tmp/dhcpd.conf.tmp $DHCP3_ROOT/dhcpd.conf
    fi

    # Remove DNS forward entry.
    cat $BIND9_ROOT/master/db.fwd_$NET_REVERSED | sed -e "/$HOST IN A /d" > /tmp/db.fwd_$NET_REVERSED
    mv /tmp/db.fwd_$NET_REVERSED $BIND9_ROOT/master/db.fwd_$NET_REVERSED

    # Remove DNS reverse entry.
    cat $BIND9_ROOT/master/db.rev_$NET_REVERSED | sed -e "/ IN PTR $HOST.$NET./d" > /tmp/db.rev_$NET_REVERSED
    mv /tmp/db.rev_$NET_REVERSED $BIND9_ROOT/master/db.rev_$NET_REVERSED
}

# lstHost(): List a host on the system.
lstHost() {
    local NET="$1"
    local HOST="$2"
    local IP="$3"
    local MAC="$4"
    local OS="$5"

    # Optional values.
    if [ "$NET" ]; then verifyNetEntry "$NET" invert || return 1; fi
    if [ "$HOST" ]; then verifyHostEntry "$HOST" invert || return 1; fi
    if [ "$IP" ]; then verifyIpEntry "$IP" invert || return 1; fi
    if [ "$MAC" ]; then verifyMacEntry "$MAC" invert || return 1; fi
    if [ "$OS" ]; then verifyOsEntry "$OS" invert || return 1; fi

    # If os list was requested, list the available osses.
    if [ "$OS_LIST"  -a "$OS" ]; then
        printf "%s\n" "Os"
        getOsList | grep -w "$OS"
        return $?
    elif [ "$OS_LIST"  -a ! "$OS" ]; then
        printf "%s\n" "Os"
        getOsList
        return $?
    fi

    # If domain list was requested, list the available networks.
    if [ "$DOMAIN_LIST"  -a "$DOMAIN" ]; then
        printf "%-17%s-11s\n" "Network " "Range "
        DOMAIN="$(getNet "$DOMAIN")"
        RANGE="$(getRange "$DOMAIN")"
        [ "$DOMAIN" ] && printf "%-17s%-11s\n" "$DOMAIN " "$RANGE "
        return $?
    elif [ "$DOMAIN_LIST"  -a ! "$DOMAIN" ]; then
        printf "%-17s%-11s\n" "Network " "Range "
        DOMAIN_LIST=$(getNetList)
        for DOMAIN in $DOMAIN_LIST; do
            RANGE=$(getRange "$DOMAIN")
            printf "%-17s%-11s\n" "$DOMAIN " "$RANGE "
        done
        return $?
    fi

    # Print header.
    local PFORMAT="%-17s%-11s%-16s%-18s%-18s\n"
    printf "$PFORMAT" "Network " "Hostname " "Ip " "Mac " "Os "

    if [ "$NET" ]; then
        if [ "$HOST" ]; then
            IP="$(getIp "$NET" "$HOST")"
            MAC="$(getMac "$NET" "$HOST")"
            OS="$(getOs "$NET" "$HOST")"
            [ "$IP" ] && printf "$PFORMAT" "$NET " "$HOST " "$IP " "$MAC " "$OS "
        else
            HOST_LIST="$(getHostList "$NET")"
            for HOST in $HOST_LIST; do
                IP="$(getIp "$NET" "$HOST")"
                MAC="$(getMac "$NET" "$HOST")"
                OS="$(getOs "$NET" "$HOST")"
                [ "$IP" ] && printf "$PFORMAT" "$NET " "$HOST " "$IP " "$MAC " "$OS "
            done
        fi
    else
        if [ "$HOST" ]; then
            for NET in $ALLOWED_DOMAINS; do
                NET_REVERSED="$(revNet "$NET")"
                IP="$(getIp "$NET" "$HOST")"
                MAC="$(getMac "$NET" "$HOST")"
                OS="$(getOs "$NET" "$HOST")"
                [ "$IP" ] && printf "$PFORMAT" "$NET " "$HOST " "$IP " "$MAC " "$OS "
            done
        else
            for NET in $ALLOWED_DOMAINS; do
                NET_REVERSED="$(revNet "$NET")"
                HOST_LIST="$(getHostList "$NET")"
                for HOST in $HOST_LIST; do
                    IP="$(getIp "$NET" "$HOST")"
                    MAC="$(getMac "$NET" "$HOST")"
                    OS="$(getOs "$NET" "$HOST")"
                    [ "$IP" ] && printf "$PFORMAT" "$NET " "$HOST " "$IP " "$MAC " "$OS "
                done
            done
        fi
    fi
}


# Main case statement:
HELPSTRING="Usage: $0 {add|mod|del|lst|rld} [-n networkname] [-h hostname] [-i ipaddress] [-m macaddress] [-o operatingsystem] [-nn newnetwork] [-hh newhostname]"
case "$1" in
    add)
    while [ $# -gt 0 ]; do
        case "$2" in
            -n)
            NET="$3"
            NET_REVERSED="$(revNet "$NET")"
            shift;;
            -h)
            HOST="$3"
            shift;;
            -i)
            IP="$3"
            IP_REVERSED="$(revIp "$IP")"
            shift;;
            -m)
            MAC="$3"
            shift;;
            -o)
            OS="$3"
            shift;;
            -*)
            echo >&2 "$HELPSTRING"
            exit 1
            ;;
            *)
            break
            ;;
        esac
    shift
    done
    addHost "$NET" "$HOST" "$IP" "$MAC" "$OS"
    ;; 
    mod)
    while [ $# -gt 0 ]; do
        case "$2" in
            -n)
            NET="$3"
            NET_REVERSED="$(revNet "$NET")"
            shift;;
            -h)
            HOST="$3"
            shift;;
            -i)
            IP="$3"
            IP_REVERSED="$(revIp "$IP")"
            shift;;
            -m)
            MAC="$3"
            shift;;
            -o)
            OS="$3"
            shift;;
            -nn)
            NET_NEW="$3"
            NET_NEW_REVERSED="$(revNet "$NET_NEW")"
            shift;;
            -hh)
            HOST_NEW="$3"
            shift;;
            -*)
            echo >&2 "$HELPSTRING"
            exit 1
            ;;
            *)
            break
            ;;
        esac
    shift
    done
    modHost "$NET" "$HOST" "$IP" "$MAC" "$OS" "$NET_NEW" "$HOST_NEW"
    ;;
    del)
    while [ $# -gt 0 ]; do
        case "$2" in
            -n)
            NET="$3"
            NET_REVERSED="$(revNet "$NET")"
            shift;;
            -h)
            HOST="$3"
            shift;;
            -i)
            IP="$3"
            IP_REVERSED="$(revIp "$IP")"
            shift;;
            -m)
            MAC="$3"
            shift;;
            -o)
            OS="$3"
            shift;;
            -*)
            echo >&2 "$HELPSTRING"
            exit 1
            ;;
            *)
            break
            ;;
        esac
    shift
    done
    delHost "$NET" "$HOST"
    ;;
    lst)
    while [ $# -gt 0 ]; do
        case "$2" in
            -n)
            NET="$3"
            NET_REVERSED="$(revNet "$NET")"
            shift;;
            -h)
            HOST="$3"
            shift;;
            -i)
            IP="$3"
            IP_REVERSED="$(revIp "$IP")"
            shift;;
            -m)
            MAC="$3"
            shift;;
            -o)
            OS="$3"
            OS_LIST="yes"
            shift;;
            -d)
            DOMAIN="$3"
            DOMAIN_LIST="yes"
            shift;;
            -*)
            echo >&2 "$HELPSTRING"
            exit 1
            ;;
            *)
            break
            ;;
        esac
    shift
    done
    lstHost "$NET" "$HOST" "$IP" "$MAC" "$OS"
    ;;
    rld)
    service named restart
    service dhcpd restart
    ;;
    dbg)
    set | sort
    ;;
    *)
    echo >&2 "$HELPSTRING"
    exit 1
esac

