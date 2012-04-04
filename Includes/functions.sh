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

# checkMacFormat() <mac>: Check mac string format.
checkMacFormat(){
    echo "$1" | sed -n '/^\([0-9A-Fa-f][0-9A-Fa-f]:\)\{5\}[0-9A-Fa-f][0-9A-Fa-f]$/p'
}

# checkIpFormat() <ip>: Check ip string format.
checkIpFormat(){
    IFS=.
    set -- $1
    if [ $# -eq 4 ]; then
        for SEQUENCE do
            case $SEQUENCE in
                ""|*[!0-9]*)
                return 1;
                break
                ;;
                *)
                [ $SEQUENCE -gt 255 ] && return 1
                ;;
            esac
        done
    else
        return 1
    fi
    unset IFS
    echo "$1.$2.$3.$4"
}

# checkHostFormat() <host>: Check host string format.
checkHostFormat(){
    if [ `expr length $1` -lt 64 ]; then
        echo "$1" | sed "s|[\~\!\@\#\$\%\^\&\*\(\)\_\+\=\,\.\/\?\;\`\'\:\"\{\}\[\]\\]*||g" \
                  | sed "s|\-$||g" \
                  | sed -n '/^[a-z][a-z0-9]/p'
    else
        echo "Hostname too long."
    fi;
}

# checkNetFormat() <net>: Check net string format.
checkNetFormat(){
    echo "$1" | sed -n '/^[a-z].[a-z0-9]*\..[a-z]/p'
}

# checkOsFormat() <os>: Check operating system string format.
checkOsFormat(){
    echo "$1" | sed -n '/^[a-z][a-z0-9]/p'
}

# mask2Cidr(): Calculates the cidr notation from a complete netmask.
mask2Cidr() {
    nbits=0
    IFS=.
    for dec in $1 ; do
        case $dec in
            255) nbits=`expr $nbits + 8`;;
            254) nbits=`expr $nbits + 7`;;
            252) nbits=`expr $nbits + 6`;;
            248) nbits=`expr $nbits + 5`;;
            240) nbits=`expr $nbits + 4`;;
            224) nbits=`expr $nbits + 3`;;
            192) nbits=`expr $nbits + 2`;;
            128) nbits=`expr $nbits + 1`;;
            0);;
            *) echo "Error: $dec is not recognised"; exit 1
        esac
    done
    echo "$nbits"
}

# cidr2Mask(): Calculates the netmask notation from a cidr number.
cidr2Mask() {
    mask=0.0.0.0
    case $1 in
            32) mask=255.255.255.255;;
            31) mask=255.255.255.254;;
            30) mask=255.255.255.252;;
            29) mask=255.255.255.248;;
            28) mask=255.255.255.240;;
            27) mask=255.255.255.224;;
            26) mask=255.255.255.192;;
            25) mask=255.255.255.128;;
            24) mask=255.255.255.0;;
            23) mask=255.255.254.0;;
            22) mask=255.255.252.0;;
            21) mask=255.255.248.0;;
            20) mask=255.255.240.0;;
            19) mask=255.255.224.0;;
            18) mask=255.255.192.0;;
            17) mask=255.255.128.0;;
            16) mask=255.255.0.0;;
            15) mask=255.254.0.0;;
            14) mask=255.252.0.0;;
            13) mask=255.248.0.0;;
            12) mask=255.240.0.0;;
            11) mask=255.224.0.0;;
            10) mask=255.192.0.0;;
            9) mask=255.128.0.0;;
            8) mask=255.0.0.0;;
            7) mask=254.0.0.0;;
            6) mask=252.0.0.0;;
            5) mask=248.0.0.0;;
            4) mask=240.0.0.0;;
            3) mask=224.0.0.0;;
            2) mask=192.0.0.0;;
            1) mask=128.0.0.0;;
            0);;
            *) echo "Error: $1 is not recognised"; exit 1
    esac
    echo "$mask"
}

# ip2Int(): Convert a dotted-quad ip address to a decimal number.
ip2Int(){
    oct1=`echo $1|awk -F\. '{print $1}'`;
    oct2=`echo $1|awk -F\. '{print $2}'`;
    oct3=`echo $1|awk -F\. '{print $3}'`;
    oct4=`echo $1|awk -F\. '{print $4}'`;

    out1=$(($oct1 << 24));
    out2=$(($oct2 << 16));
    out3=$(($oct3 << 8));
    out4=$oct4;

    echo `expr $out1 + $out2 + $out3 + $out4`;
}

# int2Ip(): Convert a decimal number to an ip address.
int2Ip(){
    out1=$(($1 >> 24));
    out2=$(($(($1 >> 16)) & 255))
    out3=$(($(($1 >> 8)) & 255))
    out4=$(($1 & 255))
    echo $out1.$out2.$out3.$out4;
}

# getServerIp(): Return the server ip as configured in setup.conf
getServerIp(){
    COUNT=0
    for RANGE in $DOMAIN_RANGES; do
        COUNT=`expr $COUNT "+" 1`
    done

    if [ $COUNT -eq 1 ]; then
        echo "${DOMAIN_RANGES}" | cut -d ' ' -f 1 | cut -d '/' -f2 | sed -e "s|.0$|.${SERVER_SUFFIX}|g"
    elif [ $COUNT -gt 1 ]; then
        echo "${DOMAIN_RANGES}" | cut -d ' ' -f 2 | cut -d '/' -f2 | sed -e "s|.0$|.${SERVER_SUFFIX}|g"
    else
        return 1
    fi
}

