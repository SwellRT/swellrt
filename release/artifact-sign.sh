#!/bin/zsh
#Assumes it is being run in the base directory.

PRE="dist/apache-wave-"
for f in $PRE*; do
gpg --armor --output $f.asc --detach-sig $f
gpg --print-md SHA512 $f > $f.sha
done