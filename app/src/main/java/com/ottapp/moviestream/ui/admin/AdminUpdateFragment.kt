package com.ottapp.moviestream.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ottapp.moviestream.R
import com.ottapp.moviestream.data.model.UpdateConfig
import com.ottapp.moviestream.data.repository.UpdateRepository
import com.ottapp.moviestream.databinding.FragmentAdminUpdateBinding
import kotlinx.coroutines.launch

class AdminUpdateFragment : Fragment() {

    private var _binding: FragmentAdminUpdateBinding? = null
    private val binding get() = _binding!!
    private val repository = UpdateRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminUpdateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCurrentConfig()

        binding.btnSaveUpdate.setOnClickListener {
            saveConfig()
        }
    }

    private fun loadCurrentConfig() {
        lifecycleScope.launch {
            try {
                val config = repository.getUpdateConfig()
                config?.let {
                    binding.etVersionCode.setText(it.latestVersionCode.toString())
                    binding.etVersionName.setText(it.latestVersionName)
                    binding.etUpdateTitle.setText(it.updateTitle)
                    binding.etUpdateMessage.setText(it.updateMessage)
                    binding.etChangelog.setText(it.changelog.joinToString("\n"))
                    binding.etDownloadLink.setText(it.downloadLink)
                    if (it.updateType == "FORCE") {
                        binding.rbForce.isChecked = true
                    } else {
                        binding.rbSoft.isChecked = true
                    }
                    binding.switchEnabled.isChecked = it.isEnabled
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading config: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveConfig() {
        val versionCode = binding.etVersionCode.text.toString().toIntOrNull() ?: 0
        val versionName = binding.etVersionName.text.toString()
        val title = binding.etUpdateTitle.text.toString()
        val message = binding.etUpdateMessage.text.toString()
        val changelog = binding.etChangelog.text.toString().split("\n").filter { it.isNotBlank() }
        val downloadLink = binding.etDownloadLink.text.toString()
        val updateType = if (binding.rbForce.isChecked) "FORCE" else "SOFT"
        val isEnabled = binding.switchEnabled.isChecked

        if (versionName.isEmpty() || title.isEmpty() || downloadLink.isEmpty()) {
            Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val config = UpdateConfig(
            latestVersionCode = versionCode,
            latestVersionName = versionName,
            updateTitle = title,
            updateMessage = message,
            changelog = changelog,
            downloadLink = downloadLink,
            updateType = updateType,
            isEnabled = isEnabled
        )

        lifecycleScope.launch {
            try {
                repository.saveUpdateConfig(config)
                Toast.makeText(context, "Update configuration saved!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving config: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
