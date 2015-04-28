#!/bin/bash

java -jar ./jarjar-1.4.jar process rules-guava-15.txt ./jar-source/guava-15.0.jar ./jar-target/guava-15.0-sliced.jar
java -jar ./jarjar-1.4.jar process rules-gwt-user-2.6.1.txt ./jar-source/gwt-user-2.6.1.jar ./jar-target/gwt-user-2.6.1-sliced.jar
java -jar ./jarjar-1.4.jar process rules-gwt-dev-2.6.1.txt ./jar-source/gwt-dev-2.6.1.jar ./jar-target/gwt-dev-2.6.1-sliced.jar


# Remove some duplicate files in target jars and empty folders that causes errors in apk building

zip -d ./jar-target/gwt-dev-2.6.1-sliced.jar \
plugin.properties \
org/w3c/** \
about_files/** \ javax/** \
about.html \
META-INF/**

zip -d ./jar-target/gwt-user-2.6.1-sliced.jar \
plugin.properties \
org/w3c/css/sac/COPYING.html \
about_files/** \
org/hibernate/** \
about.html \
META-INF/**
