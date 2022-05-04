#!/usr/bin/env bash

testdir=./tests/official_testcases/pa5_tests

for file in echo $testdir/*.java; do
  echo $file
  /usr/bin/env /opt/homebrew/Cellar/openjdk/17.0.1_1/libexec/openjdk.jdk/Contents/Home/bin/java -XX:+ShowCodeDetailsInExceptionMessages -cp "/Users/siraire/OneDrive - University of North Carolina at Chapel Hill/Junior Year/COMP 520/miniJavaCompiler/bin" miniJava.Compiler $file
done

for file in echo $testdir/*.mJAM; do
  echo $file
  /usr/bin/env /opt/homebrew/Cellar/openjdk/17.0.1_1/libexec/openjdk.jdk/Contents/Home/bin/java -XX:+ShowCodeDetailsInExceptionMessages -cp "/Users/siraire/OneDrive - University of North Carolina at Chapel Hill/Junior Year/COMP 520/miniJavaCompiler/bin" miniJava.mJAM.Interpreter $file
done