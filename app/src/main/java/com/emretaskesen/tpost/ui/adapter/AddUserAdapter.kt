package com.emretaskesen.tpost.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.cache.UserCache
import com.emretaskesen.tpost.model.UserModel
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import de.hdodenhof.circleimageview.CircleImageView

/**
 * `AddUserAdapter`, Kullanıcı eklenen aktivitelerde kullanıcıları listelemek için kullanılır.
 * @param context Uygulama bağlamı
 * @param userList Kullanıcıları içeren liste
 * @param listener Kullanıcılara tıklandığında gerçekleşen olayları dinleyen nesne
 */
class AddUserAdapter(val context: Context , var userList: ArrayList<UserModel> , private val listener: OnUserListener) :
    RecyclerView.Adapter<AddUserAdapter.UserViewHolder>() {

    // Firestore bağlantısını oluşturuyoruz
    val db = Firebase.firestore

    // Arayüz (interface) ile kullanıcı olaylarını dinleyen nesneyi tanımlıyoruz
    interface OnUserListener {
        fun onUserClik(user: UserModel , view: View)
    }

    // Filtreleme için kullanılacak metod
    @SuppressLint("NotifyDataSetChanged")
    fun filterList(filteredUserList: List<UserModel>) {
        this.userList = ArrayList(filteredUserList)
        notifyDataSetChanged()
    }

    // İç sınıf (inner class) UserViewHolder'ı tanımlıyoruz. Bu sınıf, RecyclerView içindeki öğelerin görünümlerini tutacak.
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Layout içindeki bileşenleri tanımlıyoruz ve bir değere atıyoruz
        val vtgProfileImageV : CircleImageView = itemView.findViewById(R.id.vtgProfileImage)
        val vtgPersonalNameV : MaterialTextView = itemView.findViewById(R.id.vtgPersonalName)
        val vtgUserNameV : MaterialTextView = itemView.findViewById(R.id.vtgUserName)
        val vtgAddUserV: ImageButton = itemView.findViewById(R.id.vtgAddUser)
    }

    // Yeni bir görünüm oluşturmak için onCreateViewHolder fonksiyonunu tanımlıyoruz
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.view_add_user_single, parent, false)
        return UserViewHolder(view)
    }

    // Veri kümesindeki öğe sayısını döndüren fonksiyon
    override fun getItemCount(): Int {
        return userList.size
    }

    // Görünümlerin bağlandığı ve içeriklerinin atanmasının gerçekleştiği fonksiyon
    override fun onBindViewHolder(holder: UserViewHolder , position: Int) {
        // Mevcut UserModel öğesini alıyoruz
        val currentUser = userList[position]

        //Listelenecek kullanıcının bilgilerini önbellekten alıyoruz
        UserCache.getUser(currentUser.userID!!) { userModel ->
            userModel?.let { user ->
                // Kullanıcı bilgileri mevcut, doğrudan önbellekten yükleyin
                holder.vtgPersonalNameV.text = user.personalName ?: context.getString(R.string.anonymous_user)
                holder.vtgUserNameV.text = context.getString(R.string.user_name_tag, user.userName)
                Glide.with(holder.itemView)
                    .load(user.profileImage)
                    .error(R.drawable.ic_rounded_user)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.vtgProfileImageV)
            } ?: run {
                // Kullanıcı bilgisi önbellekte bulunamadıysa önbelleği temizler ve kullanıcıları veritabanından tekrar çeker.
                UserCache.clear()
                return@getUser
            }
        }

        // Artı butonuna tıklandığında listener aracılığıyla olayı bildir
        holder.vtgAddUserV.setOnClickListener {
            listener.onUserClik(userList[position], holder.vtgAddUserV)
        }
    }
}
