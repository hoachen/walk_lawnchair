package com.ur.apps.walk.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class LockScreenEmptyFragmentLeft : Fragment() {

    companion object {
        fun newInstance(): LockScreenEmptyFragmentLeft {
            return LockScreenEmptyFragmentLeft()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 返回一个空的View，作为左滑页面
        return View(requireContext())
    }
}
