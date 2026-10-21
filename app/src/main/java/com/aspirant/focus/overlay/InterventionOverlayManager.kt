package com.aspirant.focus.overlay

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.aspirant.focus.MainActivity

class InterventionOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())

    fun showIntervention(title: String, message: String, isStrictLockout: Boolean = false) {
        handler.post {
            if (overlayView != null) {
                // Already showing, just update text
                overlayView?.findViewById<TextView>(ID_TITLE)?.text = title
                overlayView?.findViewById<TextView>(ID_MSG)?.text = message
                return@post
            }

            try {
                val view = createOverlayView(title, message, isStrictLockout)
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    else
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.CENTER
                }

                windowManager.addView(view, params)
                overlayView = view

                // Auto-dismiss after 6 seconds if not in strict lockout
                if (!isStrictLockout) {
                    handler.postDelayed({
                        dismissIntervention()
                    }, 6000)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun dismissIntervention() {
        handler.post {
            overlayView?.let {
                try {
                    windowManager.removeView(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                overlayView = null
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun createOverlayView(title: String, message: String, isStrictLockout: Boolean): View {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#E60D1117")) // 90% opaque dark background
            setPadding(64, 64, 64, 64)
        }

        // Warning Icon / Pill
        val badge = TextView(context).apply {
            text = if (isStrictLockout) "🔒 STRICT MODE LOCKED" else "🎯 ASPIRANT REALITY CHECK"
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            val bg = GradientDrawable().apply {
                setColor(if (isStrictLockout) Color.parseColor("#EF4444") else Color.parseColor("#3B82F6"))
                cornerRadius = 32f
            }
            background = bg
            setPadding(32, 12, 32, 12)
        }
        root.addView(badge)

        // Pulsing breathing circle
        val breathingCircle = View(context).apply {
            val size = 200
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                topMargin = 48
                bottomMargin = 48
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#3310B981"))
                setStroke(4, Color.parseColor("#10B981"))
            }
        }
        root.addView(breathingCircle)

        // Breathe animation
        val scaleX = ObjectAnimator.ofFloat(breathingCircle, "scaleX", 1.0f, 1.4f, 1.0f).apply {
            duration = 4000
            repeatCount = ValueAnimator.INFINITE
        }
        val scaleY = ObjectAnimator.ofFloat(breathingCircle, "scaleY", 1.0f, 1.4f, 1.0f).apply {
            duration = 4000
            repeatCount = ValueAnimator.INFINITE
        }
        scaleX.start()
        scaleY.start()

        // Title
        val titleView = TextView(context).apply {
            id = ID_TITLE
            text = title
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }
        root.addView(titleView)

        // Message
        val msgView = TextView(context).apply {
            id = ID_MSG
            text = message
            textSize = 15f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 48
            }
        }
        root.addView(msgView)

        // Action Button: Return to Study
        val button = Button(context).apply {
            text = "Back to Study"
            textSize = 16f
            setTextColor(Color.WHITE)
            val btnBg = GradientDrawable().apply {
                setColor(Color.parseColor("#10B981"))
                cornerRadius = 24f
            }
            background = btnBg
            setPadding(48, 24, 48, 24)
            setOnClickListener {
                dismissIntervention()
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(intent)
            }
        }
        root.addView(button)

        return root
    }

    companion object {
        private const val ID_TITLE = 1001
        private const val ID_MSG = 1002
    }
}
