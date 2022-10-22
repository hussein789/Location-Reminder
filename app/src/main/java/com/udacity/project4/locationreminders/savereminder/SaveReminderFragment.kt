package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.app.Activity.RESULT_OK
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
import androidx.fragment.app.Fragment
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
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment

    private lateinit var currentReminder:ReminderDataItem
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
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
        private const val LOCATION_PERMISSION_REQUEST_CODE = 456
        private const val FOREGROUND_AND_BACKGROUND_LOCATION_REQUEST_CODE = 798
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
                reminderData?.let {
                    currentReminder = it
                    enableLocationPermission()
                }
            })
        }
    }

    private fun enableLocationPermission() {
        if (locationPermissionGranted()) {
            checkDeviceLocationSettingsAndStartGeofence(currentReminder)
        } else {
            requestLocationPermission()
        }
    }

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
                binding.root,
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
        } else {
            enableLocationPermission()
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
                    startIntentSenderForResult(exception.resolution.intentSender,REQUEST_TURN_DEVICE_LOCATION_ON,null,0,0,0,null)
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
                        enableLocationPermission()
                    }
            }
        }
        locationCallback.addOnCompleteListener {
            if (it.isSuccessful) {
                addGeofenceForReminder(reminderDataItem)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_TURN_DEVICE_LOCATION_ON){
            if(resultCode == RESULT_OK){
                enableLocationPermission()
            }
        } else {
            Toast.makeText(requireActivity(),"Location must be on to create geofence",Toast.LENGTH_SHORT).show()
        }
    }

    private fun addGeofenceForReminder(reminderDataItem: ReminderDataItem) {
        val geofence = Geofence.Builder()
            .setRequestId(reminderDataItem.id)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setCircularRegion(
                reminderDataItem.latitude ?: 0.0,
                reminderDataItem.longitude ?: 0.0,
                GEOFENCE_CIRCULAR_RADIUS
            )
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
