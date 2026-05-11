package com.dreamtravel.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SearchModule
// GeocodeSearch 不再作为单例提供，各调用方自行创建实例以避免并发 listener 竞态
// 隐私合规在 DreamTravelApp.onCreate() 中 super.onCreate() 之前统一设置
