#!/bin/bash

SCRIPTS_PATH=$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
ROOT_DIR=$(dirname ${SCRIPTS_PATH})
OUT_DIR="out"

pushd ${ROOT_DIR}/${OUT_DIR}

java -cp .:${ROOT_DIR}/lib/* ru.ifmo.rain.rasho.bank.tests.TestRunner

echo ${?}
exit ${?}

popd