#!/bin/bash

JAVA_HOME=${1}
FIXED_PROJECTS_DIR=${2}
D4J_DIR=${3}

DIR="${BASH_SOURCE%/*}"
if [[ ! -d "$DIR" ]]; then DIR="$PWD"; fi
. $DIR/D4Jutils.sh $JAVA_HOME $FIXED_PROJECTS_DIR $D4J_DIR

cd $D4J || exit 100
# cloning d4j
git clone https://github.com/rjust/defects4j.git
cd defects4j || exit 101
# initializing d4j
./init.sh

cd ..
cd ..

# extract jira projects information.
bugsOutputFolderPath=$ROOT/input/d4j_v2/evaluation/bugs
mkdir -p $bugsOutputFolderPath
echo "/input/d4j_v2/evaluation/bugs/" >> $ROOT/.gitignore
echo bugsOutputFolderPath $bugsOutputFolderPath
echo d4j_v2_projects $d4j_v2_projects
exportProjectsBugsData "${d4j_v2_projects}" $bugsOutputFolderPath
