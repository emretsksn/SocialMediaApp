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
import com.emretaskesen.tpost.model.CommentModel
import com.emretaskesen.tpost.ui.activity.user.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * `CommentAdapter`, bir gönderi altındaki yorumları RecyclerView'da görüntülemek için kullanılır.
 * @param context Uygulama bağlamı
 * @param commentUser Yorumları içeren liste
 * @param listener Yorumlara tıklanıldığında veya silinmek istendiğinde gerçekleşen olayları dinleyen nesne
 */
class CommentAdapter(
    val context: Context ,
    private var commentUser: ArrayList<CommentModel> ,
    private val listener: OnCommentListener
) :
    RecyclerView.Adapter<CommentAdapter.CommentHolder>() {

    // Firestore bağlantısını oluşturuyoruz
    val db = Firebase.firestore
    // Kullanıcının kendi ID'sini alıyoruz
    private val myId = Firebase.auth.currentUser?.uid

    // Arayüz (interface) ile yorum olaylarını dinleyen nesneyi tanımlıyoruz
    interface OnCommentListener {
        fun onCommentClick(comment: CommentModel , view: View)
    }

    // Yorum görünümünü tutacak iç sınıfı tanımlıyoruz
    inner class CommentHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Layout içindeki bileşenleri tanımlıyoruz ve bir değere atıyoruz
        val vcProfileImageV: CircleImageView = itemView.findViewById(R.id.vcProfileImage)
        val vcUserNameV : TextView = itemView.findViewById(R.id.vcUserName)
        val vcCommentContent : TextView = itemView.findViewById(R.id.vcCommentContent)
        val vcTimeV : TextView = itemView.findViewById(R.id.vcTime)
        val vcDeleteV: TextView = itemView.findViewById(R.id.vcDelete)
    }

    // Yeni bir görünüm oluşturmak için onCreateViewHolder fonksiyonunu tanımlıyoruz
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.view_comment_single, parent, false)
        return CommentHolder(view)
    }

    // Veri kümesindeki öğe sayısını döndüren fonksiyon
    override fun getItemCount(): Int {
        return commentUser.size
    }

    // Görünümlerin bağlandığı ve içeriklerinin atanmasının gerçekleştiği fonksiyon
    override fun onBindViewHolder(holder: CommentHolder , position: Int) {
        // Mevcut CommentModel öğesini alıyoruz
        val currentComment = commentUser[position]

        // Yorum yapan kullanıcının bilgilerini önbellekten alıyoruz
        UserCache.getUser(currentComment.userID!!) { userModel ->
            userModel?.let { user ->
                // Kullanıcı bilgileri mevcut, doğrudan önbellekten yükleyin
                holder.vcUserNameV.text = context.getString(R.string.user_name_tag, user.userName)
                Glide.with(holder.itemView)
                    .load(user.profileImage)
                    .error(R.drawable.ic_rounded_user)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.vcProfileImageV)
            } ?: run {
                // Kullanıcı bilgisi önbellekte bulunamadıysa önbelleği temizler ve kullanıcıları veritabanından tekrar çeker.
                UserCache.clear()
                return@getUser
            }
        }

        // Yorum içeriğini görüntüle
        holder.vcCommentContent.text = currentComment.commentContent

        // Yorumun tarihini biçimlendirerek görüntüle
        val currentTime = formatTimestamp(currentComment.commentTime!!)
        holder.vcTimeV.text = currentTime

        // Yorum yapan kullanıcıya tıklanıldığında UserProfile aktivitesini başlatan intent'i oluşturuyoruz
        holder.itemView.setOnClickListener{
            if (currentComment.userID!=myId)
            {
                val intent = Intent(context, UserProfile::class.java)
                intent.putExtra("userID",currentComment.userID)
                context.startActivity(intent)
            }else{
                val intent = Intent(context, UserProfile::class.java)
                intent.putExtra("userID",currentComment.userID)
                context.startActivity(intent)
            }
        }

        // Eğer yorum yapan kullanıcı kendi yorumuysa silme seçeneğini görüntüle
        if (currentComment.userID==myId){
            holder.vcDeleteV.visibility = View.VISIBLE
            holder.vcDeleteV.setOnClickListener {
                // Silme ikonuna tıklandığında listener aracılığıyla olayı bildir
                listener.onCommentClick(commentUser[position], holder.vcDeleteV)
            }
        }
    }

    // Timestamp'i biçimlendiren yardımcı fonksiyon
    private fun formatTimestamp(timestamp: Timestamp): String {
        val now = Calendar.getInstance()
        val timestampDate = Calendar.getInstance()
        timestampDate.time = timestamp.toDate()

        val diffInMillis = now.timeInMillis - timestampDate.timeInMillis

        // Öncelikli olarak dakika içindeyse
        if (diffInMillis < 60 * 60 * 1000) {
            val minutes = (diffInMillis / (60 * 1000)).toInt()
            return if (minutes <= 0) context.getString(R.string.just_now) else context.getString(
                R.string.minute_ago,
                minutes.toString())
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
