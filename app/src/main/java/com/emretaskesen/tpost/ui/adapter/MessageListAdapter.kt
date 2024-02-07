package com.emretaskesen.tpost.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.cache.GroupCache
import com.emretaskesen.tpost.cache.UserCache
import com.emretaskesen.tpost.model.MessageList
import com.emretaskesen.tpost.ui.activity.message.ChatPage
import com.emretaskesen.tpost.ui.activity.message.GroupChat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import de.hdodenhof.circleimageview.CircleImageView

class MessageListAdapter(
    val context: Context ,
    private var messageList: ArrayList<MessageList> ,
) :
    RecyclerView.Adapter<MessageListAdapter.MyMessageViewHolder>() {
    val db = Firebase.firestore

    // İç sınıf (inner class) MyMessageViewHolder'ı tanımlıyoruz. Bu sınıf, RecyclerView içindeki öğelerin görünümlerini tutacak.
    inner class MyMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Layout içindeki bileşenleri tanımlıyoruz ve bir değere atıyoruz.
        val vmProfileImageV: CircleImageView = itemView.findViewById(R.id.vmProfileImage)
        val vmLastMessageV: TextView = itemView.findViewById(R.id.vmLastMessage)
        val vmUserNameV: TextView = itemView.findViewById(R.id.vmUserName)
        val vmMessageCount: TextView = itemView.findViewById(R.id.vmMessageCount)
    }

    // Yeni bir görünüm oluşturmak için onCreateViewHolder fonksiyonunu tanımlıyoruz.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyMessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.view_message_single, parent, false)
        return MyMessageViewHolder(view)
    }

    // Veri kümesindeki öğe sayısını döndüren fonksiyon.
    override fun getItemCount(): Int {
        return messageList.size
    }

    // Görünümlerin bağlandığı ve içeriklerinin atanmasının gerçekleştiği fonksiyon.
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyMessageViewHolder , position: Int) {
        // Mevcut MessageList öğesini alıyoruz.
        val currentMessages = messageList[position]
        // Aktif kullanıcının ID'sini alıyoruz.
        val myID = FirebaseAuth.getInstance().currentUser!!.uid

        // Firestore'dan gelen veride mesaj verisi bir gruba mı yoksa bir kişiye ait kontrol edip, değere göre işlem yapıyoruz.
        if (currentMessages.isGroup == true){
            // onBindViewHolder içinde kullanıcı bilgilerini ayarlamak için:
            GroupCache.getGroup(currentMessages.userID!!){group->
                group?.let {
                    // Grup bilgileri mevcut, doğrudan önbellekten yükleyin.
                    holder.vmUserNameV.text =
                        context.getString(R.string.group_name_etc , group.groupName)
                    Glide.with(context)
                        .load(group.groupPhoto)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .error(R.drawable.ic_rounded_user)
                        .into(holder.vmProfileImageV)
                } ?: run {
                    // Grup bilgisi önbellekte bulunamadıysa önbelleği temizler ve grupları veritabanından tekrar çeker.
                    GroupCache.clear()
                    return@getGroup
                }

            }
        }else{
            // onBindViewHolder içinde kullanıcı bilgilerini ayarlamak için:
            UserCache.getUser(currentMessages.userID!!) { userModel ->
                userModel?.let { user ->
                    // Kullanıcı bilgileri mevcut, doğrudan önbellekten yükleyin.
                    holder.vmUserNameV.text = context.getString(R.string.user_name_tag, user.userName)
                    Glide.with(context)
                        .load(user.profileImage)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .error(R.drawable.ic_rounded_user)
                        .into(holder.vmProfileImageV)
                } ?: run {
                    // Kullanıcı bilgisi önbellekte bulunamadıysa önbelleği temizler ve kullanıcıları veritabanından tekrar çeker.
                    UserCache.clear()
                    return@getUser
                }
            }
        }

        // Mesaj sayısına göre görünürlüğü ayarlıyoruz.
        if (currentMessages.messageCount==true) {
            holder.vmMessageCount.visibility = View.VISIBLE
            holder.vmMessageCount.text = "+1"
        }else{
            holder.vmMessageCount.visibility = View.GONE
        }

        // Son mesajı gösteriyoruz.

        if (currentMessages.lastMessage?.contains("Konum") == true){
            holder.vmLastMessageV.text = "${currentMessages.lastMessage} 🗺"
        }else if (currentMessages.lastMessage?.contains("Görsel") == true){
            holder.vmLastMessageV.text = "${currentMessages.lastMessage} 📷"
        }else{
            holder.vmLastMessageV.text = currentMessages.lastMessage
        }

        // Mesajlar sayfasında mesaj öğesine tıklandığında ilgili aktivitenin açılması için fonksiyonumuz.
        holder.itemView.setOnClickListener {
            if (currentMessages.isGroup == true){
                // Grup ise GroupChat aktivitesini başlat.
                val intent = Intent(context, GroupChat::class.java)
                intent.putExtra("groupID", currentMessages.userID)
                intent.putExtra("readStatus", "readMessages")
                context.startActivity(intent)
            }else{
                // Kullanıcı ise ChatPage aktivitesini başlat.
                val intent = Intent(context, ChatPage::class.java)
                intent.putExtra("userID", currentMessages.userID)
                intent.putExtra("readStatus", "readMessages")
                context.startActivity(intent)
            }
        }

        // Mesajlar sayfasında mesaj öğesine uzun basıldığında popup açmasını sağlayan fonksiyonumuz.
        holder.itemView.setOnLongClickListener { view->
            val popupMenu = androidx.appcompat.widget.PopupMenu(context , view)
            popupMenu.inflate(R.menu.menu_option_messagelist)
            popupMenu.setOnMenuItemClickListener(object : androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onMenuItemClick(item : MenuItem?) : Boolean {
                    when (item?.itemId) {
                        R.id.actionArchive -> {
                            // Arşivleme seçeneği seçildiğinde bir Toast mesajı göster.
                            Toast.makeText(context,"Yakında", Toast.LENGTH_LONG).show()
                            return true
                        }
                        R.id.actionDelete -> {
                            // Silme seçeneği seçildiğinde ilgili mesajı veritabanından sil ve veri setini güncelle.
                            db.collection("Users").document(myID).collection("MyMessages").document(currentMessages.userID!!).delete().addOnSuccessListener {
                                notifyDataSetChanged()
                            }
                            return true
                        }
                    }
                    return false
                }
            })
            popupMenu.show()
            true
        }

    }
}
