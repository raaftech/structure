# winexe binaries

This directory used to contain two static builds of `winexe`:
`winexe-x86_32` and `winexe-x86_64`. Structure used `winexe` to run
commands on Windows machines over SMB. #217 added the binaries in
`8ad4a9af` (2016-11-23), and `doc/notes/buildwinexe.md` (#220) tells how
to build them. The Dockerfile copied `winexe-x86_64` to
`/usr/bin/winexe` in the image.

The binaries are no longer in the repository. To get them back, use the
last commit that has them:

```bash
git show ecc80bff:doc/legacy/docker/bin/winexe-x86_64 > winexe-x86_64
git show ecc80bff:doc/legacy/docker/bin/winexe-x86_32 > winexe-x86_32
```
