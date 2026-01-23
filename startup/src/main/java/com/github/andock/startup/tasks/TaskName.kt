package com.github.andock.startup.tasks

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class TaskName(val value: String = "")