#!/usr/bin/env bash

# run this script from /.../"Compiler Checkpoints", ie. the root of the git repo

maindir=./compiler-checkpoint2/tests/oldtests_fail
for test in echo ./compiler-checkpoint2/tests/oldtests_fail/*.java; do
  /usr/bin/env /opt/homebrew/Cellar/openjdk/17.0.1_1/libexec/openjdk.jdk/Contents/Home/bin/java -XX:+ShowCodeDetailsInExceptionMessages -cp "/Users/siraire/OneDrive - University of North Carolina at Chapel Hill/Junior Year/COMP 520/Compiler Checkpoints/compiler-checkpoint2/bin" miniJava.Compiler $test > /dev/null
  echo $test $?
done