package vdo.ai.android.core.base

import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

/**
 *  created by Ashish Saini at 4th Oct 2023
 *
 **/

typealias Inflate<T> = (LayoutInflater, ViewGroup?, Boolean) -> T

abstract class VdoBaseFragment<VB: ViewBinding>(private val inflate: Inflate<VB>) : Fragment(){

    private var _binding: VB? = null
    val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = inflate.invoke(inflater, container, false)
        return binding.root
    }
}