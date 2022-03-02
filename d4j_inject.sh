#!/bin/bash -l

##### test:
ibir_version='1.1.20-SNAPSHOT'
java_home=${1}

. D4Jutils.sh ${java_home}
# project: i.e. Lang
proj=${2}
# bug : i.e. 50
bug=${3}
# nbre of mutants : i.e. 100
mutantsNumber=${4}
# directory of the FL file
flDirPath=${5}
# what kind of job to run: by default, when this variable is unset or empty, default ibir will be run. else you can choose: ibir, ibir_pcl, rand, rand_pcl.
results_to_output=${6}
if [ -z "$results_to_output" ]; then
  results_to_output='DEF'
fi
node_Selection_mode=${7}
mutation_operators=${8}


# outputDir
outputDir="${ROOT}/results"

logDir="${ROOT}/logs/${proj}_${bug}/${mutantsNumber}"
execDir="execTmpDir_${proj}_${bug}"
mkdir -p $logDir

tmpFileName=".temp_${proj}_${bug}/"

#################################################
cmdbase="java -Xms512m -ea -cp ${ROOT}/release/IBIR-${ibir_version}-jar-with-dependencies.jar \
 -DD4J_INFOS_DIR=${ROOT}/input/d4j_v2/evaluation/bugs \
 -DresultType=${results_to_output} \
 -DTEMP_FILES_PATH=${tmpFileName} \
 edu.lu.uni.serval.ibir.main.Main \
 ${flDirPath} \
 ${dir}f/ \
 ${D4J}/defects4j/ \
 ${outputDir}/ \
 ${mutantsNumber} \
 ${java_home} \
 ${proj}_${bug}"

if [ ! -z "$node_Selection_mode" ]; then
  cmdbase="$cmdbase $node_Selection_mode"
fi
if [ ! -z "$mutation_operators" ]; then
  cmdbase="$cmdbase $mutation_operators"
fi

echo cmd: $cmdbase

######################### checkout fixed version
checkoutCompileTest $proj $bug $dir f

######################### inject

  cd $ROOT
  mkdir -p $execDir
  echo ++++++++++++++ injecting
  echo executing ..... $cmdbase
  pushd $execDir || exit 103
  log_files_prfix="${node_Selection_mode}_${mutation_operators}_${results_to_output}"
  $cmdbase 1>"${logDir}/${log_files_prfix}_ibir-java-out.txt" 2>"${logDir}/${log_files_prfix}_ibir-java-err.txt" >"${logDir}/${log_files_prfix}_ibir-terminal-output.txt"
  popd || exit 104

######################### delete some exec data
  rm -rf "${execDir}"