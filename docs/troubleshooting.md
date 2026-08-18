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
| POST | `/{service}/api/v1/auth/sign-up/resume` |
| POST | `/{service}/api/v1/auth/sign-in` |
| POST | `/{service}/api/v1/auth/refresh` |
| GET | `/{service}/api/v1/auth/check/login-id` |
| GET | `/{service}/api/v1/auth/check/email` |
| POST | `/{service}/api/v1/identity-verifications/confirm` |
| GET | `/{service}/api/v1/identity-verifications/{requestToken}` |

**logout** 등은 public 아님 → `vh_access_token` HttpOnly Cookie 또는 Bearer 필요.

---

## HttpOnly Cookie JWT

### 동작

- Access token: `Authorization: Bearer` **또는** Cookie `vh_access_token` (`auth.cookie.access-name`)
- `CookieBearerTokenAuthenticationConverter` — header 우선, cookie fallback
- 로그아웃 blacklist: Redis `auth:blacklist:access:{jti}` (auth-service와 동일 prefix)
- 성공 시 downstream 헤더: `X-Member-Uuid` (JWT sub), `X-Role` (claim, 기본 USER)

### 로컬 E2E

```bash
# 로그인 (Set-Cookie 저장)
curl.exe -c cookies.txt -X POST "http://localhost:8000/auth-service/api/v1/auth/sign-in" ^
  -H "Content-Type: application/json" ^
  -d "{\"logInId\":\"user01\",\"password\":\"Password1!\"}"

# Cookie로 logout
curl.exe -b cookies.txt -X POST "http://localhost:8000/auth-service/api/v1/auth/logout" -w "\n%{http_code}\n"
```

### CSRF / CORS

- `allowCredentials(true)` — FE origin 명시 필요 (`GatewayCorsConfig`)
- cross-origin cookie(3000→8000): prod `SameSite=None; Secure` + `AUTH_COOKIE_DOMAIN` 검토

상세: [docs/auth-cookie-flow.md](./auth-cookie-flow.md)

### 검증 (public path)

```bash
curl.exe -s -o NUL -w "%{http_code}" -X POST "http://localhost:8000/auth-service/api/v1/auth/sign-up" ^
  -H "Content-Type: application/json" -d "{}"
```

403 → Security 미반영 또는 JWT public path 누락  
400 등 → Gateway 통과 (auth-service 응답)

### JWT 설정

`.env.example` 참고:

- `SECURITY_JWT_ENABLED=true` (기본값). prod 에서 `false` 이면 기동 실패
- `JWT_PUBLIC_KEY` 또는 `JWT_PUBLIC_KEY_LOCATION` (auth-service `jwt-public.pem`)

---


## CORS

브라우저 → Gateway 직접 호출만 CORS 적용 (`GatewayCorsConfig`).  
Next.js Server Action → Gateway는 **서버 간** 호출이라 CORS 무관.  
403은 CORS가 아니라 Security일 가능성이 큼.

---

## 전체 Auth E2E

[auth-service/docs/troubleshooting.md](../../auth-service/docs/troubleshooting.md)
