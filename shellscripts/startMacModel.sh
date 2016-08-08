#!/bin/bash
if [ -f emlabConfig.cfg ];then
        . emlabConfig.cfg
else
    echo "Define emlabConfig.cfg, by changing the template. Exiting sc\                                                                    
ript."
    exit
fi
sh $emlabHome/shellscripts/makeRamdiskMac.sh

#start model
cd $emlabModelFolder
#mvn exec:classpathScope=${classPathScope}
mvn exec:java -Dexec.args="-Djava.library.path=/Users/apple/Applications/IBM/ILOG/CPLEX_Studio1263/cplex/bin/x86-64_osx"
#mvn -e exec:java $1
