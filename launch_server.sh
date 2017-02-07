#!/bin/sh

export CLASSPATH=".:dist/*" 

java -Dwzpath=wz/ \
-Xmx1000m net.server.Server