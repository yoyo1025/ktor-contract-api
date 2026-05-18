package com.example.contract.infra

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files
import java.nio.file.Path

class TerraformConfigTest : DescribeSpec({
    val terraformDir = Path.of("infra", "terraform")
    val modulesDir = terraformDir.resolve("modules")

    describe("Terraform ディレクトリ構成") {
        it("README で指定されたファイルが存在する") {
            Files.exists(terraformDir.resolve("main.tf")) shouldBe true
            Files.exists(terraformDir.resolve("variables.tf")) shouldBe true
            Files.exists(terraformDir.resolve("outputs.tf")) shouldBe true
            Files.exists(terraformDir.resolve("terraform.tfvars.example")) shouldBe true
        }

        it("README で指定された3つのモジュールが存在する") {
            Files.isDirectory(modulesDir.resolve("artifact_registry")) shouldBe true
            Files.isDirectory(modulesDir.resolve("cloud_sql_postgres")) shouldBe true
            Files.isDirectory(modulesDir.resolve("cloud_run_service")) shouldBe true
        }

        it("各モジュールに main.tf, variables.tf, outputs.tf がある") {
            listOf("artifact_registry", "cloud_sql_postgres", "cloud_run_service").forEach { mod ->
                val modDir = modulesDir.resolve(mod)
                Files.exists(modDir.resolve("main.tf")) shouldBe true
                Files.exists(modDir.resolve("variables.tf")) shouldBe true
                Files.exists(modDir.resolve("outputs.tf")) shouldBe true
            }
        }
    }

    describe("main.tf の内容") {
        val mainTf = Files.readString(terraformDir.resolve("main.tf"))

        it("GCS バックエンドを使用する") {
            mainTf shouldContain "backend \"gcs\""
        }

        it("Google プロバイダを使用する") {
            mainTf shouldContain "provider \"google\""
        }

        it("必要な GCP API を有効化する") {
            mainTf shouldContain "run.googleapis.com"
            mainTf shouldContain "sqladmin.googleapis.com"
            mainTf shouldContain "artifactregistry.googleapis.com"
            mainTf shouldContain "secretmanager.googleapis.com"
        }

        it("Cloud Run 用サービスアカウントを作成する") {
            mainTf shouldContain "google_service_account"
            mainTf shouldContain "roles/cloudsql.client"
        }

        it("Secret Manager でシークレットを管理する") {
            mainTf shouldContain "google_secret_manager_secret"
            mainTf shouldContain "db-password"
            mainTf shouldContain "jwt-secret"
            mainTf shouldContain "admin-password-hash"
        }

        it("README で指定された3つのモジュールを呼び出す") {
            mainTf shouldContain "module \"artifact_registry\""
            mainTf shouldContain "module \"cloud_sql\""
            mainTf shouldContain "module \"cloud_run\""
        }
    }

    describe("モジュールの内容") {
        it("artifact_registry モジュールが DOCKER format のリポジトリを作成する") {
            val content = Files.readString(modulesDir.resolve("artifact_registry/main.tf"))
            content shouldContain "google_artifact_registry_repository"
            content shouldContain "DOCKER"
        }

        it("cloud_sql_postgres モジュールが PostgreSQL インスタンスを作成する") {
            val content = Files.readString(modulesDir.resolve("cloud_sql_postgres/main.tf"))
            content shouldContain "google_sql_database_instance"
            content shouldContain "POSTGRES_"
            content shouldContain "google_sql_database"
            content shouldContain "google_sql_user"
        }

        it("cloud_run_service モジュールが Cloud Run v2 サービスを作成する") {
            val content = Files.readString(modulesDir.resolve("cloud_run_service/main.tf"))
            content shouldContain "google_cloud_run_v2_service"
            content shouldContain "run.invoker"
        }
    }

    describe("GitHub Actions CD 用リソース") {
        val mainTf = Files.readString(terraformDir.resolve("main.tf"))

        it("デプロイ用サービスアカウントを作成する") {
            mainTf shouldContain "github-actions-deploy"
            mainTf shouldContain "roles/run.admin"
            mainTf shouldContain "roles/artifactregistry.writer"
            mainTf shouldContain "roles/iam.serviceAccountUser"
        }

        it("Workload Identity Pool と Provider を作成する") {
            mainTf shouldContain "google_iam_workload_identity_pool"
            mainTf shouldContain "google_iam_workload_identity_pool_provider"
            mainTf shouldContain "token.actions.githubusercontent.com"
        }

        it("Workload Identity Federation の SA バインディングがある") {
            mainTf shouldContain "roles/iam.workloadIdentityUser"
        }

        it("IAM API を有効化する") {
            mainTf shouldContain "iam.googleapis.com"
            mainTf shouldContain "iamcredentials.googleapis.com"
        }
    }

    describe("CI ワークフローに Terraform 検証が含まれる") {
        val ciWorkflow = Files.readString(Path.of(".github", "workflows", "ci.yml"))

        it("terraform fmt と validate を実行する") {
            ciWorkflow shouldContain "terraform:"
            ciWorkflow shouldContain "terraform fmt"
            ciWorkflow shouldContain "terraform validate"
        }
    }

    describe("terraform.tfvars がコミットされない") {
        val gitignore = Files.readString(Path.of(".gitignore"))

        it(".gitignore に Terraform 関連の除外パターンがある") {
            gitignore shouldContain ".terraform/"
            gitignore shouldContain ".tfstate"
            gitignore shouldContain ".tfvars"
            gitignore shouldContain ".tfvars.example"
        }
    }
})
