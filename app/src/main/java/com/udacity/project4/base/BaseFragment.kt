package com.udacity.project4.base

import android.content.Intent
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.authentication.AuthenticationActivity

/**
 * Base Fragment to observe on the common LiveData objects
 */
abstract class BaseFragment : Fragment() {
    /**
     * Every fragment has to have an instance of a view model that extends from the BaseViewModel
     */
    abstract val _viewModel: BaseViewModel

    override fun onStart() {
        super.onStart()
        _viewModel.showErrorMessage.observe(this, Observer {
            Toast.makeText(activity, it, Toast.LENGTH_LONG).show()
        })
        _viewModel.showToast.observe(this, Observer {
            Toast.makeText(activity, it, Toast.LENGTH_LONG).show()
        })
        _viewModel.showSnackBar.observe(this, Observer {
            Snackbar.make(requireView(), it, Snackbar.LENGTH_LONG).show()
        })
        _viewModel.showSnackBarInt.observe(this, Observer {
            Snackbar.make(requireView(), getString(it), Snackbar.LENGTH_LONG).show()
        })

        _viewModel.navigationCommand.observe(this, Observer { command ->
            when (command) {
                is NavigationCommand.To -> findNavController(this).navigate(command.directions)
                is NavigationCommand.Back -> findNavController(this).popBackStack()
                is NavigationCommand.BackTo -> findNavController(this).popBackStack(
                    command.destinationId,
                    false
                )
            }
        })
    }
}