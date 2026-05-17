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
- Google Cloud Run + Cloud SQL へのデプロイ
- TerraformによるGoogle Cloudインフラ管理
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
| ロギング | Logback + logstash-logback-encoder + kotlin-logging | - |
| 静的解析 | Detekt | - |
| フォーマッタ | ktlint (ktlint-gradle) | - |
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
├── config/
│   └── detekt/
│       └── detekt.yml
├── .github/workflows/
│   ├── ci.yml
│   └── deploy.yml
├── Dockerfile
├── docker-compose.yml
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

### 4.3 User（ユーザ）

| フィールド | 型 | 必須 | 説明 |
|---|---|---|---|
| id | UserId (UUID) | ✓ | ユーザID |
| loginId | String | ✓ | ログインID（一意、最大100文字） |
| passwordHash | String | ✓ | パスワードハッシュ（bcrypt） |
| name | String | ✓ | 表示名（最大255文字） |
| createdAt | Instant | ✓ | 登録日時 |

### 4.4 ビジネスルール

- `endDate` は `startDate` より後の日付でなければならない
- `status` はDBに保存された値を正とし、検索・レスポンスともに保存値を返す
- `endDate` が現在日付より過去かつ `autoRenewal = false` の契約は、作成・更新時に `EXPIRED` として保存する
- `title` と `counterparty` は空文字を許容しない

---

## 5. API仕様

### 5.1 共通仕様

- ベースパス：`/api/v1`
- リクエスト・レスポンス：JSON（`Content-Type: application/json`）
- 日付フォーマット：ISO 8601（`YYYY-MM-DD`）
- 日時フォーマット：ISO 8601（`yyyy-MM-dd'T'HH:mm:ss'Z'`）
- 認証：`Authorization: Bearer <JWT>`

### 5.2 認証仕様

- `POST /api/v1/auth/login` でID/パスワードを検証し、JWTを発行する
- 契約管理APIはJWT Bearer認証を必須とする
- `POST /api/v1/auth/login` と `GET /health` は認証不要とする
- パスワードは **bcrypt** でハッシュ化して保存する（cost factor 10以上）
- JWT署名アルゴリズムは **HS256** を使用する
- JWTには少なくとも `sub`（ユーザID）, `iat`, `exp` を含める
- JWT署名鍵は環境変数またはSecret Managerから注入する
- トークン有効期限は短めに設定する（例：1時間）
- ログインエンドポイントには **Rate Limiting** を適用する（Ktor RateLimit プラグイン）
- まずはアプリ全体で1分間に最大60回までに制限し、超過時は `429 Too Many Requests` を返す
- IP単位やユーザ単位の細かい制限は、必要になった段階でCloud Armorまたはアプリ実装で追加する

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
  "password": "<password>"
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

### 5.5 契約API詳細

#### GET /api/v1/contracts

クエリパラメータ: `status`（フィルタ）, `counterparty`（部分一致）, `limit`（デフォルト50、最大100）, `offset`（デフォルト0）

**レスポンス（200 OK）**
```json
{
  "contracts": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "業務委託基本契約書",
      "counterparty": "株式会社サンプル",
      "startDate": "2025-04-01",
      "endDate": "2026-03-31",
      "autoRenewal": true,
      "status": "ACTIVE",
      "createdAt": "2025-03-15T10:00:00Z",
      "updatedAt": "2025-03-15T10:00:00Z"
    }
  ],
  "total": 1
}
```

#### POST /api/v1/contracts → 201 Created（作成された Contract）
#### GET /api/v1/contracts/{id} → 200 OK（Contract） / 404 Not Found
#### PATCH /api/v1/contracts/{id} → 200 OK（更新後の Contract）。全フィールド任意。
#### DELETE /api/v1/contracts/{id} → 204 No Content
#### GET /health → 200 OK（`{"status": "UP", "database": "UP"}`）

### 5.6 エラーレスポンス

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
| 429 | RATE_LIMITED | ログイン試行回数の上限超過 |
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

### 6.2 users テーブル

```sql
CREATE TABLE users (
    id             UUID PRIMARY KEY,
    login_id       VARCHAR(100) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    name           VARCHAR(255) NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
```

### 6.3 マイグレーション

Flywayで管理。`src/main/resources/db/migration/` にスキーマを配置し、アプリ起動時に自動適用する。

初期ユーザは学習用としてFlywayの初期データ投入SQLで1件だけ作成する。公開リポジトリには平文パスワードを書かず、bcrypt済みのハッシュのみを置く。

```sql
INSERT INTO users (id, login_id, password_hash, name)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    '<bcrypt-hash>',
    'Administrator'
);
```

本番運用時は初回ログイン後にパスワードを変更できる仕組みを追加するか、管理用SQLでハッシュを差し替える。

---

## 7. ロギング

- **構造化ログ（JSON形式）** を採用する
- `logstash-logback-encoder` を使用し、Logback から JSON 形式でログを出力する
- Cloud Logging との連携により、`severity`（ログレベル）や `trace`（トレースID）が自動的にマッピングされる
- ローカル開発時はコンソール出力（テキスト形式）に切り替え可能とする（`logback.xml` のプロファイル切替）
- リクエスト単位で以下の情報を MDC に含める：`requestId`, `method`, `path`

---

## 8. ローカル開発環境

```
docker-compose up -d   # PostgreSQL起動
./gradlew run           # Ktorアプリ起動 (localhost:8080)
```

```
┌───────────────────┐
│  Ktor App (:8080) │
│  (ホスト直接実行)    │
└───────┬───────────┘
        │ TCP
┌───────▼───────────┐
│  PostgreSQL (:5432)│
│  (Docker Compose)  │
└───────────────────┘
```

---

## 9. インフラ構成

### 9.1 方針

Google Cloudの構成は、最初からTerraformで完璧に作るのではなく、学習しやすい順序で進める。

1. Google Cloud Console / `gcloud` で手動構築する
2. Cloud Run + Cloud SQL でアプリが動くことを確認する
3. 手動構築した内容をTerraformコードに落とし込む
4. 必要に応じて `terraform import` で既存リソースをTerraform stateに取り込む
5. 以後の変更をTerraformで管理する

### 9.2 手動構築対象

- Artifact Registry（コンテナイメージ保管）
- Cloud Runサービス（API本体）
- Cloud SQL for PostgreSQL（本番DB）
- Secret Manager（DBパスワード等）
- Cloud Run実行用サービスアカウントと必要最小限のIAM

### 9.3 Terraform管理対象

手動構築で動作確認できたあと、以下をTerraform管理に移行する。

- Artifact Registry
- Cloud Run
- Cloud SQL for PostgreSQL
- Secret Manager
- Cloud Run実行用サービスアカウントとIAM

### 9.4 想定モジュール

- `modules/artifact_registry`
- `modules/cloud_sql_postgres`
- `modules/cloud_run_service`

### 9.5 Terraform運用

- 環境は本番環境のみを対象とする
- Terraform stateはGCSバックエンドで管理する
- Terraform導入前は、Google Cloud Console / `gcloud` による手動変更を許容する
- Terraform導入後は、原則としてTerraform経由で変更する
- Pull Requestでは `terraform plan` まで実行する
- `terraform apply` は慣れるまではローカル端末から手動実行し、運用に慣れたらGitHub Actionsの手動実行へ移行する
- `terraform.tfvars` はコミットせず、公開してよい値は `terraform.tfvars.example` に記載する

Terraform導入時に手動で準備するもの:
- Terraform state保存用GCS bucket

GitHub ActionsからTerraformを実行する段階で追加するもの:
- GitHub Actions用Workload Identity Pool / Provider
- GitHub Actionsが利用するデプロイ用サービスアカウント

### 9.6 本番構成（Google Cloud）

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

## 10. 環境変数

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

## 11. CI/CD

### 11.1 GitHub Actionsワークフロー

| トリガー | 内容 |
|---|---|
| Pull Request | `test` + `detekt` + `ktlintCheck` |
| Terraform導入後のPull Request | `terraform fmt/validate` + `terraform plan` |
| main push | テスト → Dockerビルド → Artifact Registry push → Cloud Run deploy |
| Terraform運用移行後の手動実行 | `terraform apply` による本番インフラ更新 |

### 11.2 認証方式（GitHub Actions → GCP）

- サービスアカウント鍵の長期保管は行わない
- GitHub OIDC + Workload Identity Federation で短命クレデンシャルを払い出す
- 必要最小権限のサービスアカウントを利用する

---

## 12. コーディングエージェントでの実装手順

このプロジェクトは、コーディングエージェントに小さな単位で依頼しながら実装する。各ステップでは、実装・テスト・READMEとの整合確認までを1セットにする。

### 12.1 進め方のルール

- 1回の依頼では、1つのレイヤーまたは1つの機能だけを対象にする
- 仕様変更が必要になった場合は、先にREADMEを更新してから実装する
- 各ステップ完了時に `./gradlew test` を実行する
- DBやCloud Runなど外部サービスが必要な変更は、ローカルで代替確認してからGCPに進める
- 生成されたコードは、既存のディレクトリ構成と命名に合わせる

### 12.2 実装ステップ

| Step | 依頼内容 | 完了条件 |
|---|---|---|
| 1 | Gradle依存関係とプロジェクト構成を整える | Ktor, Exposed, Flyway, Koin, JWT, bcrypt, Detekt, ktlint が導入される |
| 2 | ドメインモデルを実装する | `Contract`, `ContractId`, `ContractStatus`, `User` と単体テストがある |
| 3 | DBマイグレーションを実装する | `contracts`, `users` テーブルと初期ユーザ投入SQLがある |
| 4 | RepositoryとDB接続を実装する | PostgreSQLに対するCRUDが統合テストで確認できる |
| 5 | UseCaseを実装する | 契約CRUDとログイン処理がApplication層に実装される |
| 6 | Ktor RoutingとDTOを実装する | `/api/v1/auth/login`, `/api/v1/contracts`, `/health` が動作する |
| 7 | JWT認証を実装する | ログイン後のJWTで契約APIにアクセスできる |
| 8 | エラーハンドリングとログを整備する | 共通エラーレスポンスと構造化ログが出力される |
| 9 | Dockerfileとdocker-composeを整備する | ローカルでPostgreSQL + アプリを起動できる |
| 10 | GitHub ActionsのCIを作る | PRでテスト、Detekt、ktlintが実行される |
| 11 | GCPを手動構築してデプロイする | Cloud RunからCloud SQLへ接続でき、APIが動作する |
| 12 | 手動構築したGCP構成をTerraform化する | Terraformで同等の構成を管理できる |
| 13 | CDを整備する | main pushでビルド済みイメージをCloud Runへデプロイできる |

### 12.3 コーディングエージェントへの依頼例

```text
READMEの仕様に従って、Step 2 のドメインモデルだけを実装してください。
既存ファイルを確認し、必要なテストも追加してください。
完了後に ./gradlew test を実行し、変更内容と残課題を報告してください。
```

```text
READMEの仕様に従って、Step 6 のKtor RoutingとDTOだけを実装してください。
DBや認証の未実装部分がある場合は、既存のUseCaseインターフェースに合わせて進めてください。
```

---

## 13. コード品質

- **Detekt** で静的解析を実行する（`./gradlew detekt`）
  - カスタムルールセットは `config/detekt/detekt.yml` に配置
  - complexity, style, potential-bugs ルールを有効化
- **ktlint** でコードフォーマットを統一する（`./gradlew ktlintCheck` / `./gradlew ktlintFormat`）
- CI（Pull Request）で `detekt` と `ktlintCheck` を必須チェックとして実行する

---

## 14. テスト方針

| レイヤー | テスト種別 | ツール |
|---|---|---|
| Domain | 単体テスト | Kotest |
| Application (UseCase) | 単体テスト（Repositoryをモック） | Kotest + MockK |
| Infrastructure | 統合テスト（実DB使用） | Testcontainers (PostgreSQL) |
| Presentation | APIテスト | Ktor TestApplication |
| Security | ログイン・JWT検証テスト | Ktor TestApplication |

カバレッジ目標：Domain層・Application層は **80%以上**。

---

## 15. 参考

- Ktor 公式ドキュメント: https://ktor.io/docs/
- Exposed Wiki: https://github.com/JetBrains/Exposed/wiki
- Terraform Google Provider: https://registry.terraform.io/providers/hashicorp/google/latest/docs
- Cloud Run + Cloud SQL 接続ガイド: https://cloud.google.com/sql/docs/postgres/connect-run
