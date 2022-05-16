#!/usr/bin/env bash
if [ "$1" == "--version" ]; then
    gpg $@
else
    gpg --pinentry-mode loopback --batch --passphrase-fd 0 --no-tty $@ <<< $GPG_PASSPHRASE
fi;
