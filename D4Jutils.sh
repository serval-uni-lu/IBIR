#!/bin/bash

DIR="${BASH_SOURCE%/*}"
if [[ ! -d "$DIR" ]]; then DIR="$PWD"; fi
ROOT=$DIR
echo ROOT FOLDER: $ROOT


d4j_v2_projects='Cli Codec Collections Compress Csv JxPath Lang Math'
D4J=${3}
if [ -z "$D4J" ]; then
  D4J="$ROOT/D4J"
fi
dir=${2}
if [ -z "$dir" ]; then
  dir="${D4J}/projects/" # Store the fixed version projects.
fi
mkdir -p $dir

export PATH="$D4J/defects4j/framework/bin/:$PATH"
# example of java home paths.
# /Library/Java/JavaVirtualMachines/jdk1.8.0_261.jdk/Contents/Home/
# "~/envlib/jdk1.8.0_251"
export JAVA_HOME=${1}
export PATH=$JAVA_HOME/bin:$PATH
echo $(java -version)
echo $(javac -version)

#get rid of the perl language warning
export LANGUAGE=en_US.UTF-8
export LC_ALL=en_US.UTF-8
export LANG=en_US.UTF-8
export LC_CTYPE=en_US.UTF-8

checkoutCompileTest(){
  proj=$1
  bug=$2
  dir=$3
  version=$4
  projDir="${dir}${version}/${proj}_${bug}"
  mkdir -p ${dir}${version}
  if [ -d "${projDir}" ]
  then
    rm -rf ${projDir}
  fi
  cmd="defects4j checkout -p ${proj} -v ${bug}${version} -w ${projDir}"
  echo executing ....... $cmd
  $cmd || exit 100
  cd $projDir || (stdout "failed to cd ${projDir}" && return 100)
  defects4j compile
  defects4j test
	cd ../../../../
}

checkout(){
  proj=$1
  bug=$2
  dir=$3
  version=$4
  projDir="${dir}${version}/${proj}_${bug}"
  mkdir -p ${dir}${version}
  if [ -d "${projDir}" ]
    then
      cmd="git --work-tree=${projDir} --git-dir=${projDir}/.git checkout -- ."
    else
      cmd="defects4j checkout -p ${proj} -v ${bug}${version} -w ${projDir}"
    fi

  echo executing ....... $cmd
  $cmd
  cd $projDir || (stdout "failed to cd ${projDir}" && return 100)
	cd ../../../
}

checkoutProjectBugs(){
  proj=$1
  bugStart=$2
  bugEnd=$3
  dir=$4
  version=$5
  for bug in $(seq $bugStart $bugEnd)
    do
      checkoutCompileTest $proj $bug $dir $version
    done
}

exportProjectBugsData(){
  proj=$1
  outputFolderPath=$2
  option=$3 # []: active bugs only | [-A]: all bugs | [-D]: deprecated bugs only
  field_list="bug.id,project.id,project.vcs,project.repository,project.bugs.csv,revision.id.buggy,revision.id.fixed,report.id,report.url,classes.modified,tests.trigger"
  output_file="${outputFolderPath}/${proj}_bugs.csv"
  echo $output_file
  echo $proj
  echo ${outputFolderPath}
  #touch $output_file
  cmd="query -p $proj -q $field_list -o $output_file $option"
  echo executing d4j cmd ....... $cmd
  defects4j $cmd
  sed -i old '1s/^/'$field_list'\n/' $output_file
  rm -f $output_file'old'
}

exportProjectsBugsData(){
  echo exportProjectsBugsData $1 $2
  for project in $1; do
      exportProjectBugsData $project $2
  done
}
