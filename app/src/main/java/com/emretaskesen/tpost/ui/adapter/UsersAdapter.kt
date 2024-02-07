package com.emretaskesen.tpost.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.cache.UserCache
import com.emretaskesen.tpost.model.UserModel
import com.emretaskesen.tpost.ui.activity.user.UserProfile
import com.google.android.material.textview.MaterialTextView
import de.hdodenhof.circleimageview.CircleImageView

class UsersAdapter(
    val context: Context,
    var userList: ArrayList<UserModel>
) :
    RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    // Filtreleme işlemi için kullanıcı listesini güncelleyen fonksiyon
    @SuppressLint("NotifyDataSetChanged")
    fun filterList(username: List<UserModel> , personalname: List<UserModel>) {
        if (username.isNotEmpty()) {
            this.userList = ArrayList(username)
        } else if (personalname.isNotEmpty()) {
            this.userList = ArrayList(personalname)
        }
        notifyDataSetChanged()
    }

    // ViewHolder sınıfı
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vusProfileImageV: CircleImageView = itemView.findViewById(R.id.vusProfileImage)
        val vusPersonalNameV: MaterialTextView = itemView.findViewById(R.id.vusPersonalName)
        val vusUserNameV: MaterialTextView = itemView.findViewById(R.id.vusUserName)
    }

    // ViewHolder oluşturulduğunda çağrılan fonksiyon
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.view_user_single, parent, false)
        return UserViewHolder(view)
    }

    // Toplam öğe sayısını döndüren fonksiyon
    override fun getItemCount(): Int {
        return userList.size
    }

    // Her bir öğe için görünümü bağlayan fonksiyon
    override fun onBindViewHolder(holder: UserViewHolder , position: Int) {
        val currentUser = userList[position]

        // Kullanıcı ID'sine göre önbellekten kullanıcı bilgilerini yükleme
        currentUser.userID?.let {
            UserCache.getUser(currentUser.userID!!) { userModel ->
                userModel?.let { user ->
                    // Kullanıcı bilgileri mevcut, doğrudan önbellekten yükleyin.
                    holder.vusPersonalNameV.text = user.personalName
                        ?: context.getString(R.string.anonymous_user)
                    holder.vusUserNameV.text =
                        context.getString(R.string.user_name_tag, user.userName)
                    Glide.with(context)
                        .load(user.profileImage)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .error(R.drawable.ic_rounded_user)
                        .into(holder.vusProfileImageV)
                } ?: run {
                    // Kullanıcı bilgileri eksikse önbelleği temizle
                    UserCache.clear()
                    return@getUser
                }
            }
        }

        // Her bir öğe tıklandığında UserProfile ekranına geçiş yapma
        holder.itemView.setOnClickListener {
            val intent = Intent(context, UserProfile::class.java)
            intent.putExtra("userID", currentUser.userID)
            context.startActivity(intent)
        }
    }
}
