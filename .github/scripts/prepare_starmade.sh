#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${STARMADE_DEV_BASE_URL:-http://files-origin.star-made.org/build/dev/}"
DIRECT_URL="${STARMADE_BUILD_URL:-}"
DOWNLOAD_PATTERN="${STARMADE_DOWNLOAD_PATTERN:-^starmade-build_[0-9]{8}_[0-9]{6}\.zip$}"
WORK_ROOT="${WORK_ROOT:-$PWD/.ci/starmade}"
STAGE_ROOT="$WORK_ROOT/stage"
ARCHIVE_PATH="$WORK_ROOT/starmade_build.zip"

mkdir -p "$WORK_ROOT" "$STAGE_ROOT"

fallback_to_http_if_starmade_host() {
  local url="$1"
  if [[ "$url" =~ ^https://files-origin\.star-made\.org/ ]]; then
    printf '%s\n' "${url/https:\/\//http://}"
    return
  fi
  printf '%s\n' "$url"
}

curl_fetch() {
  local url="$1"
  local fallback_url
  fallback_url="$(fallback_to_http_if_starmade_host "$url")"

  if [[ "$fallback_url" != "$url" ]]; then
    curl -fsSL "$url" || curl -fsSL "$fallback_url"
    return
  fi

  curl -fsSL "$url"
}

curl_download() {
  local url="$1"
  local output_path="$2"
  local fallback_url
  fallback_url="$(fallback_to_http_if_starmade_host "$url")"

  if [[ "$fallback_url" != "$url" ]]; then
    curl -fL "$url" -o "$output_path" || curl -fL "$fallback_url" -o "$output_path"
    return
  fi

  curl -fL "$url" -o "$output_path"
}

join_url() {
  local base="$1"
  local tail="$2"
  if [[ "$tail" =~ ^https?:// ]]; then
    printf '%s\n' "$tail"
    return
  fi
  if [[ "$base" != */ ]]; then
    base="$base/"
  fi
  printf '%s%s\n' "$base" "$tail"
}

resolve_download_url() {
  if [[ -n "$DIRECT_URL" ]]; then
    printf '%s\n' "$DIRECT_URL"
    return
  fi

  # Strip anchors from DOWNLOAD_PATTERN so grep -oE can extract just the filename.
  local filename_pat
  filename_pat="$(printf '%s' "$DOWNLOAD_PATTERN" | sed 's/^\^//; s/\$$//')"

  # Scan the raw HTML for matching filenames — works regardless of href format,
  # query parameters, or whether the server returns relative or absolute URLs.
  local latest_file
  latest_file="$(curl_fetch "$BASE_URL" \
    | grep -oE "$filename_pat" \
    | sort -uV \
    | tail -n 1 || true)"

  if [[ -n "$latest_file" ]]; then
    join_url "$BASE_URL" "$latest_file"
    return
  fi

  echo "Failed to discover a StarMade build zip from $BASE_URL" >&2
  exit 1
}

DOWNLOAD_URL="$(resolve_download_url)"
echo "Resolved StarMade build download URL: $DOWNLOAD_URL"

curl_download "$DOWNLOAD_URL" "$ARCHIVE_PATH"
unzip -q -o "$ARCHIVE_PATH" -d "$STAGE_ROOT"

STARMADE_JAR_PATH="$(find "$STAGE_ROOT" -type f -name 'StarMade.jar' | head -n 1 || true)"
if [[ -z "$STARMADE_JAR_PATH" ]]; then
  echo "Could not locate StarMade.jar in downloaded build archive" >&2
  exit 1
fi

STARMADE_BASE_DIR="$(dirname "$STARMADE_JAR_PATH")"
STARMADE_LIB_DIR="$STARMADE_BASE_DIR/lib"

if [[ ! -d "$STARMADE_LIB_DIR" ]]; then
  echo "Could not locate lib directory beside StarMade.jar" >&2
  exit 1
fi

FINAL_ROOT="$WORK_ROOT/StarMade"
FINAL_LIB="$FINAL_ROOT/lib"
mkdir -p "$FINAL_LIB" "$FINAL_ROOT/mods"

cp "$STARMADE_JAR_PATH" "$FINAL_ROOT/StarMade.jar"
cp -R "$STARMADE_LIB_DIR"/. "$FINAL_LIB"/

# Ensure trailing slash for Gradle property compatibility.
FINAL_ROOT_WITH_SLASH="$FINAL_ROOT/"

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "download_url=$DOWNLOAD_URL"
    echo "starmade_root=$FINAL_ROOT_WITH_SLASH"
  } >> "$GITHUB_OUTPUT"
else
  echo "download_url=$DOWNLOAD_URL"
  echo "starmade_root=$FINAL_ROOT_WITH_SLASH"
fi
