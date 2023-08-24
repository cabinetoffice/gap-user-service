#!/bin/sh

set -- ../src/main/resources/db/migration/*'__'*''

FAILED="false"

printf 'Checking for duplicate migration script versions...\n'

for name do
  printf 'name: %s\n' $name
  shift
  for dup do
    printf 'comparing %s and %s\n' $name $dup
     if [ "${name%%__*}" = "${dup%%__*}" ]; then
        if [ "$FAILED" = "false" ]; then
           printf 'Found duplication migration script version(s) - please bump your script version(s)\n'
        fi;
        printf '%s <-> %s\n' $(basename -- $name) $(basename -- $dup)
        FAILED="true"
     fi
  done
done

if [ "$FAILED" = "true" ]; then
  exit 1
else
  printf 'No duplicate migration script versions - good job!\n'
fi;
