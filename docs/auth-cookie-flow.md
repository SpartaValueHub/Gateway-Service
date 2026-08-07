# Gateway Auth Cookie Flow

## JWT 추출

1. `Authorization: Bearer {token}` (하위 호환)
2. 없으면 Cookie `vh_access_token`

## Blacklist

Redis key: `auth:blacklist:access:{jti}` — auth-service logout과 공유.

## Internal Headers

| Header | Source |
|--------|--------|
| `X-Member-Uuid` | JWT `sub` |
| `X-Role` | JWT `role` claim (default USER) |

## Env

| Variable | Default |
|----------|---------|
| `AUTH_COOKIE_ACCESS_NAME` | `vh_access_token` |
| `REDIS_HOST` | `localhost` |
| `SECURITY_JWT_ENABLED` | `true` (기본). prod 에서 `false` 금지 |

## Manual E2E

See [troubleshooting.md](./troubleshooting.md#httponly-cookie-jwt).
