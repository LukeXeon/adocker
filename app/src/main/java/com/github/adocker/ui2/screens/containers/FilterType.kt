package com.github.adocker.ui2.screens.containers

import androidx.annotation.StringRes
import com.github.adocker.R
import com.github.adocker.daemon.containers.ContainerState

enum class FilterType(
    @param:StringRes val labelResId: Int,
    val predicate: (ContainerState) -> Boolean
) {
    All(R.string.containers_tab_all, { true }),
    Created(R.string.containers_tab_created, { it is ContainerState.Created }),
    Running(R.string.containers_tab_running, { it is ContainerState.Running }),
    Exited(R.string.containers_tab_exited, { it is ContainerState.Exited })
}