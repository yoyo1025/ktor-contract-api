package com.example.contract.infrastructure

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.copyTo

class FlywayFilesystemMigrationTest : DescribeSpec({
    val postgres =
        PostgreSQLContainer("postgres:15-alpine").apply {
            withDatabaseName("contract_test")
            withUsername("testuser")
            withPassword("testpass")
        }

    beforeSpec {
        postgres.start()
    }

    afterSpec {
        postgres.stop()
    }

    describe("Flyway filesystem migration") {
        it("should apply migrations from filesystem location") {
            val tempDir = Files.createTempDirectory("flyway-test")
            try {
                val resourceDir = Path.of("src/main/resources/db/migration")
                resourceDir.toFile().listFiles()?.forEach { file ->
                    file.toPath().copyTo(tempDir.resolve(file.name))
                }

                val flyway =
                    Flyway.configure()
                        .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
                        .locations("filesystem:${tempDir.toAbsolutePath()}")
                        .load()

                val result = flyway.migrate()

                result.success shouldBe true
                result.migrationsExecuted shouldBe 3
            } finally {
                tempDir.toFile().deleteRecursively()
            }
        }
    }
})
