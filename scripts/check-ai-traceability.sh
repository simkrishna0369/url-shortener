#!/usr/bin/env bash
# Fail when application code changes without an AI session log update.
# GitHub cannot see Cursor chats; the log in git is the traceability record.
set -euo pipefail

BASE="${1:-}"
HEAD="${2:-HEAD}"

if [[ -z "${BASE}" ]]; then
  if git rev-parse --verify HEAD~1 >/dev/null 2>&1; then
    BASE="HEAD~1"
  else
    echo "No parent commit; skipping AI traceability check."
    exit 0
  fi
fi

if [[ "${BASE}" =~ ^0+$ ]]; then
  echo "No previous commit on this branch; skipping AI traceability check."
  exit 0
fi

if ! git cat-file -e "${BASE}^{commit}" 2>/dev/null; then
  echo "Base commit ${BASE} is not in this clone; skipping AI traceability check."
  exit 0
fi

changed=$(git diff --name-only "${BASE}" "${HEAD}")

non_trivial=0
log_updated=0
while IFS= read -r path; do
  [[ -z "${path}" ]] && continue
  case "${path}" in
    src/*|pom.xml)
      non_trivial=1
      ;;
    docs/ai-traceability.md)
      log_updated=1
      ;;
  esac
done <<EOF
${changed}
EOF

if [[ "${non_trivial}" -eq 0 ]]; then
  echo "No src/ or pom.xml changes; AI session log not required."
  exit 0
fi

if [[ "${log_updated}" -eq 1 ]]; then
  echo "Non-trivial change includes docs/ai-traceability.md."
  exit 0
fi

echo "Non-trivial files changed without an AI session log:"
echo "${changed}"
echo
echo "Add a short entry to docs/ai-traceability.md (intent, files, accepted/rejected, how you validated)."
echo "Docs-only, README, and workflow-only changes do not need a log."
exit 1
