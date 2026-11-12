
package com.eniola.smartstudyplanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.content.Intent
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import com.google.firebase.Timestamp

/**ProfileFragment displays the student's personal information, academic
  details, study preferences, and task statistics. It also manages profile
  pictures, profile editing, and navigation to application settings. */
class ProfileFragment : Fragment() {

    // Profile information views.
    private lateinit var imgProfile: ImageView
    private lateinit var txtProfileName: TextView
    private lateinit var txtProfileEmail: TextView
    private lateinit var txtProfileDepartment: TextView
    private lateinit var txtProfileLevel: TextView
    private lateinit var txtPreferredStudyTime: TextView
    private lateinit var txtAvailableStudyHours: TextView
    private lateinit var txtCompletedTasks: TextView
    private lateinit var txtPendingTasks: TextView
    private lateinit var txtMissedTasks: TextView

    // Profile picture button.
    private lateinit var btnChangeProfilePicture: ImageButton

    // Provides access to the signed-in student's Firebase account.
    private val auth = FirebaseAuth.getInstance()

    // Provides access to the student's profile and task records.
    private val firestore = FirebaseFirestore.getInstance()

    // Used to open the gallery.
    private val galleryLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            uri?.let {
                saveGalleryImageLocally(it)
            }
        }

    // Used to open the camera.
    private val cameraLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicturePreview()
        ) { bitmap: Bitmap? ->

            bitmap?.let {
                saveCameraImageLocally(it)
            }
        }

    // Request camera permission.
    private val requestCameraPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                cameraLauncher.launch(null)
            } else {

                Toast.makeText(
                    requireContext(),
                    "Camera permission is required.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    /** Initializes the profile interface and handles profile-related navigation. */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_profile,
            container,
            false
        )

        // Connect profile picture views.
        imgProfile =
            view.findViewById(R.id.imgProfile)

        btnChangeProfilePicture =
            view.findViewById(
                R.id.btnChangeProfilePicture
            )

        // Connect profile information views.
        txtProfileName =
            view.findViewById(R.id.txtProfileName)

        txtProfileEmail =
            view.findViewById(R.id.txtProfileEmail)

        txtProfileDepartment =
            view.findViewById(R.id.txtProfileDepartment)

        txtProfileLevel =
            view.findViewById(R.id.txtProfileLevel)

        txtPreferredStudyTime =
            view.findViewById(
                R.id.txtPreferredStudyTime
            )

        txtAvailableStudyHours =
            view.findViewById(
                R.id.txtAvailableStudyHours
            )

        // Connect study statistics views.
        txtCompletedTasks =
            view.findViewById(R.id.txtCompletedTasks)

        txtPendingTasks =
            view.findViewById(R.id.txtPendingTasks)

        txtMissedTasks =
            view.findViewById(R.id.txtMissedTasks)

        // Open the profile picture options.
        btnChangeProfilePicture.setOnClickListener {
            showProfilePictureBottomSheet()
        }

        // Open Edit Profile.
        val btnEditProfile =
            view.findViewById<MaterialButton>(
                R.id.btnEditProfile
            )

        btnEditProfile.setOnClickListener {

            startActivity(
                android.content.Intent(
                    requireContext(),
                    EditProfileActivity::class.java
                )
            )
        }

        // Open the Settings screen.
        val imgProfileSettings =
            view.findViewById<ImageView>(R.id.imgProfileSettings)

        imgProfileSettings.setOnClickListener {

            startActivity(
                Intent(
                    requireContext(),
                    SettingsActivity::class.java
                )
            )
        }

        return view
    }

    /** Refreshes profile information, profile picture, and task statistics. */
    override fun onResume() {
        super.onResume()

        // Reload profile information.
        loadProfile()

        // Reload the locally saved profile picture.
        loadLocalProfilePicture()

        // Reload all-time study statistics.
        loadStudyStatistics()
    }

    /** Displays the bottom sheet containing the profile picture options */
    private fun showProfilePictureBottomSheet() {

        val bottomSheet =
            BottomSheetDialog(requireContext())

        val sheetView =
            layoutInflater.inflate(
                R.layout.bottom_sheet_profile_picture,
                null
            )
        bottomSheet.setContentView(sheetView)

        val btnTakePicture =
            sheetView.findViewById<MaterialButton>(
                R.id.btnTakePicture
            )

        val btnChooseGallery =
            sheetView.findViewById<MaterialButton>(
                R.id.btnChooseGallery
            )

        val btnCancel =
            sheetView.findViewById<MaterialButton>(
                R.id.btnCancelPicture
            )

        // Open camera.
        btnTakePicture.setOnClickListener {
            bottomSheet.dismiss()
            openCamera()
        }

        // Open gallery.
        btnChooseGallery.setOnClickListener {
            bottomSheet.dismiss()
            openGallery()
        }

        // Close bottom sheet.
        btnCancel.setOnClickListener {
            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }

    /**Opens the device camera.*/
    private fun openCamera() {
        // Check camera permission before launching the camera.
        if (
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            cameraLauncher.launch(null)

        } else {

            requestCameraPermission.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    /**Opens the device gallery.*/
    private fun openGallery() {

        galleryLauncher.launch("image/*")
    }

    /** Saves a gallery image inside the app's private storage on the student's device.*/
    private fun saveGalleryImageLocally(uri: Uri) {
        try {
            val inputStream =
                requireContext()
                    .contentResolver
                    .openInputStream(uri)

            if (inputStream == null) {
                showPictureError()
                return
            }

            // Create the local file used to store the selected image.
            val file =
                File(
                    requireContext().filesDir,
                    "profile_picture.jpg"
                )

            val outputStream =
                FileOutputStream(file)

            // Copy the selected image into the application's private storage.
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            // Display the picture immediately.
            Glide.with(this)
                .load(file)
                .skipMemoryCache(true)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .placeholder(R.drawable.ic_profile)
                .into(imgProfile)

            Toast.makeText(
                requireContext(),
                "Profile picture updated.",
                Toast.LENGTH_SHORT
            ).show()

        } catch (exception: Exception) {

            showPictureError()
        }
    }

    /**Saves a picture taken with the camera inside the app's private storage.*/
    private fun saveCameraImageLocally(bitmap: Bitmap) {
        try {
            val file =
                File(
                    requireContext().filesDir,
                    "profile_picture.jpg"
                )

            val outputStream =
                FileOutputStream(file)

            // Compress the captured image into JPEG format before saving.
            outputStream.use { output ->

                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    90,
                    output
                )
            }
            // Display the picture immediately.
            Glide.with(this)
                .load(file)
                .skipMemoryCache(true)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .placeholder(R.drawable.ic_profile)
                .into(imgProfile)

            Toast.makeText(
                requireContext(),
                "Profile picture updated.",
                Toast.LENGTH_SHORT
            ).show()

        } catch (exception: Exception) {

            showPictureError()
        }
    }

    //Loads the profile picture saved onthe student's device.
    private fun loadLocalProfilePicture() {

        val file =
            File(
                requireContext().filesDir,
                "profile_picture.jpg"
            )

        if (file.exists()) {

            Glide.with(this)
                .load(file)
                .skipMemoryCache(true)
                .diskCacheStrategy(
                    com.bumptech.glide.load.engine.DiskCacheStrategy.NONE
                )
                .placeholder(R.drawable.ic_profile)
                .into(imgProfile)
        }
    }

    // Displays a message if the picture could not be saved.
    private fun showPictureError() {

        if (!isAdded) return

        Toast.makeText(
            requireContext(),
            "Failed to save profile picture.",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**Loads the student's profile informationfrom Firestore. */
    private fun loadProfile() {
        val user = auth.currentUser
        if (user == null) {
            return
        }

        // Retrieve the student's profile using their Firebase user ID.
        firestore
            .collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->

                if (!isAdded) {
                    return@addOnSuccessListener
                }

                if (!document.exists()) {
                    return@addOnSuccessListener
                }

                // Basic profile information.
                txtProfileName.text =
                    document.getString("fullName")
                        ?: "Not set"

                txtProfileEmail.text =
                    document.getString("email")
                        ?: user.email
                                ?: "Not set"

                // Academic information.
                txtProfileDepartment.text =
                    document.getString("department")
                        ?: "Not set"

                txtProfileLevel.text =
                    document.getString("level")
                        ?: "Not set"

                // Study preferences.
                txtPreferredStudyTime.text =
                    document.getString("preferredStudyTime")
                        ?: "Not set"

                txtAvailableStudyHours.text =
                    document.getString("availableStudyHours")
                        ?: "Not set"
            }
            .addOnFailureListener {

                if (!isAdded) return@addOnFailureListener

                Toast.makeText(
                    requireContext(),
                    "Failed to load profile",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    /**Loads all-time study statistics for the currently logged-in student. */
    private fun loadStudyStatistics() {
        val user = auth.currentUser
        if (user == null) {
            return
        }

        // Retrieve the student's tasks to calculate completion statistics.
        firestore
            .collection("users")
            .document(user.uid)
            .collection("tasks")
            .get()
            .addOnSuccessListener { documents ->

                if (!isAdded) {
                    return@addOnSuccessListener
                }

                var completedTasks = 0
                var pendingTasks = 0
                var missedTasks = 0

                val currentTime = Timestamp.now()

                // Categorize each task based on completion status and deadline.
                for (document in documents) {

                    val completed =
                        document.getBoolean("completed")
                            ?: false

                    val deadline =
                        document.getTimestamp("deadline")

                    if (completed) {

                        // The task has been completed.
                        completedTasks++

                    } else if (
                        deadline != null &&
                        deadline.toDate().before(currentTime.toDate())
                    ) {

                        // The deadline has passed and the task was not completed.
                        missedTasks++

                    } else {
                        // The task is still pending.
                        pendingTasks++
                    }
                }

                // Update the statistics displayed on the Profile page.
                txtCompletedTasks.text = "Completed Tasks: $completedTasks"
                txtPendingTasks.text = "Pending Tasks: $pendingTasks"
                txtMissedTasks.text = "Missed Tasks: $missedTasks"
            }
            .addOnFailureListener {

                if (!isAdded) {
                    return@addOnFailureListener
                }

                Toast.makeText(
                    requireContext(),
                    "Failed to load study statistics",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}
