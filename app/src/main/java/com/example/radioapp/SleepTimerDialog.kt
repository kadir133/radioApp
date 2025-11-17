package com.example.radioapp

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout

class SleepTimerDialog(
    context: Context,
    private val onTimerSelected: (Int) -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createDialogLayout())
        setTitle("Uyku Zamanlayıcı")
    }

    private fun createDialogLayout(): LinearLayout {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val times = listOf(
            15 to "15 Dakika",
            30 to "30 Dakika",
            45 to "45 Dakika",
            60 to "60 Dakika"
        )

        times.forEach { (minutes, label) ->
            val button = Button(context).apply {
                text = label
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16)
                }
                setOnClickListener {
                    onTimerSelected(minutes)
                    dismiss()
                }
            }
            layout.addView(button)
        }

        val cancelButton = Button(context).apply {
            text = "İptal Et"
            setOnClickListener {
                onTimerSelected(0) // 0 = timer iptal
                dismiss()
            }
        }
        layout.addView(cancelButton)

        return layout
    }
}