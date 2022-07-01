package host.stjin.anonaddy.watchface

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.SurfaceHolder
import android.view.WindowInsets
import androidx.core.content.ContextCompat
import host.stjin.anonaddy.R
import java.lang.ref.WeakReference
import java.util.*
import kotlin.random.Random

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 *
 *
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
class AnonAddyWatchFace : CanvasWatchFaceService() {


    companion object {
        private val NORMAL_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, 500, false)

        /**
         * Updates rate in milliseconds for interactive mode. We update once a second since seconds
         * are displayed in interactive mode.
         */
        private const val INTERACTIVE_UPDATE_RATE_MS = 1000

        /**
         * Handler message id for updating the time periodically in interactive mode.
         */
        private const val MSG_UPDATE_TIME = 0
    }

    override fun onCreateEngine(): Engine {
        return Engine()
    }

    private class EngineHandler(reference: Engine) : Handler() {
        private val mWeakReference: WeakReference<Engine> = WeakReference(reference)

        override fun handleMessage(msg: Message) {
            val engine = mWeakReference.get()
            if (engine != null) {
                when (msg.what) {
                    MSG_UPDATE_TIME -> engine.handleUpdateTimeMessage()
                }
            }
        }
    }

    inner class Engine : CanvasWatchFaceService.Engine() {

        private var mOffsetSpacing: Float = 64f

        private lateinit var mCalendar: Calendar

        private var mRegisteredTimeZoneReceiver = false


        private lateinit var mTextPaint: Paint
        private lateinit var mDummyTextPaint: Paint

        // The -1 minute does not exist, that means that the first dummyApiKey generation will always happen
        private var lastUpdateMinute = -1

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        private var mLowBitAmbient: Boolean = false
        private var mBurnInProtection: Boolean = false
        private var mAmbient: Boolean = false

        private val mUpdateTimeHandler: Handler = EngineHandler(this)

        private val mTimeZoneReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            setWatchFaceStyle(
                WatchFaceStyle.Builder(this@AnonAddyWatchFace)
                    .setAcceptsTapEvents(true)
                    .build()
            )

            mCalendar = Calendar.getInstance()


            // Initializes Watch Face.
            mTextPaint = Paint().apply {
                typeface = NORMAL_TYPEFACE
                isAntiAlias = true
                color = ContextCompat.getColor(applicationContext, R.color.digital_text)
            }
            // Initializes Watch Face.
            mDummyTextPaint = Paint().apply {
                typeface = NORMAL_TYPEFACE
                isAntiAlias = true
                color = ContextCompat.getColor(applicationContext, R.color.digital_text_dummy)
            }
        }

        override fun onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            super.onDestroy()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)
            mLowBitAmbient = properties.getBoolean(
                WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false
            )
            mBurnInProtection = properties.getBoolean(
                WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false
            )
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            mAmbient = inAmbientMode

            if (mLowBitAmbient) {
                mTextPaint.isAntiAlias = !inAmbientMode
                mDummyTextPaint.isAntiAlias = !inAmbientMode
            }

            // Whether the timer should be running depends on whether we"re visible (as well as
            // whether we"re in ambient mode), so we may need to start or stop the timer.
            updateTimer()
        }
/*
        */
        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         *//*
        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {
            when (tapType) {
                WatchFaceService.TAP_TYPE_TOUCH -> {
                    // The user has started touching the screen.
                }
                WatchFaceService.TAP_TYPE_TOUCH_CANCEL -> {
                    // The user has started a different gesture or otherwise cancelled the tap.
                }
                WatchFaceService.TAP_TYPE_TAP ->
                    // The user has completed the tap gesture.
            }
            invalidate()
        }*/

        // 10 lines of characters should be enough for all kind of watches
        private var dummyKeys = Array(10) { "" }
        private fun generateDummyAPIKey(): Array<String> {
            if (lastUpdateMinute != mCalendar.get(Calendar.MINUTE)) {
                // If the minute is different than last minute, change the values of the dummyKeysArray
                val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
                for (i in 0..9) {
                    // 20 character should be enough for all kinds of watches
                    val randomString = (1..20)
                        .map { Random.nextInt(0, chars.count()) }
                        .map(chars::get)
                        .joinToString("")
                    dummyKeys[i] = randomString
                }

                // Set the last update minute to make sure that the array won't be changed within the next minute
                lastUpdateMinute = mCalendar.get(Calendar.MINUTE)
            }

            return dummyKeys
        }


        override fun onDraw(canvas: Canvas, bounds: Rect) {

            canvas.drawColor(Color.BLACK)
            val now = System.currentTimeMillis()
            mCalendar.timeInMillis = now
            val hour = if (DateFormat.is24HourFormat(this@AnonAddyWatchFace)) mCalendar.get(Calendar.HOUR_OF_DAY) else mCalendar.get(Calendar.HOUR)

            var hMarker = ""
            val time = String.format(
                "%02d%02d", hour,
                mCalendar.get(Calendar.MINUTE)
            )


            if (!DateFormat.is24HourFormat(this@AnonAddyWatchFace)) {
                val mHour = mCalendar.get(Calendar.HOUR_OF_DAY)
                val hourOfDay: Int = mHour
                hMarker = if (hourOfDay >= 12) {
                    "PM"
                } else {
                    "AM"
                }
            }


            val mPaint = Paint()
            mPaint.isAntiAlias = mTextPaint.isAntiAlias
            mPaint.textSize = mTextPaint.textSize
            mPaint.typeface = NORMAL_TYPEFACE

            // If the system is in 24h format, the AM/PM is not included. This means that an extra character can be
            // added before the time to align the text a bit more to the right
            val dummyApiKey1 = if (DateFormat.is24HourFormat(this@AnonAddyWatchFace)) {
                dummyKeys[0].take(3)
            } else {
                // If the system is in 12h format, the AM/PM is included. This means that we do not insert an extra character
                // before the time to make sure the AM/PM marker fits on screen
                dummyKeys[0].take(2)
            }

            generateDummyAPIKey()
            val dummyApiKey2 = hMarker + dummyKeys[1].take(6)
            val dummyTextWidth = mPaint.measureText(dummyApiKey1)
            val timeTextPost = mPaint.measureText(time)


            var myMYOffset = -20f
            // For loop
            for (i in 2..9) {

                if (i == 5) {
                    if (!mAmbient) canvas.drawText(dummyApiKey1, 0f, myMYOffset, mDummyTextPaint)
                    canvas.drawText(time, dummyTextWidth, myMYOffset, mTextPaint)
                    if (!mAmbient) canvas.drawText(dummyApiKey2, timeTextPost + dummyTextWidth, myMYOffset, mDummyTextPaint)
                } else {
                    if (!mAmbient) canvas.drawText(dummyKeys[i], 0f, myMYOffset, mDummyTextPaint)
                }

                myMYOffset += mOffsetSpacing
            }


        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()

                // Update time zone in case it changed while we weren't visible.
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            } else {
                unregisterReceiver()
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer()
        }

        private fun registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            this@AnonAddyWatchFace.registerReceiver(mTimeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = false
            this@AnonAddyWatchFace.unregisterReceiver(mTimeZoneReceiver)
        }

        override fun onApplyWindowInsets(insets: WindowInsets) {
            super.onApplyWindowInsets(insets)
            val typedValue = TypedValue()

            val resources = this@AnonAddyWatchFace.resources
            resources.getValue(
                R.dimen.digital_x_offset, typedValue, true
            )
            mOffsetSpacing = typedValue.float

            val textSize = resources.getDimension(
                R.dimen.digital_text_size
            )

            mTextPaint.textSize = textSize
            mDummyTextPaint.textSize = textSize
        }

        /**
         * Starts the [.mUpdateTimeHandler] timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private fun updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME)
            }
        }

        /**
         * Returns whether the [.mUpdateTimeHandler] timer should be running. The timer should
         * only run when we"re visible and in interactive mode.
         */
        private fun shouldTimerBeRunning(): Boolean {
            return isVisible && !isInAmbientMode
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        fun handleUpdateTimeMessage() {
            invalidate()
            if (shouldTimerBeRunning()) {
                val timeMs = System.currentTimeMillis()
                val delayMs = INTERACTIVE_UPDATE_RATE_MS - timeMs % INTERACTIVE_UPDATE_RATE_MS
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs)
            }
        }
    }
}