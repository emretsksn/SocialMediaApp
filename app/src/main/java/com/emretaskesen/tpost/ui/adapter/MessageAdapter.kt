package com.emretaskesen.tpost.ui.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.cache.UserCache
import com.emretaskesen.tpost.model.Message
import com.emretaskesen.tpost.ui.fragment.images.ShowImageFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView
import pl.droidsonroids.gif.GifImageView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


/**
 * `MessageAdapter`, mesajlaşma sayfasında mesajları RecyclerView'da görüntülemek için kullanılır.
 * @param context Uygulama bağlamı
 * @param msgList Yorumları içeren liste
 * @param listener Yorumlara tıklanıldığında veya silinmek istendiğinde gerçekleşen olayları dinleyen nesne
 */
//Mesajlaşma sayfamız için adapter sınıfımız
class
MessageAdapter(
    val context: Context ,
    private val msgList: ArrayList<Message> ,
    private val listener: OnMessageListener
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    //Mesaj dinleyicimiz. Uzun basılınca aksiyon göstermek için
    interface OnMessageListener {
        fun onMessageClick(message: Message , view: View)
    }

    //Alıcı ve gönderici için sayısal değer tanımlıyoruz
    private val itemReceive = 1
    private val itemSent = 2

    //Mesaj görünümünü burada belirliyoruz. getItemViewType fonksiyonundaki atamaya göre görüntü oluşturuluyor.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when (viewType) {
            //1 ise gelen mesaj görünümü atanıyor
            1 -> {
                val view: View =
                    LayoutInflater.from(context).inflate(R.layout.message_receive, parent, false)
                ReceiveViewHolder(view)
            }
            // else yani 2 ise giden mesaj görünümü atanır
            else -> {
                val view: View =
                    LayoutInflater.from(context).inflate(R.layout.message_sent, parent, false)
                SentViewHolder(view)
            }
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        //Rahat bir şekilde erişebilmek için arraylistimizi bir değere tanımlıyoruz
        val currentMessage = msgList[position]

        //Mesaj modelimizdeki verileri gönderici ve alıcıya göre ayırarak yerleştiriyoruz
        when (holder.javaClass) {
            SentViewHolder::class.java -> {
                val viewHolder = holder as SentViewHolder
                viewHolder.sentMessage.text = currentMessage.messageContent
                //Formattimestap fonksiyonumuz ile Firestoredan gelen Timestamp verimizi okunabilir bir veriye dönüştürerek ekrana yazdırıyoruz
                val sentTime = currentMessage.messageTime?.let { formatTimestamp(it) }
                viewHolder.senderSendTime.text = sentTime
                //Firestoreden gelen veride görsel mevcut ise mesaj itemimizdeki görsel alanı görünür yapıp görselimizi yerleştiriyoruz
                when {
                    currentMessage.messageImage != null -> {
                        viewHolder.sentImageView.visibility = View.VISIBLE
                        Glide.with(holder.itemView).load(currentMessage.messageImage).diskCacheStrategy(DiskCacheStrategy.ALL)
                            .error(R.drawable.gif_waiting).into(viewHolder.sentImageView)
                    }

                    currentMessage.messageGif!=null -> {
                        viewHolder.sentImageView.visibility = View.VISIBLE
                        Glide.with(holder.itemView).load(currentMessage.messageGif).diskCacheStrategy(DiskCacheStrategy.ALL)
                            .error(R.drawable.gif_waiting).into(viewHolder.sentImageView)
                    }

                    else -> {
                        viewHolder.sentImageView.visibility = View.GONE
                    }
                }
                //Firestoredan gelen veride lokasyon bilgisi mevcut ise map fragmentemizi görünür yapıp lokasyonumuzu yüklüyoruz
                when {
                    !currentMessage.messageLocation.isNullOrEmpty() -> {
                        val latitude = currentMessage.messageLocation !!["latitude"]
                        val longitude = currentMessage.messageLocation !!["longitude"]
                        viewHolder.sentMessage.text = currentMessage.locationName
                        viewHolder.sentMessage.setOnClickListener {
                            val uriString = "geo:0,0?q=$latitude,$longitude(${currentMessage.locationName})"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
                            intent.setPackage("com.google.android.apps.maps")
                            context.startActivity(intent)
                        }
                        viewHolder.sentMap.visibility = View.VISIBLE
                        viewHolder.sentMap.onCreate(null)
                        viewHolder.sentMap.onResume()
                        viewHolder.sentMap.getMapAsync { googleMap ->
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude!!,longitude!!),13f))
                            googleMap.addMarker(MarkerOptions().position(LatLng(latitude , longitude)))
                            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                            googleMap.uiSettings.setAllGesturesEnabled(false)
                        }
                    }

                    else -> {
                        viewHolder.locationView.visibility = View.GONE
                        viewHolder.sentMap.visibility = View.GONE
                    }
                }
                //Firestoredan gelen okundu bilgisine göre görsel kaynağımızı değiştiriyoruz
                when (currentMessage.readStatus) {
                    true -> {
                        holder.sndReadStatus.setImageResource(R.drawable.ic_message_read)
                    }

                    else -> {
                        holder.sndReadStatus.setImageResource(R.drawable.ic_message_unread)
                    }
                }
                //Gelen veride görsel varsa görsele tıkladığımızda dialog fragment kullanarak görseli büyüterek gösteriyoruz
                viewHolder.sentImageView.setOnClickListener {
                    if (currentMessage.messageImage != null) {
                        val showImageFragment =
                            ShowImageFragment.newInstance(currentMessage.messageImage !!.toUri()) // Dialog'ı gösterin (Fragment'inizi bir dialog olarak kullanmak için)
                        showImageFragment.show((context as FragmentActivity).supportFragmentManager,
                            showImageFragment.tag)
                    }
                }
                //Interfacede belirlediğimiz fonksiyon ile mesaj öğesine uzun tıklanınca bilgileri ilgili sayfaya iletiyıruz
                viewHolder.sndMessageView.setOnLongClickListener {
                    listener.onMessageClick(msgList[position], holder.sndMessageView)
                    true
                }

            }
            else -> { // do stuff for receive view holder
                val viewHolder = holder as ReceiveViewHolder
                //Gelen mesaj görünümünde kullanıcının profil resmini ön bellekten çekerek ilgili alan yüklüyoruz
                UserCache.getUser(currentMessage.senderID!!) { userModel ->
                    userModel?.let { user ->
                        Glide.with(holder.itemView).load(user.profileImage).error(R.drawable.ic_rounded_user)
                            .diskCacheStrategy(DiskCacheStrategy.ALL).into(viewHolder.receiverProfile)
                    } ?: run {
                        // Kullanıcı bilgisi önbellekte bulunamadıysa önbelleği temizler ve kullanıcıları veritabanından tekrar çeker.
                        UserCache.clear()
                        return@getUser
                    }
                }
                //Giden mesaj görünümünde yapılan işlemler aynı şekilde gelen mesaja da uygulanıyor
                viewHolder.receiveMessage.text = currentMessage.messageContent
                val sentTime = currentMessage.messageTime?.let { formatTimestamp(it) }
                viewHolder.receiverSendTime.text = sentTime
                when {
                    currentMessage.messageImage != null -> {
                        viewHolder.receiveImageView.visibility = View.VISIBLE
                        Glide.with(holder.itemView).load(currentMessage.messageImage).diskCacheStrategy(DiskCacheStrategy.ALL)
                            .error(R.drawable.gif_waiting).into(viewHolder.receiveImageView)
                    }

                    currentMessage.messageGif!=null -> {
                        viewHolder.receiveImageView.visibility = View.VISIBLE
                        Glide.with(holder.itemView).load(currentMessage.messageGif).error(R.drawable.gif_waiting)
                            .into(viewHolder.receiveImageView)
                    }

                    else -> {
                        viewHolder.receiveImageView.visibility = View.GONE
                    }
                }

                when {
                    !currentMessage.messageLocation.isNullOrEmpty() -> {
                        val latitude = currentMessage.messageLocation !!["latitude"]
                        val longitude = currentMessage.messageLocation !!["longitude"]
                        viewHolder.receiveMessage.text = currentMessage.locationName
                        viewHolder.receiveMessage.setOnClickListener {
                            val uriString = "geo:0,0?q=$latitude,$longitude(${currentMessage.locationName})"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
                            intent.setPackage("com.google.android.apps.maps")
                            context.startActivity(intent)
                        }
                        viewHolder.receiveMap.visibility = View.VISIBLE
                        viewHolder.receiveMap.onCreate(null)
                        viewHolder.receiveMap.onResume()
                        viewHolder.receiveMap.getMapAsync { googleMap ->
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude!!,longitude!!),13f))
                            googleMap.addMarker(MarkerOptions().position(LatLng(latitude , longitude)))
                            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                            googleMap.uiSettings.setAllGesturesEnabled(false)
                        }

                    }

                    else -> {
                        viewHolder.locationView.visibility = View.GONE
                        viewHolder.receiveMap.visibility = View.GONE
                    }
                }


                viewHolder.receiveImageView.setOnClickListener {
                    val showImageFragment =
                        ShowImageFragment.newInstance(currentMessage.messageImage !!.toUri()) // Dialog'ı gösterin (Fragment'inizi bir dialog olarak kullanmak için)
                    showImageFragment.show((context as FragmentActivity).supportFragmentManager,
                        showImageFragment.tag)
                }
                viewHolder.itemView.setOnLongClickListener {
                    listener.onMessageClick(msgList[position], holder.itemView)
                    true
                }

            }
        }
    }

    //Mesajın gelen mesaj mı giden mesaj mı olduğunu firestoredan gelen senderID değeri ile belirliyoruz.
    override fun getItemViewType(position: Int): Int {

        val currentMessage = msgList[position]
        //Mesaj bize aitse giden olarak tanımlıyoruz değilse gelen olarak
        return if (FirebaseAuth.getInstance().currentUser?.uid.equals(currentMessage.senderID)) {
            itemSent
        } else {
            itemReceive
        }
    }

    override fun getItemCount(): Int {
        return msgList.size

    }

    //Gelen ve giden mesaj görünümlerinde kolaylık sağlaması adında layout içerisindeki komponentlerimizi tanımlıyor ve bir değere atıyoruz
    class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sentMessage: MaterialTextView = itemView.findViewById(R.id.sndMessageContentV)
        val senderSendTime: MaterialTextView = itemView.findViewById(R.id.sndMessageTimeView)
        val sentImageView: GifImageView = itemView.findViewById(R.id.sndImageView)
        val sndReadStatus: ImageView = itemView.findViewById(R.id.sndReadStatus)
        val sentMap : MapView = itemView.findViewById(R.id.sndMapView)
        val locationView : MaterialTextView = itemView.findViewById(R.id.sndLocationView)
        val sndMessageView: CardView = itemView.findViewById(R.id.sndMessageView)
    }

    class ReceiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiveMessage : MaterialTextView = itemView.findViewById(R.id.rcvMessageContentV)
        val receiverSendTime : MaterialTextView = itemView.findViewById(R.id.rcvMessageTimeView)
        val receiveImageView : GifImageView = itemView.findViewById(R.id.rcvImageView)
        val locationView : MaterialTextView = itemView.findViewById(R.id.rcvLocationView)
        val receiverProfile: CircleImageView = itemView.findViewById(R.id.rcvProfileView)
        val receiveMap: MapView = itemView.findViewById(R.id.rcvMapView)
    }

    //Firestordan gelen veriyi SimpleDateFormat'a yani basit tarih formatına çeviren fonksiyonumuz
    private fun formatTimestamp(timestamp: Timestamp): String {
        val now = Calendar.getInstance()
        val timestampDate = Calendar.getInstance()
        timestampDate.time = timestamp.toDate()

        // Tarih kontrolü yaparak bugün veya dün için farklı formatlama
        val dateFormat: SimpleDateFormat = when {
            now.get(Calendar.DATE) == timestampDate.get(Calendar.DATE) -> {
                SimpleDateFormat(
                    "HH:mm", Locale.getDefault()
                )
            }

            now.get(Calendar.DATE) - timestampDate.get(Calendar.DATE) == 1 -> {
                SimpleDateFormat(
                    "'Dün' HH:mm", Locale.getDefault()
                )
            }

            else -> {
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            }
        }

        return dateFormat.format(timestampDate.time)
    }
}