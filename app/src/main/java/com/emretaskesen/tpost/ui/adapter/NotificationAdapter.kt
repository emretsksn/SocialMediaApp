package com.emretaskesen.tpost.ui.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.cache.UserCache
import com.emretaskesen.tpost.model.NotificationModel
import com.emretaskesen.tpost.ui.activity.post.PostDetail
import com.google.firebase.Timestamp
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NotificationAdapter(
    val context: Context ,
    private val notifyList: ArrayList<NotificationModel> ,
    private val listener: OnNotificationListener
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Listener arayüzü, bildirim öğelerine tıklama durumunu dinlemek için kullanılır.
    interface OnNotificationListener {
        fun onNotificationClick(notification: NotificationModel , view: View)
    }

    // Bildirim türlerine göre sabitler
    private val notifyLike = 1
    private val notifyComment = 2
    private val notifyFollow = 3
    private val notifyMention = 4

    // ViewHolder sınıfları
    class LikeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ntfIcon: CircleImageView = itemView.findViewById(R.id.likentfIcon)
        val ntfContent: TextView = itemView.findViewById(R.id.likentfContent)
        val ntfTime: TextView = itemView.findViewById(R.id.likentfTime)
    }

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ntfIcon: CircleImageView = itemView.findViewById(R.id.commentntfIcon)
        val ntfTitle: TextView = itemView.findViewById(R.id.commentntfTitle)
        val ntfContent: TextView = itemView.findViewById(R.id.commentntfContent)
        val ntfTime: TextView = itemView.findViewById(R.id.commentntfTime)
    }

    class FollowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ntfIcon: CircleImageView = itemView.findViewById(R.id.followntfIcon)
        val ntfTitle: TextView = itemView.findViewById(R.id.followntfTitle)
        val ntfContent: TextView = itemView.findViewById(R.id.followntfContent)
        val ntfTime: TextView = itemView.findViewById(R.id.followntfTime)
    }

    class MentionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ntfIcon: CircleImageView = itemView.findViewById(R.id.tagntfIcon)
        val ntfTitle: TextView = itemView.findViewById(R.id.tagntfTitle)
        val ntfContent: TextView = itemView.findViewById(R.id.tagntfContent)
        val ntfTime: TextView = itemView.findViewById(R.id.tagntfTime)
    }

    // ViewHolder türüne göre görünüm oluşturulması
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> {
                val view: View =
                    LayoutInflater.from(context).inflate(R.layout.view_notif_like, parent, false)
                LikeViewHolder(view)
            }

            2 -> {
                val view: View =
                    LayoutInflater.from(context).inflate(R.layout.view_notif_comment, parent, false)
                CommentViewHolder(view)
            }

            3 -> {
                val view: View =
                    LayoutInflater.from(context).inflate(R.layout.view_notif_follow, parent, false)
                FollowViewHolder(view)
            }
            else -> {
                val view: View =
                    LayoutInflater.from(context).inflate(R.layout.view_notif_tag, parent, false)
                MentionViewHolder(view)
            }
        }
    }

    // ViewHolder'a verilerin bağlanması
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val currentNotif = notifyList[position]
        when (holder.javaClass) {
            LikeViewHolder::class.java -> {
                val viewHolder = holder as LikeViewHolder

                // Like bildirimlerinin görünümüne kullanıcı resmi yüklenmesi
                UserCache.getUser(currentNotif.trackID!!) { userModel ->
                    userModel?.let { user ->
                        // Kullanıcı bilgileri mevcut, doğrudan önbellekten yükleyin.
                        Glide.with(context).load(user.profileImage).error(R.drawable.ic_rounded_user)
                            .into(holder.ntfIcon)
                    } ?: run {
                        // Kullanıcı bilgileri eksikse önbelleği temizle
                        UserCache.clear()
                        return@getUser
                    }
                }
                viewHolder.ntfContent.text = currentNotif.notificationTitle
                val currentTime = formatTimestamp(currentNotif.notificationTime!!)
                viewHolder.ntfTime.text = currentTime

                // Like bildirimine tıklanınca PostDetail ekranına geçiş yapılması
                viewHolder.itemView.setOnClickListener {
                    val intent = Intent(context, PostDetail::class.java)
                    intent.putExtra("userID", currentNotif.userID)
                    intent.putExtra("postID", currentNotif.postID)
                    context.startActivity(intent)
                }
            }

            CommentViewHolder::class.java -> {
                val viewHolder = holder as CommentViewHolder
                // Comment bildirimlerinin görünümüne kullanıcı resmi yüklenmesi
                UserCache.getUser(currentNotif.trackID!!) { userModel ->
                    userModel?.let { user ->
                        // Kullanıcı bilgileri mevcut, doğrudan önbellekten yükleyin.
                        Glide.with(context).load(user.profileImage).error(R.drawable.ic_rounded_user)
                            .into(holder.ntfIcon)
                    } ?: run {
                        // Kullanıcı bilgileri eksikse önbelleği temizle
                        UserCache.clear()
                        return@getUser
                    }
                }
                viewHolder.ntfTitle.text = currentNotif.notificationTitle
                viewHolder.ntfContent.text = currentNotif.notificationContent
                val currentTime = formatTimestamp(currentNotif.notificationTime!!)
                viewHolder.ntfTime.text = currentTime
                // Comment bildirimine tıklanınca PostDetail ekranına geçiş yapılması
                viewHolder.itemView.setOnClickListener {
                    val intent = Intent(context, PostDetail::class.java)
                    intent.putExtra("userID", currentNotif.userID)
                    intent.putExtra("postID", currentNotif.postID)
                    context.startActivity(intent)
                }
            }

            FollowViewHolder::class.java -> {
                val viewHolder = holder as FollowViewHolder
                // Follow bildirimlerinin görünümüne kullanıcı resmi yüklenmesi
                UserCache.getUser(currentNotif.trackID!!) { userModel ->
                    userModel?.let { user ->
                        // Kullanıcı bilgileri mevcut, doğrudan önbellekten yükleyin.
                        Glide.with(context).load(user.profileImage).error(R.drawable.ic_rounded_user)
                            .into(holder.ntfIcon)
                    } ?: run {
                        // Kullanıcı bilgileri eksikse önbelleği temizle
                        UserCache.clear()
                        return@getUser
                    }
                }

                viewHolder.ntfTitle.text =
                    currentNotif.notificationContent
                viewHolder.ntfContent.text =
                    currentNotif.notificationTitle
                // "Takip İsteği" bildirimi ise özel renk verilmesi
                if (currentNotif.notificationTitle == "Takip İsteği") {
                    viewHolder.ntfIcon.borderColor = context.getColor(R.color.teal_700)
                }
                val currentTime =
                    formatTimestamp(currentNotif.notificationTime!!)
                viewHolder.ntfTime.text =
                    currentTime
                // Follow bildirimine tıklanınca listener'a bildirimi iletmek
                viewHolder.itemView.setOnClickListener {
                    listener.onNotificationClick(notifyList[position], holder.itemView)
                }
            }
            MentionViewHolder::class.java -> {
                val viewHolder = holder as MentionViewHolder
                // Mention bildirimlerinin görünümüne kullanıcı resmi yüklenmesi
                UserCache.getUser(currentNotif.trackID!!) { userModel ->
                    userModel?.let { user ->
                        // Kullanıcı bilgileri mevcut, doğrudan önbellekten yükleyin.
                        Glide.with(context).load(user.profileImage).error(R.drawable.ic_rounded_user)
                            .into(holder.ntfIcon)
                    } ?: run {
                        // Kullanıcı bilgileri eksikse önbelleği temizle
                        UserCache.clear()
                        return@getUser
                    }
                }
                viewHolder.ntfTitle.text =
                    currentNotif.notificationContent
                viewHolder.ntfContent.text =
                    currentNotif.notificationTitle
                val currentTime =
                    formatTimestamp(currentNotif.notificationTime!!)
                viewHolder.ntfTime.text =
                    currentTime
                // Mention bildirimine tıklanınca PostDetail ekranına geçiş yapılması
                viewHolder.itemView.setOnClickListener {
                    val intent = Intent(context, PostDetail::class.java)
                    intent.putExtra("userID", currentNotif.userID)
                    intent.putExtra("postID", currentNotif.postID)
                    context.startActivity(intent)
                }
            }
        }
    }

    // Zaman damgasını düzgün bir şekilde biçimlendiren yardımcı metod
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
            now.get(Calendar.DATE) == timestampDate.get(Calendar.DATE) ->
                SimpleDateFormat("HH:mm", Locale.getDefault())

            now.get(Calendar.DATE) - timestampDate.get(Calendar.DATE) == 1 ->
                SimpleDateFormat("'Dün' HH:mm", Locale.getDefault())

            else ->
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        }

        return dateFormat.format(timestampDate.time)
    }

    // View türüne göre öğe türü belirleme
    override fun getItemViewType(position: Int): Int {

        val currentNotif = notifyList[position]
        return if (currentNotif.notificationType.equals("likeNotification")) {
            notifyLike
        } else if (currentNotif.notificationType.equals("commentNotification")) {
            notifyComment
        } else if (currentNotif.notificationType.equals("followNotification")) {
            notifyFollow
        } else {
            notifyMention
        }
    }

    // Toplam öğe sayısını döndürme
    override fun getItemCount(): Int {
        return notifyList.size
    }

}
