package com.udacity.project4.locationreminders.reminderslist

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.firebase.ui.auth.AuthUI
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentRemindersBinding
import com.udacity.project4.locationreminders.ReminderDescriptionActivity
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled

import com.udacity.project4.utils.setTitle
import com.udacity.project4.utils.setup
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReminderListFragment : BaseFragment() {
    //use Koin to retrieve the ViewModel instance

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 456
        private const val FOREGROUND_AND_BACKGROUND_LOCATION_REQUEST_CODE = 798
    }

    override val _viewModel: RemindersListViewModel by viewModel()
    private lateinit var binding: FragmentRemindersBinding
    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_reminders, container, false
            )
        binding.viewModel = _viewModel

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(false)
        enableLocationPermission()
        setTitle(getString(R.string.app_name))

        binding.refreshLayout.setOnRefreshListener {
            _viewModel.loadReminders()
            binding.refreshLayout.isRefreshing = false
        }
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        setupRecyclerView()
        navigateToSignupScreenIfNotAuthenticated()
        observeViewModel()
        binding.addReminderFAB.setOnClickListener {
            navigateToAddReminder()
        }
    }

    private fun observeViewModel() {
        _viewModel.navigateToSingUp.observe(viewLifecycleOwner, Observer { navigate ->
            navigate?.let {
                if(it){
                    navigateToSingupScreen()
                }
            }
        })
    }

    private fun navigateToSignupScreenIfNotAuthenticated() {
        if (FirebaseAuth.getInstance().currentUser == null) {
            navigateToSingupScreen()
        }
    }

    private fun navigateToSingupScreen() {
        val intent = Intent(requireActivity(), AuthenticationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        //load the reminders list on the ui
        _viewModel.loadReminders()
    }

    private fun enableLocationPermission() {
        if (!locationPermissionGranted()) {
            requestLocationPermission()
        }
    }

    @TargetApi(29)
    private fun requestLocationPermission() {
        var requestCode = LOCATION_PERMISSION_REQUEST_CODE
        var permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (runningQOrLater) {
            permissions += Manifest.permission.ACCESS_BACKGROUND_LOCATION
            requestCode = FOREGROUND_AND_BACKGROUND_LOCATION_REQUEST_CODE
        }
        requestPermissions(permissions, requestCode)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults.isEmpty() || grantResults[0] == PackageManager.PERMISSION_DENIED || (requestCode == FOREGROUND_AND_BACKGROUND_LOCATION_REQUEST_CODE) &&
            grantResults[1] == PackageManager.PERMISSION_DENIED
        ) {
            Snackbar.make(
                binding.refreshLayout,
                getString(R.string.permission_denied_explanation),
                Snackbar.LENGTH_LONG
            )
                .setAction(getString(R.string.settings)) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        }
    }

    @TargetApi(29)
    private fun locationPermissionGranted(): Boolean {
        val foregroundPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (runningQOrLater) {
            val backgroundPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            return foregroundPermission && backgroundPermission
        }
        return foregroundPermission
    }

    private fun navigateToAddReminder() {
        //use the navigationCommand live data to navigate between the fragments
        _viewModel.navigationCommand.postValue(
            NavigationCommand.To(
                ReminderListFragmentDirections.toSaveReminder()
            )
        )
    }

    private fun setupRecyclerView() {
        val adapter = RemindersListAdapter {
            startReminderDetailsScreen(it)
        }

//        setup the recycler view using the extension function
        binding.reminderssRecyclerView.setup(adapter)
    }

    private fun startReminderDetailsScreen(reminderDataItem: ReminderDataItem) {
        startActivity(ReminderDescriptionActivity.newIntent(requireContext(), reminderDataItem))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout -> {
                logoutUser()
            }
        }
        return super.onOptionsItemSelected(item)

    }

    private fun logoutUser() {
        AuthUI.getInstance().signOut(requireActivity())
        _viewModel.onLogoutUser()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
//        display logout as menu item
        inflater.inflate(R.menu.main_menu, menu)
    }

}
