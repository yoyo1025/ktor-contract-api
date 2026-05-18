package com.example.contract.ci

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files
import java.nio.file.Path

class CiWorkflowTest : DescribeSpec({
    val workflowPath = Path.of(".github", "workflows", "ci.yml")
    val workflow = Files.readString(workflowPath)

    describe("GitHub Actions CI workflow") {
        it("Pull Request をトリガーにする") {
            Files.exists(workflowPath) shouldBe true
            workflow shouldContain "name: CI"
            workflow shouldContain "pull_request:"
            workflow shouldContain "contents: read"
        }

        it("READMEで指定された検証タスクを実行する") {
            workflow shouldContain "test:"
            workflow shouldContain "detekt:"
            workflow shouldContain "ktlint:"
            workflow shouldContain "run: ./gradlew test"
            workflow shouldContain "run: ./gradlew detekt"
            workflow shouldContain "run: ./gradlew ktlintCheck"
        }

        it("CI の JVM バージョンをプロジェクト設定に合わせる") {
            workflow shouldContain "actions/setup-java@v4"
            workflow shouldContain "distribution: temurin"
            workflow shouldContain "java-version: '21'"
        }
    }
})
