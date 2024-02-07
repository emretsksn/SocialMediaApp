package com.emretaskesen.tpost.ui.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.cache.UserCache
import com.emretaskesen.tpost.model.LikeModel
import com.emretaskesen.tpost.ui.activity.user.UserProfile
import com.google.firebase.Timestamp
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LikeAdapter(
    val context: Context ,
    private var likeUser: ArrayList<LikeModel> ,
) :
    RecyclerView.Adapter<LikeAdapter.CommentHolder>() {

    // İç sınıf (inner class) CommentHolder'ı tanımlıyoruz. Bu sınıf, RecyclerView içindeki öğelerin görünümlerini tutacak.
    inner class CommentHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Layout içindeki bileşenleri tanımlıyoruz ve bir değere atıyoruz.
        val vlProfileImageV: CircleImageView = itemView.findViewById(R.id.vlProfileImage)
        val vlUserNameV : TextView = itemView.findViewById(R.id.vlUserName)
        val vlTime: TextView = itemView.findViewById(R.id.vlTime)
    }

    // Yeni bir görünüm oluşturmak için onCreateViewHolder fonksiyonunu tanımlıyoruz.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.view_like_single, parent, false)
        return CommentHolder(view)
    }

    // Veri kümesindeki öğe sayısını döndüren fonksiyon.
    override fun getItemCount(): Int {
        return likeUser.size
    }

    // Görünümlerin bağlandığı ve içeriklerinin atanmasının gerçekleştiği fonksiyon.
    override fun onBindViewHolder(holder: CommentHolder , position: Int) {
        // Mevcut LikeModel öğesini alıyoruz.
        val currentLike = likeUser[position]

        // Kullanıcının bilgilerini önbellekten alıyoruz.
        UserCache.getUser(currentLike.userID!!) { userModel ->
            userModel?.let { user ->
                // Kullanıcı bilgileri mevcut, doğrudan önbellekten yükleyin.
                holder.vlUserNameV.text = context.getString(R.string.user_name_tag, user.userName)
                Glide.with(holder.itemView)
                    .load(user.profileImage)
                    .error(R.drawable.ic_rounded_user)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.vlProfileImageV)
            } ?: run {
                // Kullanıcı bilgisi önbellekte bulunamadıysa önbelleği temizler ve kullanıcıları veritabanından tekrar çeker.
                UserCache.clear()
                return@getUser
            }
        }

        // Timestamp'i biçimlendirerek zamanı gösteriyoruz.
        val currentTime = formatTimestamp(currentLike.likeTime!!)
        holder.vlTime.text = currentTime

        // Kullanıcının profil resmine tıklanıldığında UserProfile aktivitesini başlatan intent'i oluşturuyoruz.
        holder.itemView.setOnClickListener{
            val intent = Intent(context, UserProfile::class.java)
            intent.putExtra("userID",currentLike.userID)
            context.startActivity(intent)
        }
    }

    // Timestamp'i biçimlendiren yardımcı fonksiyon.
    private fun formatTimestamp(timestamp: Timestamp): String {
        val now = Calendar.getInstance()
        val timestampDate = Calendar.getInstance()
        timestampDate.time = timestamp.toDate()

        val diffInMillis = now.timeInMillis - timestampDate.timeInMillis

        // Öncelikli olarak dakika içindeyse
        if (diffInMillis < 60 * 60 * 1000) {
            val minutes = (diffInMillis / (60 * 1000)).toInt()
            return if (minutes <= 0) "az önce" else "$minutes dakika önce"
        }

        // Tarih kontrolü yaparak bugün veya dün için farklı formatlama
        val dateFormat: SimpleDateFormat = when {
            now.get(Calendar.DATE) == timestampDate.get(Calendar.DATE) -> SimpleDateFormat("HH:mm", Locale.getDefault())

            now.get(Calendar.DATE) - timestampDate.get(Calendar.DATE) == 1 -> SimpleDateFormat("'Dün' HH:mm", Locale.getDefault())

            else -> SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        }

        return dateFormat.format(timestampDate.time)
    }
}
