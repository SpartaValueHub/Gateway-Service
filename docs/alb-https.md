# ValueHub API HTTPS (ALB)

브라우저 → ALB에서 TLS 종료 → Gateway(`8000` HTTP) → 내부 마이크로서비스.  
인증서는 Spring에 넣지 않고 **ACM + ALB** 에서만 처리한다.

공개 URL: `https://api.valuehub.art`

---

## 구조

```
브라우저 / FE (https://valuehub.art, Vercel)
        │
        │  HTTPS 443
        ▼
   AWS ALB  (valuehub-api-alb)
   - ACM 인증서 api.valuehub.art
   - 80 → 301 → 443
   - idle timeout 3600초
        │
        │  HTTP 8000  (VPC 내부, ALB SG만)
        ▼
   Apps EC2  (valuehub-apps)
   Docker Compose  Gateway :8000
        │
        │  lb://  (HTTP)
        ▼
   Auth / Member / Chat / Discovery …
```

| 레이어 | 역할 |
|--------|------|
| 가비아 DNS | `api.valuehub.art` CNAME → ALB |
| ACM | `api.valuehub.art` 인증서 (서울 `ap-northeast-2`) |
| ALB | TLS 종료, `/health` 헬스체크, Gateway로 포워드 |
| Gateway | CORS, JWT, `/{service}/**` 라우팅 |
| 내부 서비스 | HTTP만. `server.ssl` 없음 |

로컬은 기존처럼 `http://localhost:8000`. HTTPS는 운영 엣지 전용이다.

---

## 트래픽 흐름

1. 클라이언트가 `https://api.valuehub.art/...` 요청
2. 가비아 CNAME이 ALB DNS로 보냄
3. ALB가 ACM 인증서로 TLS를 끊고, `X-Forwarded-Proto: https` 등을 붙임
4. Target Group이 Apps EC2 Gateway `8000`으로 HTTP 전달
5. Gateway가 VPC 사설망(ALB)에서 온 Forwarded 헤더는 유지하고, 인터넷에서 `:8000` 직접 온 헤더는 제거
6. `/{service}/**` → rewrite 후 `lb://{service}` 로 내부 호출

HTTP로 들어오면 ALB가 `https://` 로 301 리다이렉트한다.

채팅 STOMP는 Gateway 경유 시 `wss://api.valuehub.art/chat-service/ws-chat`. ALB idle timeout을 3600초로 올려 둔 상태다.

---

## AWS 리소스 (ap-northeast-2)

| 리소스 | 값 |
|--------|-----|
| VPC | `vpc-019fab8f358bdd404` |
| Apps EC2 | `valuehub-apps` / `i-0cc2c7df37a02b606` |
| ALB | `valuehub-api-alb` |
| ALB DNS | `valuehub-api-alb-1844605244.ap-northeast-2.elb.amazonaws.com` |
| 서브넷 | `ap-northeast-2a` + `ap-northeast-2c` |
| Target Group | `valuehub-gateway-tg` / HTTP / 8000 / `GET /health` |
| ACM | `api.valuehub.art` (Issued) |
| ALB SG | `valuehub-alb-sg` — 인바운드 80, 443 `0.0.0.0/0` |
| Apps SG | `valuehub-apps-sg` — 8000은 ALB SG + 관리자 `/32` |

ACM ARN:

`arn:aws:acm:ap-northeast-2:471112928396:certificate/a8c5a88a-8bf5-4cde-96e8-965b5c60e004`

배포는 기존과 같다. GitHub Actions가 Apps EC2 `/opt/valuehub-aws-infra/compose.prod-apps.yml` 로 Gateway만 재기동한다. ALB/인증서는 배포 파이프라인 밖이다.

Gateway Forwarded 헤더 변경: PR [#79](https://github.com/SpartaValueHub/Gateway-Service/pull/79) (`develop` 머지, EC2 반영됨).

---

## 가비아 DNS (`valuehub.art`)

네임서버는 가비아다. Route53이 아니다.

| 타입 | 호스트 | 값 |
|------|--------|-----|
| A | `@` | 기존 (FE/기타) |
| CNAME | `www` | Vercel |
| CNAME | `api` | `valuehub-api-alb-1844605244.ap-northeast-2.elb.amazonaws.com.` |
| CNAME | `_fc161deb1e78b9b612beb90f1f9b7d10.api` | ACM 검증용. **삭제하지 말 것** |

`api`는 A(EIP)가 아니라 CNAME이어야 한다. ALB IP는 바뀐다.

---

## Gateway 코드

- `StripUntrustedForwardedHeadersFilter`  
  - 공인 IP → `X-Forwarded-*` 제거 (spoofing 방지)  
  - VPC 사설 IP(ALB) → ALB가 붙인 `X-Forwarded-Proto=https` 유지
- `spring.cloud.gateway.server.webflux.trusted-proxies` — RFC1918
- `x-forwarded.proto-append: false` — `https,http` 로 안 붙게

Auth prod 쿠키는 이미 `Secure=true`, `SameSite=None`. CORS Origin에 `https://valuehub.art` 가 있다.

---

## 확인

```bash
curl.exe -sS -w "\nHTTP %{http_code}\n" https://api.valuehub.art/health
```

기대: `{"status":"UP"}` / 200, 브라우저 자물쇠.

콘솔:

1. 서울 리전 → Load Balancers → `valuehub-api-alb` **Active**
2. Target Groups → `valuehub-gateway-tg` → `valuehub-apps` **healthy**
3. Certificate Manager → `api.valuehub.art` **Issued**
4. `valuehub-apps-sg` 인바운드 8000에 `0.0.0.0/0` 없음

FE API base URL은 `https://api.valuehub.art` 이어야 한다. `http://IP:8000` 은 쓰지 않는다.

---

## 운영 메모

- ACM은 DNS 검증 CNAME이 남아 있으면 갱신된다. 가비아의 `_fc161deb…api` 레코드를 지우면 갱신이 실패한다.
- Spring `server.ssl` / 서비스별 인증서는 넣지 않는다.
- 8000 공인 오픈은 닫혀 있다. 관리자 IP(`210.117.11.71/32`)만 EIP:8000 직접 접속이 가능하다.
- SSH(22), Eureka(8761) 공인 오픈은 이 작업 범위 밖이다.
- `develop` / `main` push 모두 같은 Apps EC2에 Gateway를 배포한다.
