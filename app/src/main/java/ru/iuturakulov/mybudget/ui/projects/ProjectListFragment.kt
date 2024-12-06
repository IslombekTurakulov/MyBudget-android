package ru.iuturakulov.mybudget.ui.projects

import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import ru.iuturakulov.mybudget.R
import ru.iuturakulov.mybudget.databinding.FragmentProjectListBinding
import ru.iuturakulov.mybudget.ui.BaseFragment

@AndroidEntryPoint
class ProjectListFragment :
    BaseFragment<FragmentProjectListBinding>(R.layout.fragment_project_list) {

    private val viewModel: ProjectListViewModel by viewModels()

    override fun getViewBinding(view: View): FragmentProjectListBinding {
        return FragmentProjectListBinding.bind(view)
    }

    override fun setupViews() {
//        adapter = ProjectAdapter { project ->
//            val action = ProjectListFragmentDirections.actionProjectsToDetails(project.id)
////            findNavController().navigate(action)
//        }

//        binding.recyclerViewProjects.adapter = adapter

        binding.fabAddProject.setOnClickListener {
//            findNavController().navigate(R.id.action_projects_to_create)
        }
    }

    override fun setupObservers() {
        viewModel.projects.observe(viewLifecycleOwner) { projects ->
//            adapter.submitList(projects)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }
    }
}