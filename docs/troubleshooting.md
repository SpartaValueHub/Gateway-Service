# Gateway 트러블슈팅

## JWT on 시 POST 403 Forbidden

### 증상

- `POST /auth-service/api/v1/auth/sign-up` → **403**, body 없음
- `POST /auth-service/api/v1/identity-verifications/confirm` → **403**
- `GET /auth-service/api/v1/auth/check/login-id` → **200** (GET만 통과하는 경우)

클라이언트(브라우저·Next.js Server Action)에 `API 오류 (403 Forbidden)`.

### 원인

`security.jwt.enabled=true` (`SECURITY_JWT_ENABLED=true`) 일 때 `JwtSecurityConfig`가 활성화됩니다.  
public API가 `permitAll`에 매칭되지 않으면 Bearer 없는 요청이 **authenticated()** 구간에서 거부됩니다.

Gateway는 downstream으로 프록시하기 **전** 전체 경로(`/auth-service/api/v1/...`)로 Security를 판단합니다.

### 해결

1. **`AuthPublicPathMatcher`** (regex) 로 auth·identity public URI 판별
2. **`JwtSecurityConfig`** — public / JWT **SecurityWebFilterChain 분리** (`@Order(0)` public chain에 `oauth2ResourceServer` 없음)
3. **Gateway 프로세스 재기동** (`./gradlew bootRun` 등)

public API (auth-service와 동기화):

| Method | Path (Gateway 기준) |
|--------|---------------------|
| POST | `/{service}/api/v1/auth/sign-up` |
| POST | `/{service}/api/v1/auth/sign-in` |
| POST | `/{service}/api/v1/auth/refresh` |
| GET | `/{service}/api/v1/auth/check/login-id` |
| GET | `/{service}/api/v1/auth/check/email` |
| POST | `/{service}/api/v1/identity-verifications/confirm` |
| GET | `/{service}/api/v1/identity-verifications/{requestToken}` |

**logout** 등은 public 아님 → Bearer accessToken 필요.

### 검증

```bash
curl.exe -s -o NUL -w "%{http_code}" -X POST "http://localhost:8000/auth-service/api/v1/auth/sign-up" ^
  -H "Content-Type: application/json" -d "{}"
```

403 → Security 미반영 또는 JWT public path 누락  
400 등 → Gateway 통과 (auth-service 응답)

### 로컬 임시

```env
SECURITY_JWT_ENABLED=false
```

JWT off 시 `SecurityConfig`가 public 외 **전 구간 permitAll** (초기 FE 연동용).

### JWT on 설정

`.env.example` 참고:

- `SECURITY_JWT_ENABLED=true`
- `JWT_PUBLIC_KEY` 또는 `JWT_PUBLIC_KEY_LOCATION` (auth-service `jwt-public.pem`)

---

## CORS

브라우저 → Gateway 직접 호출만 CORS 적용 (`GatewayCorsConfig`).  
Next.js Server Action → Gateway는 **서버 간** 호출이라 CORS 무관.  
403은 CORS가 아니라 Security일 가능성이 큼.

---

## 전체 Auth E2E

[auth-service/docs/troubleshooting.md](../../auth-service/docs/troubleshooting.md)
