#!/bin/bash
# example of java home paths.
# /Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/ #
# "~/envlib/jdk1.8.0_251"
JAVA_HOME=${1}

DIR="${BASH_SOURCE%/*}"
if [[ ! -d "$DIR" ]]; then DIR="$PWD"; fi
. $DIR/D4Jutils.sh $JAVA_HOME

pid=${2}
bid=${3}
version=${4}

checkout $pid $bid $dir $version
#checkoutCompileTest Math 1 $dir f
