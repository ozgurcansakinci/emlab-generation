#!/bin/bash
export MAVEN_OPTS="-d64 -server -Xmx2048m -Djava.library.path=/Users/apple/Applications/IBM/ILOG/CPLEX_Studio1263/cplex/bin/x86-64_osx "
diskutil erasevolume HFS+ "ramdisk" `hdiutil attach -nomount ram://1165430`
ln -s /Volumes/ramdisk /tmp/ramdisk
