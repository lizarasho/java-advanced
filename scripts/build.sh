#!/bin/bash

SCRIPTS_PATH=$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
ROOT_DIR=$(dirname ${SCRIPTS_PATH})
OUT_DIR="out"
BANK_DIR=${ROOT_DIR}/java-solutions/ru/ifmo/rain/rasho/bank

find "${BANK_DIR}" -name "*.java" > filenames.txt
javac -cp ${ROOT_DIR}/java-solutions:${ROOT_DIR}/lib/* -d ${ROOT_DIR}/${OUT_DIR} @filenames.txt

rm -rf filenames.txt
