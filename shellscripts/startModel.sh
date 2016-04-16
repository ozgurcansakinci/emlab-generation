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
mvn exec:java $1
#mvn exec:java -Dexec.args="-Djava.library.path=/opt/ibm/ILOG/CPLEX_Studio1262/cplex/bin/x86-64_linux"
#mvn exec:exec -Dexec.executable="java"
