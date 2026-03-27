#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

TARGET_DIR="jeecg-module-system/jeecg-system-start/target"
JAR_PATH="$(find "${TARGET_DIR}" -maxdepth 1 -type f -name 'jeecg-system-start-*.jar' -printf '%T@ %p\n' | sort -nr | awk 'NR==1{$1=""; sub(/^ /,""); print}')"

if [[ -z "${JAR_PATH}" ]]; then
  echo "[ERROR] No JAR found: ${TARGET_DIR}/jeecg-system-start-*.jar"
  echo "Run: mvn -U -Pprod -DskipTests clean package -pl jeecg-module-system/jeecg-system-start -am"
  exit 1
fi

echo "Found JAR: ${JAR_PATH}"
cp -f "${JAR_PATH}" "docker-deploy/monolith/system/jeecg-system-start.jar"
echo "Done. Output: docker-deploy/monolith/system/jeecg-system-start.jar"
