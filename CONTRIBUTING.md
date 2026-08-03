# 기여 가이드

## 커밋 메시지 규칙

커밋 메시지는 아래 형식을 사용합니다.

```
<타입>: <한글 설명>
```

### 타입

| 타입 | 설명 |
|------|------|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |

### 예시

```
feat: Gateway Docker 배포 파이프라인 추가
fix: Eureka 서비스 디스커버리 연결 오류 수정
```

## 브랜치

- `main`: 운영 배포 브랜치 (push 시 EC2 자동 배포)
- `develop`: 개발 브랜치

## 브랜치 명명 규칙

1. **GitHub Issue를 먼저 생성**한다.
2. 브랜치명: `{type}/{issue-number}-{short-slug}`
   - `type`: `feat`, `fix`, `docs`, `chore`, `refactor` 등
   - `issue-number`: GitHub Issue 번호
   - `short-slug`: 작업 내용을 짧게 영문/kebab-case로 표현
3. 예시
   - Issue #1 → `feat/1-login-api`
   - Issue #12 → `docs/12-readme-unify`

## PR 제목 규칙

PR 제목 형식은 [.github/pull_request_template.md](.github/pull_request_template.md)를 참고하세요.

예: `[#{Issue Number}] Feature : 작업 내용`
