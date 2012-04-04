#!/bin/sh

# Turn off SELinux
sed -i 's/SELINUX=.*/SELINUX=disabled/g' /etc/sysconfig/selinux
sed -i 's/SELINUXTYPE=.*/SELINUXTYPE=targeted/g' /etc/sysconfig/selinux
setenforce 0

# Turn off firewall
chkconfig --level 0123456 iptables off
chkconfig --level 0123456 ip6tables off
service iptables stop
iptables -F ; iptables -L