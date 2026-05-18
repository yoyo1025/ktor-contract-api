package com.example.contract.ci

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files
import java.nio.file.Path

class CdWorkflowTest : DescribeSpec({
    val workflowPath = Path.of(".github", "workflows", "deploy.yml")
    val workflow = Files.readString(workflowPath)

    describe("GitHub Actions CD workflow") {
        it("deploy.yml が存在する") {
            Files.exists(workflowPath) shouldBe true
        }

        it("main ブランチへの push をトリガーにする") {
            workflow shouldContain "push:"
            workflow shouldContain "branches: [main]"
        }

        it("OIDC 用の id-token: write パーミッションを持つ") {
            workflow shouldContain "id-token: write"
            workflow shouldContain "contents: read"
        }
    }

    describe("test ジョブ") {
        it("デプロイ前にテストを実行する") {
            workflow shouldContain "run: ./gradlew test"
        }

        it("JDK 21 を使用する") {
            workflow shouldContain "java-version: '21'"
        }
    }

    describe("deploy ジョブ") {
        it("test ジョブに依存する") {
            workflow shouldContain "needs: test"
        }

        it("Workload Identity Federation で GCP 認証する") {
            workflow shouldContain "google-github-actions/auth@v2"
            workflow shouldContain "workload_identity_provider:"
            workflow shouldContain "service_account:"
        }

        it("Artifact Registry に Docker イメージを push する") {
            workflow shouldContain "docker build"
            workflow shouldContain "docker push"
            workflow shouldContain "docker.pkg.dev"
        }

        it("Cloud Run にデプロイする") {
            workflow shouldContain "google-github-actions/deploy-cloudrun@v2"
        }

        it("コミット SHA をイメージタグに使用する") {
            workflow shouldContain "github.sha"
        }
    }
})
