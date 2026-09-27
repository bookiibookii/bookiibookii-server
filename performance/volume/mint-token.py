"""로컬 볼륨 테스트 서버 전용 access token 발급.

performance/volume/local/application-volume.yml 의 jwt.token.secret-key(로컬 전용 더미 값)로 서명한다.
사용: python performance/volume/mint-token.py <userId> [role]
"""
import base64
import hashlib
import hmac
import json
import sys
import time

SECRET = b"volume-test-only-secret-key-not-used-anywhere-else-0123456789"


def b64(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def mint(user_id: int, role: str = "USER", ttl_seconds: int = 7 * 24 * 3600) -> str:
    now = int(time.time())
    header = {"alg": "HS256", "typ": "JWT"}
    payload = {"sub": str(user_id), "type": "access", "role": role, "iat": now, "exp": now + ttl_seconds}
    signing_input = f"{b64(json.dumps(header, separators=(',', ':')).encode())}.{b64(json.dumps(payload, separators=(',', ':')).encode())}"
    signature = hmac.new(SECRET, signing_input.encode(), hashlib.sha256).digest()
    return f"{signing_input}.{b64(signature)}"


if __name__ == "__main__":
    uid = int(sys.argv[1])
    print(mint(uid, sys.argv[2] if len(sys.argv) > 2 else "USER"))
