#!/usr/bin/env bash

# run this script from /.../"Compiler Checkpoints", ie. the root of the git repo
# best way to run it:
# ./compiler-checkpoint2/tests/runpa3.sh > ./compiler-checkpoint2/tests/tests2017pa3/zlog.txt 2>&1

maindir=./compiler-checkpoint2/tests/tests2017pa3
summary=$maindir/summary.txt

echo "" > $summary

for test in echo $maindir/fail*.java; do
  /usr/bin/env /opt/homebrew/Cellar/openjdk/17.0.1_1/libexec/openjdk.jdk/Contents/Home/bin/java -XX:+ShowCodeDetailsInExceptionMessages -cp "/Users/siraire/OneDrive - University of North Carolina at Chapel Hill/Junior Year/COMP 520/Compiler Checkpoints/compiler-checkpoint2/bin" miniJava.Compiler $test > $test.result
  out=`echo $test $?`
  echo $out
  echo $out >> $summary
done

for test in echo $maindir/pass*.java; do
  /usr/bin/env /opt/homebrew/Cellar/openjdk/17.0.1_1/libexec/openjdk.jdk/Contents/Home/bin/java -XX:+ShowCodeDetailsInExceptionMessages -cp "/Users/siraire/OneDrive - University of North Carolina at Chapel Hill/Junior Year/COMP 520/Compiler Checkpoints/compiler-checkpoint2/bin" miniJava.Compiler $test > $test.result
  out=`echo $test $?`
  echo $out
  echo $out >> $summary
done