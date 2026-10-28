sh -c 'nmap -Pn -n -p "$2" -oG - "$1" | grep -q "Ports: $2/open/"' nmap {{sh ipv4listener.addr}} {{sh ipv4listener.port}}
