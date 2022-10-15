package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.ReminderListFragment
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.TimeUnit

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment

    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
        internal const val ACTION_GEOFENCE_EVENT =
            "SaveReminderActivity.SaveReminder.action.ACTION_GEOFENCE_EVENT"
        val GEOFENCE_EXPIRATION_IN_MILLISECONDS: Long = TimeUnit.HOURS.toMillis(1)
        const val GEOFENCE_CIRCULAR_RADIUS = 100f
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            _viewModel.validateAndSaveReminder(
                ReminderDataItem(
                    title,
                    description,
                    location,
                    latitude,
                    longitude
                )
            )
//            TODO: use the user entered reminder details to:
//             1) add a geofencing request
//             2) save the reminder to the local db

            _viewModel.addGeofence.observe(viewLifecycleOwner, Observer { reminderData ->
                reminderData?.let { checkDeviceLocationSettingsAndStartGeofence(it) }
            })
        }
    }


    private fun checkDeviceLocationSettingsAndStartGeofence(reminderDataItem: ReminderDataItem) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val locationBuilder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val locationServices = LocationServices.getSettingsClient(requireContext())
        val locationCallback = locationServices.checkLocationSettings(locationBuilder.build())

        locationCallback.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (ex: Exception) {
                    Log.d("hussein", "error resolve device location due to ${ex.message}")
                }
            } else {
                Snackbar.make(
                    binding.root,
                    getString(R.string.location_required_error),
                    Snackbar.LENGTH_LONG
                )
                    .setAction(getString(android.R.string.ok)) {
                        checkDeviceLocationSettingsAndStartGeofence(reminderDataItem)
                    }
            }
        }
        locationCallback.addOnCompleteListener {
            if (it.isSuccessful) {
                addGeofenceForReminder(reminderDataItem)
            }
        }
    }

    private fun addGeofenceForReminder(reminderDataItem: ReminderDataItem) {
        val geofence = Geofence.Builder()
            .setRequestId(reminderDataItem.id)
            .setCircularRegion(
                reminderDataItem.latitude ?: 0.0,
                reminderDataItem.longitude ?: 0.0,
                GEOFENCE_CIRCULAR_RADIUS
            )
            .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofenceRequest = GeofencingRequest.Builder()
            .addGeofence(geofence)
            .setInitialTrigger(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        geofencingClient.addGeofences(geofenceRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                Log.d("hussein", "geofence added $it")
                _viewModel.navigationCommand.value = NavigationCommand.Back
            }

            addOnFailureListener {
                Toast.makeText(
                    requireContext(), getString(R.string.geofences_not_added),
                    Toast.LENGTH_SHORT
                ).show()
                _viewModel.navigationCommand.value = NavigationCommand.Back
                if (it.message != null)
                    Log.d("hussein", "geofence not added due to ${it.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
