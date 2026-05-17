# ktor-contract-api
## 1. プロジェクト概要

### 1.1 プロジェクト名
`ktor-contract-api`

### 1.2 目的
Kotlin + Ktor + PostgreSQL + Cloud Run + Cloud SQL を実際に手を動かして習熟することを目的とした、簡易契約管理APIのサンプル実装である。

### 1.3 スコープ
- 契約データのCRUD操作を提供するREST API
- DDDレイヤードアーキテクチャの実装
- Google Cloudへのコンテナデプロイ
- GitHub ActionsによるCI/CDパイプライン

### 1.4 非スコープ
- フロントエンドUIの実装
- 認証・認可機構（OIDC, JWT等）
- 契約書のOCRやAI処理
- 本格的な権限管理

---

## 2. 技術スタック

| カテゴリ | 技術 | バージョン |
|---|---|---|
| 言語 | Kotlin | 2.0.x |
| ランタイム | JVM | 21 |
| Webフレームワーク | Ktor | 3.0.x |
| ビルドツール | Gradle (Kotlin DSL) | 8.5+ |
| ORM | Exposed | 0.56.x |
| DB接続プール | HikariCP | 5.x |
| DI | Koin | 4.0.x |
| シリアライゼーション | kotlinx.serialization | 1.7.x |
| ロギング | Logback + kotlin-logging | - |
| データベース | PostgreSQL | 15 |
| マイグレーション | Flyway | 10.x |
| テスト | Kotest, MockK, Testcontainers | - |
| コンテナ | Docker | - |
| 本番インフラ | Google Cloud Run + Cloud SQL | - |
| CI/CD | GitHub Actions | - |
| Artifact管理 | Google Artifact Registry | - |

---

## 3. アーキテクチャ

### 3.1 レイヤー構成（DDD）

```
┌─────────────────────────────────────┐
│  Presentation 層                     │  Ktor Routing, DTO
├─────────────────────────────────────┤
│  Application 層                      │  UseCase
├─────────────────────────────────────┤
│  Domain 層                           │  Entity, ValueObject, Repository Interface
├─────────────────────────────────────┤
│  Infrastructure 層                   │  Exposed実装, DB接続
└─────────────────────────────────────┘
```

依存方向は **外側 → 内側** のみ。Domain層は他のいずれの層にも依存しない。

### 3.2 ディレクトリ構成

```
ktor-contract-api/
├── src/
│   ├── main/
│   │   ├── kotlin/com/example/contract/
│   │   │   ├── Application.kt                  # エントリポイント
│   │   │   ├── config/
│   │   │   │   ├── DatabaseConfig.kt           # DB接続設定
│   │   │   │   └── KoinConfig.kt               # DI設定
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   ├── Contract.kt
│   │   │   │   │   ├── ContractId.kt
│   │   │   │   │   └── ContractStatus.kt
│   │   │   │   └── repository/
│   │   │   │       └── ContractRepository.kt   # Interface
│   │   │   ├── application/
│   │   │   │   └── usecase/
│   │   │   │       ├── ListContractsUseCase.kt
│   │   │   │       ├── GetContractUseCase.kt
│   │   │   │       ├── CreateContractUseCase.kt
│   │   │   │       ├── UpdateContractUseCase.kt
│   │   │   │       └── DeleteContractUseCase.kt
│   │   │   ├── infrastructure/
│   │   │   │   └── repository/
│   │   │   │       ├── ContractsTable.kt       # Exposed Table定義
│   │   │   │       └── ContractRepositoryImpl.kt
│   │   │   └── presentation/
│   │   │       ├── routing/
│   │   │       │   └── ContractRouting.kt
│   │   │       ├── dto/
│   │   │       │   ├── ContractRequest.kt
│   │   │       │   └── ContractResponse.kt
│   │   │       └── error/
│   │   │           └── ErrorHandler.kt
│   │   └── resources/
│   │       ├── application.conf                # Ktor設定
│   │       ├── logback.xml
│   │       └── db/migration/                   # Flyway
│   │           └── V1__create_contracts_table.sql
│   └── test/
│       └── kotlin/com/example/contract/
│           ├── domain/                         # ドメイン単体テスト
│           ├── application/                    # UseCase テスト
│           └── infrastructure/                 # Testcontainers
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── Dockerfile
├── docker-compose.yml
├── .github/workflows/
│   └── deploy.yml
├── .gitignore
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

### 5.2 エンドポイント一覧

| Method | Path | 説明 |
|---|---|---|
| GET | `/api/v1/contracts` | 契約一覧取得 |
| POST | `/api/v1/contracts` | 契約新規登録 |
| GET | `/api/v1/contracts/{id}` | 契約詳細取得 |
| PATCH | `/api/v1/contracts/{id}` | 契約更新 |
| DELETE | `/api/v1/contracts/{id}` | 契約削除 |
| GET | `/health` | ヘルスチェック |

### 5.3 詳細仕様

#### 5.3.1 GET /api/v1/contracts

**クエリパラメータ**

| パラメータ | 型 | 必須 | 説明 |
|---|---|---|---|
| status | String | - | 契約状態でフィルタ |
| counterparty | String | - | 契約先名で部分一致検索 |
| limit | Int | - | 取得件数（デフォルト50、最大100） |
| offset | Int | - | オフセット（デフォルト0） |

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

#### 5.3.2 POST /api/v1/contracts

**リクエストボディ**

```json
{
  "title": "業務委託基本契約書",
  "counterparty": "株式会社サンプル",
  "startDate": "2025-04-01",
  "endDate": "2026-03-31",
  "autoRenewal": true
}
```

**レスポンス（201 Created）**

作成された Contract オブジェクト。

#### 5.3.3 GET /api/v1/contracts/{id}

**レスポンス（200 OK）**

Contract オブジェクト。存在しない場合は `404 Not Found`。

#### 5.3.4 PATCH /api/v1/contracts/{id}

**リクエストボディ**（すべてのフィールドが任意）

```json
{
  "title": "業務委託基本契約書（改訂版）",
  "endDate": "2027-03-31",
  "autoRenewal": false,
  "status": "ACTIVE"
}
```

**レスポンス（200 OK）**

更新後の Contract オブジェクト。

#### 5.3.5 DELETE /api/v1/contracts/{id}

**レスポンス（204 No Content）**

ボディなし。

#### 5.3.6 GET /health

**レスポンス（200 OK）**

```json
{
  "status": "UP",
  "database": "UP"
}
```

### 5.4 エラーレスポンス

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

Flywayで管理。`src/main/resources/db/migration/V1__create_contracts_table.sql` に初期スキーマを配置し、アプリ起動時に自動適用する。

---

## 7. インフラ構成

### 7.1 ローカル開発環境

```
┌──────────────────────┐
│  Docker Compose      │
│  ┌────────────────┐  │
│  │  Ktor App      │  │
│  │  :8080         │  │
│  └───────┬────────┘  │
│          │ TCP       │
│  ┌───────▼────────┐  │
│  │  PostgreSQL 15 │  │
│  │  :5432         │  │
│  └────────────────┘  │
└──────────────────────┘
```

### 7.2 本番環境（Google Cloud）

```
[クライアント]
     │ HTTPS
     ▼
[Cloud Run] ← Artifact Registry からイメージ取得
     │
     │ Unix socket (Cloud SQL Auth Proxy)
     ▼
[Cloud SQL (PostgreSQL 15)]
     ・db-f1-micro
     ・asia-northeast1
     ・Private IP
```

### 7.3 環境変数

| 変数名 | 用途 | ローカル例 | Cloud Run例 |
|---|---|---|---|
| DB_HOST | DB接続先 | `localhost` | `/cloudsql/PROJECT:REGION:INSTANCE` |
| DB_PORT | DBポート | `5432` | （未使用） |
| DB_NAME | DB名 | `contract_one` | `contract_one` |
| DB_USER | DBユーザ | `appuser` | `appuser` |
| DB_PASSWORD | DBパスワード | `password` | Secret Manager から注入 |
| PORT | Ktorリッスンポート | `8080` | `8080`（Cloud Runが自動設定） |

---

## 8. CI/CD

### 8.1 GitHub Actions ワークフロー

| トリガー | 内容 |
|---|---|
| Pull Request | テスト実行（`gradle test`） |
| main ブランチへのpush | テスト → Dockerビルド → Artifact Registryへpush → Cloud Runへデプロイ |

### 8.2 シークレット

| シークレット名 | 用途 |
|---|---|
| `GCP_SA_KEY` | デプロイ用サービスアカウントキー |
| `GCP_PROJECT_ID` | GCPプロジェクトID |

---

## 9. テスト方針

| レイヤー | テスト種別 | ツール |
|---|---|---|
| Domain | 単体テスト | Kotest |
| Application (UseCase) | 単体テスト（Repositoryをモック） | Kotest + MockK |
| Infrastructure | 統合テスト（実DB使用） | Testcontainers (PostgreSQL) |
| Presentation | E2Eテスト | Ktor TestApplication |

カバレッジ目標：Domain層・Application層は **80%以上**。

---

## 10. 開発マイルストーン

| Phase | 内容 | 完了条件 |
|---|---|---|
| Phase 1 | プロジェクト初期化 | Gradle構成、Hello Worldエンドポイント |
| Phase 2 | ドメイン実装 | Contract Entity、Repository Interface、UseCase |
| Phase 3 | DB連携 | Exposed実装、Flywayマイグレーション、docker-composeで起動確認 |
| Phase 4 | API実装 | 全CRUDエンドポイント完成、ローカルでcurlによる動作確認 |
| Phase 5 | テスト整備 | 単体テスト・統合テストを実装 |
| Phase 6 | GCPデプロイ | Cloud SQL作成、手動でCloud Runにデプロイ成功 |
| Phase 7 | CI/CD | GitHub Actionsによる自動デプロイ |
| Phase 8 | README整備 | セットアップ手順、アーキテクチャ図、技術選定理由を記述 |

---

## 11. README に書くべき内容

- プロジェクト概要
- アーキテクチャ図
- 技術選定理由（特にContract Oneとの対応関係）
- ローカル起動手順
- デプロイ手順
- ディレクトリ構成の説明
- 工夫した点・学んだこと

ポートフォリオとして提示する際の最重要ファイル。

---

## 12. 参考

- Sansan Contract One Engineering Unit 紹介資料 (Speaker Deck)
- Ktor 公式ドキュメント: https://ktor.io/docs/
- Exposed Wiki: https://github.com/JetBrains/Exposed/wiki
- Cloud Run + Cloud SQL接続ガイド: https://cloud.google.com/sql/docs/postgres/connect-run