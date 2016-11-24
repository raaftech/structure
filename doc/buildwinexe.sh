#!/bin/bash

BUILDDEPS="gcc perl mingw-binutils-generic mingw-filesystem-base mingw32-binutils mingw32-cpp mingw32-crt mingw32-filesystem mingw32-gcc mingw32-headers mingw64-binutils mingw64-cpp mingw64-crt mingw64-filesystem mingw64-gcc mingw64-headers libcom_err-devel popt-devel zlib-devel zlib-static glibc-devel glibc-static python-devel git gnutls-devel libacl1-dev libacl-devel libldap2-dev openldap-devel"
SAMBA_NAME=samba
SAMBA_URL=git://git.samba.org/samba.git
WINEXE_NAME=winexe
WINEXE_URL=git://git.code.sf.net/p/winexe/winexe-waf
GNUTLS_NAME=gnutls
GNUTLS_VER=2.8.5
GNUTLS_URL=ftp://ftp.gnutls.org/gcrypt/$GNUTLS_NAME/v2.8/$GNUTLS_NAME-$GNUTLS_VER.tar.bz2
LIBGCRYPT_NAME=libgcrypt
LIBGCRYPT_VER=1.4.5
LIBGCRYPT_URL=http://mirrors.dotsrc.org/gcrypt/$LIBGCRYPT_NAME/$LIBGCRYPT_NAME-$LIBGCRYPT_VER.tar.bz2
LIBGPGERROR_NAME=libgpg-error
LIBGPGERROR_VER=1.7
LIBGPGERROR_URL=https://www.gnupg.org/ftp/gcrypt/$LIBGPGERROR_NAME/$LIBGPGERROR_NAME-$LIBGPGERROR_VER.tar.bz2

## make sure we have proper deps installed (and uninstalled)
yum install -y epel-release
yum install -y $BUILDDEPS
yum groupinstall -y "Development Tools"
yum remove -y libbsd-devel

## get and prepare sources
cd /usr/src
git clone $WINEXE_URL $WINEXE_NAME
git clone $SAMBA_URL $SAMBA_NAME
cd $SAMBA_NAME
# because of some bug in samba we need to reset to a certain commit, see
# https://sourceforge.net/p/winexe/bugs/64/
git reset --hard a6bda1f2bc85779feb9680bc74821da5ccd401c5
cd /usr/src/
curl -O $GNUTLS_URL -O $LIBGCRYPT_URL -O $LIBGPGERROR_URL
tar xvf $GNUTLS_NAME-$GNUTLS_VER.tar.bz2
tar xvf $LIBGCRYPT_NAME-$LIBGCRYPT_VER.tar.bz2
tar xvf $LIBGPGERROR_NAME-$LIBGPGERROR_VER.tar.bz2
rm -rf *.tar.bz2
mv $GNUTLS_NAME-$GNUTLS_VER $GNUTLS_NAME
chmod 0700 $GNUTLS_NAME
mv $LIBGCRYPT_NAME-$LIBGCRYPT_VER $LIBGCRYPT_NAME
chmod 0700 $LIBGCRYPT_NAME
mv $LIBGPGERROR_NAME-$LIBGPGERROR_VER $LIBGPGERROR_NAME
chmod 0700 $LIBGPGERROR_NAME

## build sources
#build winexe objects
cd /usr/src/winexe/source
./waf --samba-dir=../../samba configure build
# build gnutls
cd /usr/src/$GNUTLS_NAME
./configure --disable-shared --enable-static && make
GNUTLS_STATICLIB=`readlink -f $(dirname $(find -name libgnutls.a))`
# build libgcrypt
cd /usr/src/$LIBGCRYPT_NAME
./configure --disable-shared --enable-static && make
LIBGCRYPT_STATICLIB=`readlink -f $(dirname $(find -name libgcrypt.a))`
# build libgpg-error
cd /usr/src/$LIBGPGERROR_NAME
./configure --disable-shared --enable-static && make
LIBGPGERROR_STATICLIB=`readlink -f $(dirname $(find -name libgpg-error.a))`
# build winexe static
cd /usr/src/winexe/source/build/
/usr/bin/gcc -pthread winexe.c.6.o svcinstall.c.6.o async.c.6.o winexesvc32_exe.c.6.o winexesvc64_exe.c.6.o \
             -o /usr/src/winexe/source/build/winexe-static \
             -Wl-Bstatic -L/usr/src/winexe/source/smb_static/build -lsmb_static \
             -lbsd -lz -lresolv -lrt \
             -Wl-Bdynamic -ldl -L$GNUTLS_STATICLIB -lgnutls \
             -L$LIBGCRYPT_STATICLIB -lgcrypt \
             -L$LIBGPGERROR_STATICLIB -lgpg-error
