#!/usr/bin/env bash
set -euo pipefail

BUNDLE_NAME="${1:-upload-monolith}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUNDLE_DIR="${REPO_ROOT}/${BUNDLE_NAME}"

copy_latest_jar() {
  local source_pattern="$1"
  local destination_path="$2"
  local display_name="$3"
  local source_dir
  local source_name
  local jar_path

  source_dir="$(dirname "${source_pattern}")"
  source_name="$(basename "${source_pattern}")"

  jar_path="$(find "${source_dir}" -maxdepth 1 -type f -name "${source_name}" -printf '%T@ %p\n' \
    | sort -nr \
    | awk 'NR==1{$1=""; sub(/^ /,""); print}')"

  if [[ -z "${jar_path}" ]]; then
    echo "ERROR: ${display_name} not found, expected pattern: ${source_pattern}" >&2
    exit 1
  fi

  mkdir -p "$(dirname "${destination_path}")"
  cp -f "${jar_path}" "${destination_path}"
  echo "Copied ${display_name} => ${destination_path}"
}

rm -rf "${BUNDLE_DIR}"
mkdir -p "${BUNDLE_DIR}/system" "${BUNDLE_DIR}/mysql"

# docker-compose and env
cp -f "${REPO_ROOT}/docker-deploy/monolith/docker-compose.yml" "${BUNDLE_DIR}/docker-compose.yml"
cp -f "${REPO_ROOT}/.env" "${BUNDLE_DIR}/.env"

# Dockerfile + jar
cp -f "${REPO_ROOT}/docker-deploy/monolith/system/Dockerfile" "${BUNDLE_DIR}/system/Dockerfile"
copy_latest_jar \
  "${REPO_ROOT}/jeecg-module-system/jeecg-system-start/target/jeecg-system-start-*.jar" \
  "${BUNDLE_DIR}/system/jeecg-system-start.jar" \
  "Monolith system JAR"

# MySQL init/config
cp -a "${REPO_ROOT}/docker-deploy/monolith/mysql/." "${BUNDLE_DIR}/mysql/"

# Optional exported image tar
if [[ -f "${REPO_ROOT}/jeecg-monolith.tar" ]]; then
  cp -f "${REPO_ROOT}/jeecg-monolith.tar" "${BUNDLE_DIR}/jeecg-monolith.tar"
  echo "Attached image tar: ${REPO_ROOT}/jeecg-monolith.tar"
else
  echo "jeecg-monolith.tar not found, skip image tar attachment."
fi

echo "Bundle generated at: ${BUNDLE_DIR}"

