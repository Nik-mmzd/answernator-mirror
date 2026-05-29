package pw.modder.answernator4.di

import org.kodein.di.DI

interface KodeinModuleProvider {
    val module: DI.Module
    val name get() = module.name
    val version: String
}
