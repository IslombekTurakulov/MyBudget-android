package ru.iuturakulov.mybudget.ui.projects

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.iuturakulov.mybudget.data.local.entities.ProjectEntity
import ru.iuturakulov.mybudget.domain.repositories.ProjectRepository
import javax.inject.Inject

@HiltViewModel
class ProjectListViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _projects = MutableLiveData<List<ProjectEntity>>()
    val projects: LiveData<List<ProjectEntity>> = _projects

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadProjects()
    }

    private fun loadProjects() {
        viewModelScope.launch {
            try {
                val projectList = projectRepository.getProjects()
                _projects.postValue(projectList)
            } catch (e: Exception) {
                _errorMessage.postValue("Ошибка загрузки проектов: ${e.localizedMessage}")
            }
        }
    }
}