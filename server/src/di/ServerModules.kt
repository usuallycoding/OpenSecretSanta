package com.usuallycoding.opensecretsanta.server.di

import com.usuallycoding.opensecretsanta.server.config.ServerConfig
import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.Migration
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import com.usuallycoding.opensecretsanta.server.presentation.HealthService
import org.koin.dsl.module

fun configModule(config: ServerConfig) = module {
    single { config }
}

fun databaseModule(migrations: List<Migration> = ClasspathMigrations.load()) = module {
    single { Database(get<ServerConfig>().dbPath) }
    single { MigrationRunner(get(), migrations) }
}

val presentationModule = module {
    single { HealthService(get()) }
}

fun serverModules(
    config: ServerConfig,
    migrations: List<Migration> = ClasspathMigrations.load(),
) = listOf(configModule(config), databaseModule(migrations), presentationModule)
