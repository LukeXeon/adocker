package com.github.andock.ui.screens.search

import androidx.lifecycle.ViewModel
import com.github.andock.daemon.search.SearchClient
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchClient: SearchClient,
) : ViewModel() {

}