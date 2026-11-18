package com.luminacamera.camera.activities

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.luminacamera.camera.BuildConfig
import com.luminacamera.camera.R
import com.luminacamera.camera.databinding.ActivitySettingsBinding
import com.luminacamera.camera.extensions.checkLocationPermission
import com.luminacamera.camera.extensions.config
import com.luminacamera.camera.helpers.AdMobManager
import com.luminacamera.camera.models.CaptureMode
import org.fossify.commons.dialogs.*
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.*
import org.fossify.commons.models.RadioItem
import java.util.*
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {
    private val binding by viewBinding(ActivitySettingsBinding::inflate)
    private var bannerAdView: AdView? = null
    private var isClosingWithAd = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.apply {
            setContentView(root)
            setupOptionsMenu()
            settingsBrandSubtitle.text = getString(R.string.lumina_settings_tagline, BuildConfig.VERSION_NAME)

            setupEdgeToEdge(padBottomSystem = listOf(adViewContainer))
            setupMaterialScrollListener(binding.settingsNestedScrollview, binding.settingsAppbar)

            adViewContainer.post {
                if (!isDestroyed && !isFinishing) {
                    bannerAdView = AdMobManager.loadAdaptiveBanner(this@SettingsActivity, adViewContainer)
                }
            }
        }
        AdMobManager.preloadInterstitial(this)
    }

    override fun onResume() {
        super.onResume()
        bannerAdView?.resume()
        setupTopAppBar(binding.settingsAppbar, NavigationIcon.Arrow)
        binding.settingsToolbar.setNavigationOnClickListener {
            finishWithInterstitial()
        }

        val midnightSurface = ContextCompat.getColor(this, R.color.lumina_midnight_surface)
        val obsidianBg = ContextCompat.getColor(this, R.color.lumina_obsidian)
        val primaryText = ContextCompat.getColor(this, R.color.lumina_text_primary)
        val cyanAccent = ContextCompat.getColor(this, R.color.lumina_cyan)

        binding.settingsCoordinator.setBackgroundColor(obsidianBg)
        binding.settingsAppbar.setBackgroundColor(midnightSurface)
        binding.settingsToolbar.setBackgroundColor(midnightSurface)
        binding.settingsToolbar.setTitleTextColor(primaryText)

        setupCustomizeColors()
        setupUseEnglish()
        setupLanguage()
        setupSound()
        setupVolumeButtonsAsShutter()
        setupMaxBrightness()
        setupFlipPhotos()
        setupSavePhotoMetadata()
        setupSavePhotoVideoLocation()
        setupSavePhotosFolder()
        setupPhotoQuality()
        setupCaptureMode()
        applyLuminaMidnightColors(primaryText, ContextCompat.getColor(this, R.color.lumina_text_secondary), cyanAccent)
    }

    private fun applyLuminaMidnightColors(primaryText: Int, secondaryText: Int, cyanAccent: Int) {
        binding.settingsToolbar.navigationIcon?.setTint(primaryText)
        binding.settingsToolbar.menu.findItem(R.id.about)?.icon?.setTint(cyanAccent)

        val switchThumbColors = android.content.res.ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
            intArrayOf(cyanAccent, android.graphics.Color.parseColor("#CBD5E1"))
        )
        val switchTrackColors = android.content.res.ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
            intArrayOf(android.graphics.Color.parseColor("#4D00E5FF"), android.graphics.Color.parseColor("#2A3554"))
        )

        fun styleTree(view: android.view.View) {
            when (view) {
                is androidx.appcompat.widget.SwitchCompat -> {
                    view.setTextColor(primaryText)
                    view.thumbTintList = switchThumbColors
                    view.trackTintList = switchTrackColors
                }
                is android.widget.CompoundButton -> view.setTextColor(primaryText)
                is android.widget.TextView -> view.setTextColor(primaryText)
            }
            if (view is android.view.ViewGroup) {
                for (i in 0 until view.childCount) {
                    styleTree(view.getChildAt(i))
                }
            }
        }

        styleTree(binding.settingsHolder)

        binding.apply {
            settingsBrandSubtitle.setTextColor(secondaryText)
            settingsLanguage.setTextColor(secondaryText)
            settingsSavePhotos.setTextColor(secondaryText)
            settingsPhotoQuality.setTextColor(secondaryText)
            settingsCaptureMode.setTextColor(secondaryText)

            arrayListOf(
                settingsColorCustomizationLabel,
                settingsGeneralSettingsLabel,
                settingsCameraLabel,
                settingsSavingLabel,
            ).forEach {
                it.setTextColor(cyanAccent)
            }
        }
    }

    override fun onPause() {
        bannerAdView?.pause()
        super.onPause()
    }

    override fun onDestroy() {
        bannerAdView?.destroy()
        bannerAdView = null
        super.onDestroy()
    }

    override fun onBackPressedCompat(): Boolean {
        finishWithInterstitial()
        return true
    }

    private fun finishWithInterstitial() {
        if (isClosingWithAd) return
        isClosingWithAd = true
        AdMobManager.maybeShowInterstitial(this) {
            if (!isFinishing && !isDestroyed) {
                finish()
            }
        }
    }

    private fun setupOptionsMenu() {
        binding.settingsToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.about -> launchAbout()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun setupCustomizeColors() {
        binding.settingsColorCustomizationHolder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupUseEnglish() = binding.apply {
        settingsUseEnglishHolder.beVisibleIf((config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus())
        settingsUseEnglish.isChecked = config.useEnglish
        settingsUseEnglishHolder.setOnClickListener {
            settingsUseEnglish.toggle()
            config.useEnglish = settingsUseEnglish.isChecked
            exitProcess(0)
        }
    }

    private fun setupLanguage() = binding.apply {
        settingsLanguage.text = Locale.getDefault().displayLanguage
        settingsLanguageHolder.beVisibleIf(isTiramisuPlus())

        listOf(settingsGeneralSettingsHolder, settingsGeneralSettingsLabel).forEach {
            it.beGoneIf(settingsUseEnglishHolder.isGone() && settingsLanguageHolder.isGone())
        }

        settingsLanguageHolder.setOnClickListener {
            launchChangeAppLanguageIntent()
        }
    }

    private fun launchAbout() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.app_name)
            .setIcon(R.mipmap.ic_launcher)
            .setMessage(getString(R.string.about_lumina_message, BuildConfig.VERSION_NAME))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun getLastPart(path: String): String {
        val humanized = humanizePath(path)
        return humanized.substringAfterLast("/", humanized)
    }

    private fun setupSound() = binding.apply {
        settingsSound.isChecked = config.isSoundEnabled
        settingsSoundHolder.setOnClickListener {
            settingsSound.toggle()
            config.isSoundEnabled = settingsSound.isChecked
        }
    }

    private fun setupVolumeButtonsAsShutter() = binding.apply {
        settingsVolumeButtonsAsShutter.isChecked = config.volumeButtonsAsShutter
        settingsVolumeButtonsAsShutterHolder.setOnClickListener {
            settingsVolumeButtonsAsShutter.toggle()
            config.volumeButtonsAsShutter = settingsVolumeButtonsAsShutter.isChecked
        }
    }

    private fun setupMaxBrightness() = binding.apply {
        settingsMaxBrightness.isChecked = config.maxBrightness
        settingsMaxBrightnessHolder.setOnClickListener {
            settingsMaxBrightness.toggle()
            config.maxBrightness = settingsMaxBrightness.isChecked
        }
    }

    private fun setupFlipPhotos() = binding.apply {
        settingsFlipPhotos.isChecked = config.flipPhotos
        settingsFlipPhotosHolder.setOnClickListener {
            settingsFlipPhotos.toggle()
            config.flipPhotos = settingsFlipPhotos.isChecked
        }
    }

    private fun setupSavePhotoMetadata() = binding.apply {
        settingsSavePhotoMetadata.isChecked = config.savePhotoMetadata
        settingsSavePhotoMetadataHolder.setOnClickListener {
            settingsSavePhotoMetadata.toggle()
            config.savePhotoMetadata = settingsSavePhotoMetadata.isChecked
        }
    }

    private fun setupSavePhotoVideoLocation() = binding.apply {
        settingsSavePhotoVideoLocation.isChecked = config.savePhotoVideoLocation
        settingsSavePhotoVideoLocationHolder.setOnClickListener {
            val willEnableSavePhotoVideoLocation = !config.savePhotoVideoLocation

            if (willEnableSavePhotoVideoLocation) {
                if (checkLocationPermission()) {
                    updateSavePhotoVideoLocationConfig(true)
                } else {
                    handlePermission(PERMISSION_ACCESS_FINE_LOCATION) { _ ->
                        if (checkLocationPermission()) {
                            updateSavePhotoVideoLocationConfig(true)
                        } else {
                            OpenDeviceSettingsDialog(
                                activity = this@SettingsActivity,
                                message = getString(org.fossify.commons.R.string.allow_location_permission)
                            )
                        }
                    }
                }
            } else {
                updateSavePhotoVideoLocationConfig(false)
            }
        }
    }

    private fun updateSavePhotoVideoLocationConfig(enabled: Boolean) {
        binding.settingsSavePhotoVideoLocation.isChecked = enabled
        config.savePhotoVideoLocation = enabled
    }

    private fun setupSavePhotosFolder() = binding.apply {
        settingsSavePhotosLabel.text = getString(R.string.save_photos)
        settingsSavePhotos.text = getLastPart(config.savePhotosFolder)
        settingsSavePhotosHolder.setOnClickListener {
            FilePickerDialog(
                this@SettingsActivity,
                config.savePhotosFolder,
                false,
                showFAB = true
            ) {
                val path = it
                handleSAFDialog(it) { success ->
                    if (success) {
                        config.savePhotosFolder = path
                        settingsSavePhotos.text = getLastPart(config.savePhotosFolder)
                    }
                }
            }
        }
    }

    private fun setupPhotoQuality() {
        updatePhotoQuality(config.photoQuality)
        binding.settingsPhotoQualityHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(100, "100%"),
                RadioItem(95, "95%"),
                RadioItem(90, "90%"),
                RadioItem(85, "85%"),
                RadioItem(80, "80%"),
                RadioItem(75, "75%"),
                RadioItem(70, "70%"),
                RadioItem(65, "65%"),
                RadioItem(60, "60%"),
                RadioItem(55, "55%"),
                RadioItem(50, "50%")
            )

            RadioGroupDialog(this@SettingsActivity, items, config.photoQuality) {
                config.photoQuality = it as Int
                updatePhotoQuality(it)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updatePhotoQuality(quality: Int) {
        binding.settingsPhotoQuality.text = "$quality%"
    }

    private fun setupCaptureMode() {
        updateCaptureMode(config.captureMode)
        binding.settingsCaptureModeHolder.setOnClickListener {
            val items = CaptureMode.values().mapIndexed { index, captureMode ->
                RadioItem(index, getString(captureMode.stringResId), captureMode)
            }

            RadioGroupDialog(this@SettingsActivity, ArrayList(items), config.captureMode.ordinal) {
                config.captureMode = it as CaptureMode
                updateCaptureMode(it)
            }
        }
    }

    private fun updateCaptureMode(captureMode: CaptureMode) {
        binding.settingsCaptureMode.text = getString(captureMode.stringResId)
    }
}
