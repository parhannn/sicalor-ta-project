package com.example.sicalor.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.sicalor.R
import com.example.sicalor.databinding.FragmentInfoScanBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class InfoScanFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentInfoScanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentInfoScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}