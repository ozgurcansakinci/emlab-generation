#!/bin/bash
if [ -f emlabConfig.cfg ];then
        . emlabConfig.cfg
else
    echo "Define emlabConfig.cfg, by changing the template. Exiting sc\                                                                    
ript."
    exit
fi

sh $emlabHome/shellscripts/makeRamdisk.sh

#start model
cd $emlabModelFolder
mvn exec:java -Dexec.args="-Djava.library.path=/Users/apple/Applications/IBM/ILOG/CPLEX_Studio1263/cplex/bin/x86-64_osx"
mvn exec:java $1

#mvn exec:exec -Dexec.executable="java"
