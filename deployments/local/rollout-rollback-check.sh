#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT="$(mktemp)"
trap 'rm -f "$OUT"' EXIT
kubectl kustomize "$ROOT_DIR/deployments/kubernetes/base" > "$OUT"

grep -q 'namespace: payment-gateway' "$OUT"

# Check each rendered domain Deployment, rather than merely finding probe strings
# somewhere in the aggregate YAML. This catches a service accidentally losing a
# probe or being wired to the wrong gRPC port.
python3 - "$OUT" <<'PY'
from pathlib import Path
import sys

text = Path(sys.argv[1]).read_text()
docs = text.split("\n---\n")
expected = {
    "auth": 50051,
    "role": 50052,
    "card": 50053,
    "merchant": 50054,
    "user": 50055,
    "saldo": 50056,
    "topup": 50057,
    "transaction": 50058,
    "transfer": 50059,
    "withdraw": 50060,
}
for name, port in expected.items():
    matches = [doc for doc in docs if "kind: Deployment" in doc and f"  name: {name}\n" in doc]
    if len(matches) != 1:
        raise SystemExit(f"expected exactly one Deployment for {name}, found {len(matches)}")
    doc = matches[0]
    for probe in ("livenessProbe:", "readinessProbe:", "startupProbe:"):
        if probe not in doc:
            raise SystemExit(f"{name} missing {probe}")
    if f"port: {port}" not in doc:
        raise SystemExit(f"{name} missing gRPC port {port}")

jobs = [doc for doc in docs if "kind: Job" in doc]
if not any("  name: migrate\n" in doc and "restartPolicy: OnFailure" in doc for doc in jobs):
    raise SystemExit("migration Job or restartPolicy OnFailure is missing")
PY

echo "Kubernetes manifest contract passed: namespace, per-service gRPC ports/probes, and migration Job are valid."
echo "Live rollout/rollback remains opt-in: kubectl rollout status/undo require a target cluster."
