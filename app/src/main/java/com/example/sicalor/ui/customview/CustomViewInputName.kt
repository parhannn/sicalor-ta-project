package com.example.sicalor.ui.customview

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.example.sicalor.R
import java.util.regex.Pattern

class CustomViewInputName : AppCompatEditText, View.OnFocusChangeListener {
    private var isValid = false

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, styleAttr: Int) : super(
        context,
        attrs,
        styleAttr
    ) {
        init()
    }

    private fun init() {
        inputType = InputType.TYPE_CLASS_TEXT
        background = ContextCompat.getDrawable(context, R.drawable.custom_border)
        onFocusChangeListener = this
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateNameInput()
            }
            override fun afterTextChanged(s: Editable?) {
            }
        })
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        if (!hasFocus) {
            validateNameInput()
        }
    }

    private fun validateNameInput() {
        val pattern = "^[a-zA-Z\\s]+$"
        isValid = Pattern.matches(pattern, text.toString())

        error = if (!isValid) {
            resources.getString(R.string.wrong_format_name)
        } else {
            null
        }
    }
}