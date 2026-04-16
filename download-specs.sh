#!/usr/bin/env bash
# Downloads Jakarta EE specification HTML documents for RAG ingestion.
# Run from project root: ./download-specs.sh
set -euo pipefail

SPECS_DIR="src/main/resources/data/jakarta-specs"
mkdir -p "$SPECS_DIR"

declare -A SPECS=(
  ["cdi"]="https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1.html"
  ["jpa"]="https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html"
  ["concurrency"]="https://jakarta.ee/specifications/concurrency/3.1/jakarta-concurrency-spec-3.1.html"
  ["data"]="https://jakarta.ee/specifications/data/1.0/jakarta-data-1.0"
  ["jaxrs"]="https://jakarta.ee/specifications/restful-ws/4.0/jakarta-restful-ws-spec-4.0.html"
)

for name in "${!SPECS[@]}"; do
  url="${SPECS[$name]}"
  dest="$SPECS_DIR/${name}.html"
  if [ -f "$dest" ]; then
    echo "SKIP $name (already exists: $dest)"
  else
    echo "GET  $name <- $url"
    curl -fsSL "$url" -o "$dest"
    size=$(wc -c < "$dest")
    echo "  -> $dest ($size bytes)"
  fi
done

echo "Done. Files in $SPECS_DIR/:"
ls -lh "$SPECS_DIR/"
