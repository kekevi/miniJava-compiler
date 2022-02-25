#!/usr/bin/env bash
for file in ./../official_testcases/pa1_tests/* ; do
  cd "/Users/siraire/OneDrive - University of North Carolina at Chapel Hill/Junior Year/COMP 520/Compiler Checkpoints/compiler-checkpoint2" ; /usr/bin/env /opt/homebrew/Cellar/openjdk/17.0.1_1/libexec/openjdk.jdk/Contents/Home/bin/java -XX:+ShowCodeDetailsInExceptionMessages -cp "/Users/siraire/OneDrive - University of North Carolina at Chapel Hill/Junior Year/COMP 520/Compiler Checkpoints/compiler-checkpoint2/bin" miniJava.Compiler $file > /dev/null
  echo $file $?
done

