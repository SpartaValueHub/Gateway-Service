# Gateway Service

Spring Cloud Gateway 기반 API Gateway입니다.

## 로컬 실행

```bash
./gradlew bootRun
```

- Gateway: http://localhost:8000
- Health: http://localhost:8000/health

## Docker 실행

```bash
docker compose up -d --build
```

## 배포

`main` 브랜치 push 시 GitHub Actions를 통해 EC2에 자동 배포됩니다.

### GitHub Secrets

| Secret | 설명 |
|--------|------|
| `EC2_HOST` | App EC2 Elastic IP |
| `EC2_USER` | `ec2-user` |
| `EC2_SSH_KEY` | SSH private key (PEM) |

## 커밋 규칙

`feat:` / `fix:` 접두어 + 한글 설명. 자세한 내용은 [CONTRIBUTING.md](CONTRIBUTING.md) 참고.
