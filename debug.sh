#!/usr/bin/env bash

fileEnd=java
file=`echo $1 | rev | cut -f 2- -d '.' | rev`
rootpath="/Users/siraire/OneDrive - University of North Carolina at Chapel Hill/Junior Year/COMP 520/miniJavaCompiler"
cd $rootpath
/usr/bin/env /opt/homebrew/Cellar/openjdk/17.0.1_1/libexec/openjdk.jdk/Contents/Home/bin/java -XX:+ShowCodeDetailsInExceptionMessages -cp "$rootpath/bin" miniJava.mJAM.Interpreter $file.mJAM $file.asm


