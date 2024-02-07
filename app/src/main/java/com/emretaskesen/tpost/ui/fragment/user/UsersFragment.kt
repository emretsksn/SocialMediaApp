package com.emretaskesen.tpost.ui.fragment.user

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.ui.adapter.UsersAdapter
import com.emretaskesen.tpost.databinding.FragmentUsersBinding
import com.emretaskesen.tpost.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// TODO: Rename parameter arguments, choose names that match
class UsersFragment : DialogFragment() {
    lateinit var binding: FragmentUsersBinding
    private val db =FirebaseFirestore.getInstance()
    private lateinit var userAdapter : UsersAdapter
    var userList = ArrayList<UserModel>()
    var auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater : LayoutInflater , container : ViewGroup? ,
        savedInstanceState : Bundle? ,
    ) : View {
        // ui bağlantısı
        binding = FragmentUsersBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view : View , savedInstanceState : Bundle?) {
        super.onViewCreated(view , savedInstanceState)
        initMenu()
        initViewsAndFunctions()
    }

    private fun initMenu() {
        val menuHost : MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu : Menu , menuInflater : MenuInflater) {
                menuInflater.inflate(R.menu.menu_userspage , menu)
                val searchItem = menu.findItem(R.id.search)
                val searchView = searchItem?.actionView as? SearchView
                searchView?.queryHint = getString(R.string.search)

                searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query : String?) : Boolean {
                        // Submit tuşuna basıldığında çağrılır
                        filterUsers(query)
                        return true
                    }

                    override fun onQueryTextChange(newText : String?) : Boolean {
                        // Kullanıcı yazarken çağrılır. Burada anlık arama yapabilirsiniz.
                        filterUsers(newText)
                        return true
                    }
                })

                searchItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                    override fun onMenuItemActionExpand(item : MenuItem) : Boolean {
                        return true
                    }

                    override fun onMenuItemActionCollapse(item : MenuItem) : Boolean {
                        getUsers()
                        return true
                    }
                })

            }

            override fun onMenuItemSelected(menuItem : MenuItem) : Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    android.R.id.home -> {
                        activity?.finish()
                        true
                    }

                    else -> false
                }
            }
        } , viewLifecycleOwner , Lifecycle.State.RESUMED)
    }

    private fun initViewsAndFunctions(){
        val layoutManager = LinearLayoutManager(requireActivity())
        binding.userDetail.layoutManager = layoutManager
        userAdapter = UsersAdapter(requireActivity() , userList)
        binding.userDetail.adapter = userAdapter
        setupToolbar()
        getUsers()
        requireActivity().onBackPressedDispatcher.addCallback(
            requireActivity() /* lifecycle owner */ ,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Back is pressed... Finishing the activity
                    requireActivity().finish()
                }
            })
    }

    private fun setupToolbar() {
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.userspageToolbar)
        val myActionBar : ActionBar? = (requireActivity() as AppCompatActivity).supportActionBar
        myActionBar !!.setDisplayHomeAsUpEnabled(true)
    }

    // Arama yapmak için bu metodu kullanın
    private fun filterUsers(query : String?) {
        if (! query.isNullOrEmpty()) {
            val filterUsername = userList.filter {
                it.userName?.contains(query) == true
            }
            val filterPersonalName = userList.filter {
                it.personalName?.contains(query) == true
            }
            binding.userDetail.visibility = View.VISIBLE
            userAdapter.filterList(filterUsername,filterPersonalName)
        }
        else {
            userAdapter.filterList(userList,userList)
            binding.userDetail.visibility = View.GONE
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getUsers() {
        val myID = auth.currentUser?.uid
        db.collection("Users").orderBy("userName" , Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot , error ->
                if (error != null) {
                    Toast.makeText(requireActivity(), error.localizedMessage, Toast.LENGTH_LONG).show()
                }
                else {
                    snapshot?.let {
                        userList.clear()
                        if (! it.isEmpty) {
                            val documents = snapshot.documents
                            for (userDoc in documents) {
                                val userID = userDoc.getString("userID") ?: ""
                                if (userID != myID) {
                                    val user = userDoc.toObject(UserModel::class.java)
                                    if (user!=null){
                                        userList.add(user)
                                    }
                                }
                                userAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
    }
}