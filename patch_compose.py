import re
import sys

def patch_file(filepath):
    print(f"Patching {filepath}...")
    with open(filepath, 'r') as f:
        content = f.read()

    # 1. Replace redis-<service> depends_on to redis-cluster-init
    services = [
        "apigateway", "auth", "merchant", "card", "saldo", "role", 
        "topup", "transaction", "transfer", "user", "withdraw"
    ]
    
    for s in services:
        pattern = rf"redis-{s}:\s*\n\s*condition:\s*service_started"
        replacement = "redis-cluster-init:\n        condition: service_completed_successfully"
        content, count = re.subn(pattern, replacement, content)
        print(f"  Replaced dependency redis-{s}: {count} times")

    # 2. Add REDIS_CLUSTER_ENABLED and REDIS_CLUSTER_ENDPOINTS in environment blocks
    # We find the environment block for each service that contains REDIS_PASSWORD
    pattern_env = r"(REDIS_PASSWORD=\$\{REDIS_PASSWORD_[A-Z]+\})"
    replacement_env = r"\1\n      - REDIS_CLUSTER_ENABLED=${REDIS_CLUSTER_ENABLED}\n      - REDIS_CLUSTER_ENDPOINTS=${REDIS_CLUSTER_ENDPOINTS}"
    content, count = re.subn(pattern_env, replacement_env, content)
    print(f"  Added cluster env vars to environment blocks: {count} times")

    # 3. Replace the legacy redis volumes block
    legacy_volumes = """  redis_apigateway_data:
  redis_auth_data:
  redis_user_data:
  redis_card_data:
  redis_merchant_data:
  redis_role_data:
  redis_saldo_data:
  redis_transaction_data:
  redis_topup_data:
  redis_transfer_data:
  redis_withdraw_data:"""

    new_volumes = """  redis_node_1_data:
  redis_node_2_data:
  redis_node_3_data:
  redis_node_4_data:
  redis_node_5_data:
  redis_node_6_data:"""

    if legacy_volumes in content:
        content = content.replace(legacy_volumes, new_volumes)
        print("  Successfully replaced legacy volumes block")
    else:
        # Try alternate formatting (without leading spaces or different newlines)
        legacy_v_stripped = [line.strip() for line in legacy_volumes.splitlines()]
        found_stripped = True
        for v in legacy_v_stripped:
            if v not in content:
                found_stripped = False
                break
        if found_stripped:
            # Let's replace the whole lines containing them
            for v, nv in zip(legacy_v_stripped, [line.strip() for line in new_volumes.splitlines()]):
                content = re.sub(rf"^\s*{v}\s*$", f"  {nv}", content, flags=re.MULTILINE)
            # Remove any left-over legacy volumes not matched in zip
            # We had 11 legacy volumes, and 6 new volumes, so zip will only replace the first 6.
            # We must explicitly delete the remaining 5 legacy volumes:
            remaining_legacy = legacy_v_stripped[6:]
            for v in remaining_legacy:
                content = re.sub(rf"^\s*{v}\s*\n?", "", content, flags=re.MULTILINE)
            print("  Replaced legacy volumes block using line-by-line regex mapping")
        else:
            print("  WARNING: Could not find legay volumes block exactly!")

    with open(filepath, 'w') as f:
        f.write(content)
    print("Patching complete!")

if __name__ == "__main__":
    if len(sys.argv) > 1:
        patch_file(sys.argv[1])
    else:
        print("Please provide a filepath.")
