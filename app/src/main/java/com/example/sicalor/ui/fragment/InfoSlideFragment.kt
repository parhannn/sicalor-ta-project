package com.example.sicalor.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.sicalor.R
import com.example.sicalor.databinding.FragmentInfoSlideBinding
import com.example.sicalor.ui.MainActivity

class InfoSlideFragment : DialogFragment() {
    private lateinit var binding: FragmentInfoSlideBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.DialogTheme_Transparent)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInfoSlideBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backButton.setOnClickListener {
            val mainActivity = activity as MainActivity

            mainActivity.isClosed = true
            dismiss()
        }
    }
}