Structure
=========
How to run
----------
    git clone ssh://git@git.raaftech.com:8022/str/structure.git
    cd structure
    mvn package
    cd web/html/debug/
    bower install
    cd -
    java -jar target/structure-0.6.1-fat.jar --debug

SSH
---
The SSH plugin expects a RSA keypair in the plugin directory (resources/plugins/raaftech_sshexec).
