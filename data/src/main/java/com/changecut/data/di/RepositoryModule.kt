package com.changecut.data.di

import com.changecut.data.repository.ProjectRepositoryImpl
import com.changecut.domain.repository.ProjectRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository
}
