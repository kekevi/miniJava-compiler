#!/usr/bin/env bash

# run this script from /.../"Compiler Checkpoints", ie. the root of the git repo

maindir=./compiler-checkpoint2/tests/oldtests_pass
for test in echo ./compiler-checkpoint2/tests/oldtests_pass/*.java; do
  /usr/bin/env /opt/homebrew/Cellar/openjdk/17.0.1_1/libexec/openjdk.jdk/Contents/Home/bin/java -XX:+ShowCodeDetailsInExceptionMessages -cp "/Users/siraire/OneDrive - University of North Carolina at Chapel Hill/Junior Year/COMP 520/Compiler Checkpoints/compiler-checkpoint2/bin" miniJava.Compiler $test > $maindir/temp.txt
  diff <(cat $maindir/temp.txt) <(cat $test.out) > $test.result
  cat $maindir/temp.txt > $test.outresult
done

# reads *.result files and says which one has problems
for result in echo ./compiler-checkpoint2/tests/oldtests_pass/*.result; do
  echo $result
  head -n 2 $result | tail -1
done