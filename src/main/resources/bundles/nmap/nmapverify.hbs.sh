sh -c 'nmap -sn -n -oG - "$1" | grep -q "Status: Up"' nmap {{sh ipv4address.addr}}
