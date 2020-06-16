#!/bin/bash

SCRIPTS_PATH=$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
ROOT_DIR=$(dirname ${SCRIPTS_PATH})
OUT_DIR="out"

pushd ${ROOT_DIR}/${OUT_DIR}

java -jar ${ROOT_DIR}/lib/junit-platform-console-standalone-1.6.2.jar -cp . \
            -c ru.ifmo.rain.rasho.bank.tests.ClientTests \
            -c ru.ifmo.rain.rasho.bank.tests.BankTests

popd            