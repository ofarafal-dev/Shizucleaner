package dev.ofarafal.enklawa.ui

import android.graphics.Color as AColor
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Pole tekstowe jako EditText, bo tylko tak można ustawić IME_FLAG_NO_PERSONALIZED_LEARNING
 * (klawiatura ma się nie uczyć z wpisywanego tekstu). To prośba do klawiatury, nie gwarancja.
 */
@Composable
fun PrivateEditText(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    hint: String = "",
    password: Boolean = false,
) {
    val changed = rememberUpdatedState(onTextChange)
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            EditText(ctx).apply {
                inputType = if (password) {
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                } else if (singleLine) {
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                } else {
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                }
                imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING or
                    (if (singleLine) EditorInfo.IME_ACTION_DONE else 0)
                if (singleLine || password) setSingleLine(true) else setHorizontallyScrolling(false)
                gravity = if (singleLine) Gravity.CENTER_VERTICAL else (Gravity.TOP or Gravity.START)
                this.hint = hint
                setTextColor(AColor.BLACK)
                setHintTextColor(AColor.GRAY)
                textSize = 15f
                setPadding(20, 16, 20, 16)
                background = GradientDrawable().apply {
                    cornerRadius = 10f
                    setColor(AColor.WHITE)
                    setStroke(2, 0xFF7F9DB9.toInt())
                }
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = Unit
                    override fun afterTextChanged(s: Editable?) {
                        changed.value(s?.toString().orEmpty())
                    }
                })
            }
        },
        update = { if (it.text.toString() != text) it.setText(text) },
    )
}
