package host.stjin.anonaddy

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import host.stjin.anonaddy.databinding.FragmentLoadingBinding
import host.stjin.anonaddy.ui.recipients.RecipientsFragment


class LoadingFragment : Fragment() {
    private var _binding: FragmentLoadingBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance() = RecipientsFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoadingBinding.inflate(inflater, container, false)
        val animated = context?.let { AnimatedVectorDrawableCompat.create(it, R.drawable.ic_loading_logo) }
        animated?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                binding.loadingFragmentIv.post { animated.start() }
            }

        })
        binding.loadingFragmentIv.setImageDrawable(animated)
        animated?.start()
        return binding.root
    }
}