#!/bin/bash

SCRIPTS_PATH=$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
ROOT_DIR=$(dirname ${SCRIPTS_PATH})
OUT_DIR="out"

pushd ${ROOT_DIR}/${OUT_DIR}

java ru.ifmo.rain.rasho.bank.client.Client $*

popd