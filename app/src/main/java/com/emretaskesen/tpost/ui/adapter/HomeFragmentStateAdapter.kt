package com.emretaskesen.tpost.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

// Wiewpager için fragment adaptörü
class HomeFragmentStateAdapter(
    fragmentManager : FragmentManager ,
    lifecycle : Lifecycle ,
    private val fragmentList : List<Fragment> ,
) : FragmentStateAdapter(fragmentManager , lifecycle) {

    // Fragment sayısını hesaplayın
    override fun getItemCount() : Int {
        return fragmentList.size
    }

    // Fragmentler oluşturun
    override fun createFragment(position : Int) : Fragment {
        return fragmentList[position]
    }
}