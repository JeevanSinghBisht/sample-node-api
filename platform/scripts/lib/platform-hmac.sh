#!/usr/bin/env bash
# Shared HMAC signing for registry / bridge internal APIs (matches PlatformHmac).

platform_hmac_signature() {
  local timestamp="$1"
  local body="$2"
  local secret="${PLATFORM_INTERNAL_HMAC_SECRET:?Set PLATFORM_INTERNAL_HMAC_SECRET}"
  python3 -c "
import hmac, hashlib, os, sys
secret = os.environ['PLATFORM_INTERNAL_HMAC_SECRET'].encode()
payload = (sys.argv[1] + '.' + sys.argv[2]).encode()
print('hmac-sha256=' + hmac.new(secret, payload, hashlib.sha256).hexdigest())
" "$timestamp" "$body"
}

platform_hmac_post() {
  local url="$1"
  local body="$2"
  local ts
  ts="$(date +%s)"
  local sig
  sig="$(platform_hmac_signature "$ts" "$body")"
  curl -sS -X POST "$url" \
    -H "Content-Type: application/json" \
    -H "X-Platform-Timestamp: $ts" \
    -H "X-Platform-Signature: $sig" \
    -d "$body"
}
