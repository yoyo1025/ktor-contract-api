# ktor-contract-api

## 1. プロジェクト概要

### 1.1 プロジェクト名
`ktor-contract-api`

### 1.2 目的
Kotlin + Ktor + PostgreSQL + Cloud Run + Cloud SQL を実際に手を動かして習熟することを目的とした、簡易契約管理APIのサンプル実装である。

### 1.3 スコープ
- 契約データのCRUD操作を提供するREST API
- DDDレイヤードアーキテクチャの実装
- ID/パスワードログインとJWTによるAPI認証
- TerraformによるGoogle Cloudインフラ構築
- GitHub ActionsによるCI/CDパイプライン

### 1.4 非スコープ
- フロントエンドUIの実装
- 契約書のOCRやAI処理
- 本格的な権限管理（ABAC/ReBACなど）

---

## 2. 技術スタック

| カテゴリ | 技術 | バージョン |
|---|---|---|
| 言語 | Kotlin | 2.3.10 |
| ランタイム | JVM | 21 |
| Webフレームワーク | Ktor | 3.1.2 |
| ビルドツール | Gradle (Kotlin DSL) | 9.3.0 |
| ORM | Exposed | 0.56.x |
| DB接続プール | HikariCP | 5.x |
| DI | Koin | 4.0.x |
| シリアライゼーション | kotlinx.serialization | 1.7.x |
| ロギング | Logback + kotlin-logging | - |
| データベース | PostgreSQL | 15 |
| マイグレーション | Flyway | 10.x |
| API認証 | JWT Bearer認証 | - |
| IaC | Terraform | 1.9+ |
| 本番インフラ | Google Cloud Run + Cloud SQL | - |
| CI/CD | GitHub Actions | - |
| Artifact管理 | Google Artifact Registry | - |

---

## 3. アーキテクチャ

### 3.1 レイヤー構成（DDD）

```
┌─────────────────────────────────────┐
│  Presentation 層                     │  Ktor Routing, DTO, Auth Guard
├─────────────────────────────────────┤
│  Application 層                      │  UseCase
├─────────────────────────────────────┤
│  Domain 層                           │  Entity, ValueObject, Repository Interface
├─────────────────────────────────────┤
│  Infrastructure 層                   │  Exposed実装, DB接続, JWT発行/検証
└─────────────────────────────────────┘
```

依存方向は **外側 → 内側** のみ。Domain層は他のいずれの層にも依存しない。

### 3.2 ディレクトリ構成（仕様）

```text
ktor-contract-api/
├── src/
│   ├── main/
│   │   ├── kotlin/com/example/contract/
│   │   │   ├── Application.kt
│   │   │   ├── config/
│   │   │   │   ├── DatabaseConfig.kt
│   │   │   │   ├── KoinConfig.kt
│   │   │   │   └── SecurityConfig.kt
│   │   │   ├── domain/
│   │   │   ├── application/
│   │   │   ├── infrastructure/
│   │   │   └── presentation/
│   │   └── resources/
│   │       ├── application.conf
│   │       └── db/migration/
│   └── test/
├── infra/
│   └── terraform/
│       ├── main.tf
│       ├── variables.tf
│       ├── outputs.tf
│       ├── terraform.tfvars.example
│       ├── modules/
│       │   ├── cloud_run_service/
│       │   ├── cloud_sql_postgres/
│       │   └── artifact_registry/
├── .github/workflows/
│   ├── ci.yml
│   └── deploy.yml
└── README.md
```

---

## 4. ドメインモデル

### 4.1 Contract（契約）

| フィールド | 型 | 必須 | 説明 |
|---|---|---|---|
| id | ContractId (UUID) | ✓ | 契約ID |
| title | String | ✓ | 契約書タイトル（最大255文字） |
| counterparty | String | ✓ | 契約先名（最大255文字） |
| startDate | LocalDate | ✓ | 契約開始日 |
| endDate | LocalDate? | - | 契約終了日 |
| autoRenewal | Boolean | ✓ | 自動更新有無 |
| status | ContractStatus | ✓ | 契約状態 |
| createdAt | Instant | ✓ | 登録日時 |
| updatedAt | Instant | ✓ | 更新日時 |

### 4.2 ContractStatus（契約状態）

| 値 | 説明 |
|---|---|
| `ACTIVE` | 契約中 |
| `EXPIRED` | 契約終了 |
| `CANCELLED` | 解約済み |

### 4.3 ビジネスルール

- `endDate` は `startDate` より後の日付でなければならない
- `endDate` が現在日付より過去かつ `autoRenewal = false` の場合、`status` は自動的に `EXPIRED` と判定される
- `title` と `counterparty` は空文字を許容しない

---

## 5. API仕様

### 5.1 共通仕様

- ベースパス：`/api/v1`
- リクエスト・レスポンス：JSON（`Content-Type: application/json`）
- 日付フォーマット：ISO 8601（`YYYY-MM-DD`）
- 日時フォーマット：ISO 8601（`YYYY-MM-DDTHH:MM:SSZ`）
- 認証：`Authorization: Bearer <JWT>`

### 5.2 認証仕様

- `POST /api/v1/auth/login` でID/パスワードを検証し、JWTを発行する
- 契約管理APIはJWT Bearer認証を必須とする
- `POST /api/v1/auth/login` と `GET /health` は認証不要とする
- パスワードは平文保存せず、ハッシュ化して保存する
- JWTには少なくとも `sub`, `iat`, `exp` を含める
- JWT署名鍵は環境変数またはSecret Managerから注入する
- トークン有効期限は短めに設定する（例：1時間）

### 5.3 エンドポイント一覧

| Method | Path | 説明 | 認証 |
|---|---|---|---|
| POST | `/api/v1/auth/login` | ログイン・JWT発行 | 不要 |
| GET | `/api/v1/contracts` | 契約一覧取得 | 要 |
| POST | `/api/v1/contracts` | 契約新規登録 | 要 |
| GET | `/api/v1/contracts/{id}` | 契約詳細取得 | 要 |
| PATCH | `/api/v1/contracts/{id}` | 契約更新 | 要 |
| DELETE | `/api/v1/contracts/{id}` | 契約削除 | 要 |
| GET | `/health` | ヘルスチェック | 不要 |

### 5.4 ログインAPI

#### 5.4.1 POST /api/v1/auth/login

**リクエストボディ**

```json
{
  "loginId": "admin",
  "password": "password"
}
```

**レスポンス（200 OK）**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### 5.5 エラーレスポンス

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "title must not be blank",
    "details": [
      {
        "field": "title",
        "reason": "must not be blank"
      }
    ]
  }
}
```

| ステータス | エラーコード | 発生条件 |
|---|---|---|
| 400 | VALIDATION_ERROR | リクエストのバリデーション失敗 |
| 401 | UNAUTHORIZED | ログイン失敗、JWTなし、JWT検証失敗 |
| 404 | NOT_FOUND | 指定IDのリソースが存在しない |
| 500 | INTERNAL_ERROR | 予期せぬサーバエラー |

---

## 6. データベース設計

### 6.1 contracts テーブル

```sql
CREATE TABLE contracts (
    id            UUID PRIMARY KEY,
    title         VARCHAR(255) NOT NULL,
    counterparty  VARCHAR(255) NOT NULL,
    start_date    DATE NOT NULL,
    end_date      DATE,
    auto_renewal  BOOLEAN NOT NULL DEFAULT FALSE,
    status        VARCHAR(20) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_contracts_counterparty ON contracts(counterparty);
CREATE INDEX idx_contracts_status ON contracts(status);
CREATE INDEX idx_contracts_end_date ON contracts(end_date);
```

### 6.2 マイグレーション

Flywayで管理。`src/main/resources/db/migration/` にスキーマを配置し、アプリ起動時に自動適用する。

---

## 7. インフラ構成（Terraform管理）

### 7.1 Terraform管理対象

- Artifact Registry（コンテナイメージ保管）
- Cloud Runサービス（API本体）
- Cloud SQL for PostgreSQL（本番DB）
- Secret Manager（DBパスワード等）
- Cloud Run実行用サービスアカウントと最小IAM

### 7.2 想定モジュール

- `modules/artifact_registry`
- `modules/cloud_sql_postgres`
- `modules/cloud_run_service`

### 7.3 Terraform運用

- 環境は本番環境のみを対象とする
- Terraform stateはGCSバックエンドで管理する
- `plan` と `apply` はGitHub Actionsから実行する（手動承認ゲートあり）
- `terraform.tfvars` はコミットせず、公開してよい値は `terraform.tfvars.example` に記載する

### 7.4 本番構成（Google Cloud）

```
[Client]
   │ HTTPS
   ▼
[Cloud Run]
   │ Cloud SQL Connector
   ▼
[Cloud SQL (PostgreSQL 15)]
```

---

## 8. 環境変数

| 変数名 | 用途 | 例 |
|---|---|---|
| DB_HOST | DB接続先 | `localhost` / `/cloudsql/PROJECT:REGION:INSTANCE` |
| DB_PORT | DBポート | `5432` |
| DB_NAME | DB名 | `contract_one` |
| DB_USER | DBユーザ | `appuser` |
| DB_PASSWORD | DBパスワード | Secret Managerから注入 |
| JWT_SECRET | JWT署名鍵 | Secret Managerから注入 |
| JWT_ISSUER | JWT発行者 | `ktor-contract-api` |
| JWT_AUDIENCE | JWT対象 | `ktor-contract-api` |
| JWT_EXPIRES_IN_SECONDS | JWT有効期限秒数 | `3600` |
| PORT | Ktorリッスンポート | `8080` |

---

## 9. CI/CD

### 9.1 GitHub Actionsワークフロー

| トリガー | 内容 |
|---|---|
| Pull Request | `test` + `terraform fmt/validate` + `terraform plan` |
| main push | テスト → Dockerビルド → Artifact Registry push → Cloud Run deploy |

### 9.2 認証方式（GitHub Actions → GCP）

- サービスアカウント鍵の長期保管は行わない
- GitHub OIDC + Workload Identity Federation で短命クレデンシャルを払い出す
- 必要最小権限のサービスアカウントを利用する

---

## 10. テスト方針

| レイヤー | テスト種別 | ツール |
|---|---|---|
| Domain | 単体テスト | Kotest |
| Application (UseCase) | 単体テスト（Repositoryをモック） | Kotest + MockK |
| Infrastructure | 統合テスト（実DB使用） | Testcontainers (PostgreSQL) |
| Presentation | APIテスト | Ktor TestApplication |
| Security | ログイン・JWT検証テスト | Ktor TestApplication |

カバレッジ目標：Domain層・Application層は **80%以上**。

---

## 11. 参考

- Ktor 公式ドキュメント: https://ktor.io/docs/
- OpenID Connect Core 1.0: https://openid.net/specs/openid-connect-core-1_0.html
- Terraform Google Provider: https://registry.terraform.io/providers/hashicorp/google/latest/docs
- Cloud Run + Cloud SQL 接続ガイド: https://cloud.google.com/sql/docs/postgres/connect-run
