package com.emretaskesen.tpost.ui.activity.message

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.emretaskesen.tpost.R
import com.emretaskesen.tpost.cache.UserCache
import com.emretaskesen.tpost.databinding.ActivityChatPageBinding
import com.emretaskesen.tpost.model.Message
import com.emretaskesen.tpost.ui.activity.main.MapsActivity
import com.emretaskesen.tpost.ui.activity.user.UserProfile
import com.emretaskesen.tpost.ui.adapter.MessageAdapter
import com.emretaskesen.tpost.ui.fragment.images.SelectSourceFragment
import com.emretaskesen.tpost.util.ConstVal.Configurations.GIPHY_API_KEY
import com.giphy.sdk.core.models.Media
import com.giphy.sdk.ui.GPHContentType
import com.giphy.sdk.ui.Giphy
import com.giphy.sdk.ui.views.GiphyDialogFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID


class ChatPage : AppCompatActivity() , GiphyDialogFragment.GifSelectionListener ,
    SelectSourceFragment.BottomSheetListener , MessageAdapter.OnMessageListener {
    lateinit var binding : ActivityChatPageBinding
    lateinit var recyclerViewAdapter : MessageAdapter
    private val msgList = ArrayList<Message>()
    private var selectedImage : Uri? = null
    private var selectedGif : Uri? = null
    val db = Firebase.firestore
    private val storage = Firebase.storage
    private val auth = FirebaseAuth.getInstance()
    private val myID = auth.currentUser !!.uid
    private lateinit var senderRef : CollectionReference
    private lateinit var receiverRef : CollectionReference
    var userID : String? = null
    private var messageContent : String? = null
    private var latLng : LatLng? = null
    private var locationName : String? = null

    //Haritalar aktivitesini aç
    @Suppress("DEPRECATION")
    private val getLocation =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                latLng = result.data !!.getParcelableExtra("latLang")
                locationName = result.data !!.getStringExtra("locationName")
                saveMessage(null)
            }
        }

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViewsAndFunctions()
    }

    private fun initViewsAndFunctions() {
        val intent : Intent? = intent
        //Intent ile gelen veriyi kontrol et.
        when {
            intent != null -> {
                if (intent.hasExtra("userID")) {
                    userID = intent.getStringExtra("userID") as String
                }
            }
            else -> this.finish()
        }

        //Inten ile gelen UserID boş değilse Firestore koleksiyon referanslarını oluştur
        if (userID != null) {
            senderRef =
                db.collection("Users").document(myID).collection("Chat").document(userID !!)
                    .collection("messages")
            receiverRef =
                db.collection("Users").document(userID !!).collection("Chat").document(myID)
                    .collection("messages")
        }

        // Adapteri tanımla ve recyclerview'e atama yap
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerViewAdapter = MessageAdapter(this , msgList , this)
        binding.charRecyclerView.layoutManager = layoutManager
        binding.charRecyclerView.adapter = recyclerViewAdapter

        // Gönder butonuna tıklayınca yapılacak işlem
        binding.sendButton.setOnClickListener {
            saveDatabase()
        }

        // + butonuna tıklayınca yapılacak işlem
        binding.addExtraButton.setOnClickListener { view ->
            showPopup(view)
        }

        // Toolbar üzerinden kullanıcı profiline tıklayınca yapılacak işlem
        binding.gotoProfile.setOnClickListener {
            val otherUser = Intent(this , UserProfile::class.java)
            otherUser.putExtra("userID" , userID)
            startActivity(otherUser)
        }

        //Edittexte tıklayınca emoji picker kapat
        binding.messageBox.setOnClickListener {
            binding.cardEmojiArea.visibility = View.GONE
        }
        binding.addEmoji.setOnClickListener {
            // Klavyeyi kontrol etmek için kullanılacak InputMethodManager örneği alınıyor.
            val inputMethodManager =
                it.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            // Eğer EditText şu anda odaklanmış değilse klavyeyi göster.
            when {
                ! binding.messageBox.isFocused -> {
                    binding.messageBox.requestFocus() // EditText odaklanmasını sağlar.
                    // Klavyeyi göster
                    inputMethodManager.showSoftInput(binding.messageBox ,
                        InputMethodManager.SHOW_IMPLICIT)
                    binding.cardEmojiArea.visibility = View.GONE
                }

                else -> {
                    // Eğer EditText zaten odaklanmışsa, klavyeyi kapat.
                    inputMethodManager.hideSoftInputFromWindow(binding.messageBox.windowToken , 0)
                    binding.cardEmojiArea.visibility = View.VISIBLE
                }
            }
        }

        //
        binding.emojiPicker.setOnEmojiPickedListener {
            binding.messageBox.append(it.emoji)
        }

        // Mesaj sayfası için Giphy sdk'sini yapılandırın
        Giphy.configure(this , GIPHY_API_KEY)

        // Eylem çubuğunu tanımlayın ve yapılandırın
        setSupportActionBar(binding.userprofileToolbar)
        val myActionBar : ActionBar? = supportActionBar
        myActionBar !!.setDisplayHomeAsUpEnabled(true)
        myActionBar.setDisplayShowTitleEnabled(false)

       // Alıcı kullanıcının bilgilerini çek ve ekrana yazdır
        if (userID != null) {
            UserCache.getUser(userID !!) { userModel ->
                userModel?.let { user ->
                    // Kullanıcı bilgileri mevcut, doğrudan önbellekten yükleyin.
                    binding.userName.text = getString(R.string.user_name_tag , user.userName)
                    Glide.with(this@ChatPage).load(user.profileImage)
                        .error(R.drawable.ic_rounded_user).diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(binding.profileImage)
                } ?: run {
                    UserCache.clear()
                    return@getUser
                }
            }
        }

        textControl()
        isWrite(false)
        messageCounterReset()
        getMessage()
    }

    // Fotoğraf, gif ve konum için + butonunda açılacak popup menu
    private fun showPopup(view : View) {
        val popupMenu = PopupMenu(this , view)
        popupMenu.inflate(R.menu.menu_message_extra)
        popupMenu.setForceShowIcon(true)
        popupMenu.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
            override fun onMenuItemClick(item : MenuItem?) : Boolean {
                when (item?.itemId) {
                    R.id.action_camera -> {

                        val selectSource = SelectSourceFragment()
                        selectSource.setBottomSheetListener(this@ChatPage)
                        val tag = SelectSourceFragment::class.java.simpleName
                        selectSource.show(supportFragmentManager , tag)
                        selectedGif = null
                    }

                    R.id.action_gif -> {
                        showGiphyDialog()
                        selectedImage = null
                    }

                    R.id.action_location -> {
                        getLocation.launch(Intent(this@ChatPage , MapsActivity::class.java))
                        return true
                    }


                }
                return false
            }
        })
        popupMenu.show()
    }

    // Mesaj öğelerini gizle ve sil
    private fun hideAndClearMessageItems() {
        selectedImage = null
        selectedGif = null
        messageContent = null
        binding.messageBox.text?.clear()
        binding.loadingBar.isEnabled = false
        binding.loadingBar.visibility = View.GONE
    }

    // Giphy iletişim kutusu görüntüleme işlevi
    private fun showGiphyDialog() {
        val giphyDialog = GiphyDialogFragment.newInstance()
        giphyDialog.gifSelectionListener = this
        giphyDialog.show(supportFragmentManager , "giphy_dialog")
    }

    // Mesaj girişi boşsa mesaj öğelerini kontrol eden fonksiyon
    private fun textControl() {
        binding.messageBox.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s : CharSequence? ,
                start : Int ,
                count : Int ,
                after : Int ,
            ) {
            }

            override fun onTextChanged(
                s : CharSequence? ,
                start : Int ,
                before : Int ,
                count : Int ,
            ) {
            }

            override fun afterTextChanged(s : Editable?) { // This method is called after the text has changed.
                // Here we control the visibility of sendButton.
                when {
                    s.isNullOrBlank() -> {
                        when {
                            selectedGif != null || selectedImage != null -> {
                                isWrite(true)
                                binding.sendButton.setImageResource(R.drawable.ic_message_send_active)
                                binding.addExtraButton.visibility = View.GONE
                                binding.sendButton.isEnabled = true
                            }

                            else -> {
                                isWrite(false)
                                binding.sendButton.isEnabled = false
                                binding.addExtraButton.visibility = View.VISIBLE
                                binding.sendButton.setImageResource(R.drawable.ic_message_send_passive)
                            }
                        }
                    }

                    else -> {
                        isWrite(true)
                        binding.sendButton.setImageResource(R.drawable.ic_message_send_active)
                        binding.addExtraButton.visibility = View.GONE
                        binding.sendButton.isEnabled = true
                    }
                }
            }
        })
    }

    // Etkinlik çalışmaya devam ederken kullanıcının mesaj yazdığını ve aktifliğini kontrol et
    override fun onResume() {
        super.onResume()
        joinTimeListener()
        isWrite(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        isWrite(false)
    }

    private fun joinTimeListener() {
        if (userID != null) {
            val userRef = db.collection("Users").document(userID !!)
            userRef.addSnapshotListener { value , error ->
                if (error != null) {
                    return@addSnapshotListener
                } else {
                    if (value != null) {
                        val joinTime = value.getTimestamp("joinTime")
                        val onlineStatus = value.get("onlineStatus") as Boolean?
                        val writeText = value.get("isWrite") as Boolean?
                        when (onlineStatus) {
                            true -> {
                                when (writeText) {
                                    true -> {
                                        binding.joinTime.text = getString(R.string.writing)
                                        binding.joinTime.visibility = View.VISIBLE
                                    }

                                    else -> {
                                        val online = formatTimestamp(joinTime !!)
                                        binding.joinTime.text = online
                                    }
                                }
                            }

                            else -> {
                                when (writeText) {
                                    true -> {
                                        binding.joinTime.text = getString(R.string.writing)
                                        binding.joinTime.visibility = View.VISIBLE
                                    }

                                    else -> {
                                        binding.joinTime.visibility = View.GONE
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    private fun isWrite(isWrite : Boolean) {
        val myID = auth.currentUser?.uid !!
        db.collection("Users").document(myID).update("isWrite" , isWrite)
    }

    private fun messageCounterUpdate() {
        if (userID != null) {
            val myID = auth.currentUser !!.uid
            val userName = auth.currentUser?.displayName
            val messageMap = HashMap<String , Any>()
            messageMap["userID"] = userID !!
            messageMap["messageTime"] = Timestamp.now()
            messageMap["isGroup"] = false
            if (! messageContent.isNullOrBlank()) {
                val shortMessage =
                    if (messageContent !!.length > 30) messageContent !!.take(30) + "..." else messageContent !!
                messageMap["lastMessage"] = "$userName: $shortMessage"
            } else {
                if (latLng != null) {
                    messageMap["lastMessage"] = "$userName: Konum"
                } else if (selectedImage != null || selectedGif != null) {
                    messageMap["lastMessage"] = "$userName: Görsel"
                }
            }
            val counterMap = HashMap<String , Any>()
            counterMap["messageCount"] = true
            counterMap["messageTime"] = Timestamp.now()
            if (! messageContent.isNullOrBlank()) {
                val shortMessage =
                    if (messageContent !!.length > 30) messageContent !!.take(30) + "..." else messageContent !!
                counterMap["lastMessage"] = "$userName: $shortMessage"
            } else {
                if (latLng != null) {
                    counterMap["lastMessage"] = "$userName: Konum"
                } else if (selectedImage != null || selectedGif != null) {
                    counterMap["lastMessage"] = "$userName: Görsel"
                }
            }
            counterMap["userID"] = myID
            counterMap["isGroup"] = false
            db.collection("Users").document(userID !!).collection("MyMessages").document(myID)
                .set(counterMap).addOnSuccessListener {
                    db.collection("Users").document(myID).collection("MyMessages")
                        .document(userID !!).set(messageMap).addOnSuccessListener {
                            hideAndClearMessageItems()
                        }
                }


        }

    }

    private fun createNotification() {
        if (userID != null) {
            val currentUser = FirebaseAuth.getInstance().currentUser !!
            val myID = currentUser.uid
            val lastMessage = binding.messageBox.text
            val userName = auth.currentUser?.displayName !!
            val notificationRef =
                db.collection("Users").document(userID !!).collection("MessageNotification")
            val notification = HashMap<String , Any>()
            val notificationID = UUID.randomUUID().toString()
            if (! lastMessage.isNullOrBlank()) {
                val message = lastMessage.toString()
                val shortMessage = if (message.length > 30) message.take(30) + "..." else message
                notification["notificationContent"] = shortMessage
            } else {
                if (latLng != null) {
                    notification["notificationContent"] = "Konum"
                } else if (selectedImage != null || selectedGif != null) {
                    notification["notificationContent"] = "Görsel"
                }
            }
            notification["messageType"] = "Individual"
            notification["notificationID"] = notificationID
            notification["notificationTitle"] = userName
            notification["notificationTime"] = Timestamp.now()
            notification["trackID"] = myID
            notification["isNew"] = true
            notificationRef.document(notificationID).set(notification).addOnSuccessListener {
                messageCounterUpdate()
            }
        }
    }

    private fun deleteNotification() {
        val notificationRef =
            db.collection("Users").document(userID !!).collection("MessageNotification")
        notificationRef.get().addOnSuccessListener { snapshot ->
            if (snapshot != null) {
                val batch = db.batch()
                for (document in snapshot.documents) {
                    batch.delete(document.reference)
                }

                batch.commit()
            }
        }
    }

    //Mesaj sayacı sıfırlama fonksiyonu
    //Firebase FireStore'da Kullanıcıya bağlı mesaj koleksiyonundaki mesaj sayacını sıfırlar
    private fun messageCounterReset() {
        val readStatus = intent.getStringExtra("readStatus")
        if (readStatus == "readMessages") {
            val userID = intent.getStringExtra("userID") !!
            val myID = auth.currentUser !!.uid
            val counterMap = HashMap<String , Any>()
            counterMap["messageCount"] = false
            db.collection("Users").document(myID).collection("MyMessages").document(userID)
                .update(counterMap).addOnSuccessListener {
                    updateReadStatus()
                    deleteNotification()
                }
        }

    }

    // Okundu bilgisi güncelleme fonksiyonu
    // Kullanıcı aktivitede oturum açtıysa ve aktivite devam ediyorsa mesajları okundu olarak güncelle
    private fun updateReadStatus() {

        receiverRef.whereEqualTo("readStatus" , false).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    return@addOnSuccessListener
                }

                // Toplu yazdırmayı başlat
                val batch = db.batch()

                // Her belge için toplu işlemin bir parçası olarak `readStatus'u güncelleyin
                for (document in documents) {
                    val docRef = receiverRef.document(document.id)
                    batch.update(docRef , "readStatus" , true)
                }

                // Toplu işlemi uygula
                batch.commit().addOnCompleteListener {
                    if (it.isSuccessful) {
                        return@addOnCompleteListener
                    } else {
                        return@addOnCompleteListener
                    }
                }
            }
    }

    // Veritabanı kayıt fonksiyonu
    private fun saveDatabase() {
        when {
            selectedImage != null -> {
                // Kullanıcı bir görüntü eklediyse, onu depoya kaydedin ve firestore kaydetme işlevini çağırın
                binding.loadingBar.visibility = View.VISIBLE
                binding.loadingBar.isEnabled = true
                val reference = storage.reference

                val uuid = UUID.randomUUID()
                val imageName = uuid.toString()

                val imageReference = reference.child("messageImage").child(imageName)

                imageReference.putFile(selectedImage !!).addOnSuccessListener {
                    val uploadImageReference = reference.child("messageImage").child(imageName)
                    uploadImageReference.downloadUrl.addOnSuccessListener { uri ->
                        val downloadUrl = uri.toString()
                        saveMessage(downloadUrl)
                    }

                }.addOnFailureListener { exception ->
                    Toast.makeText(applicationContext ,
                        exception.localizedMessage ,
                        Toast.LENGTH_LONG).show()
                }
            }

            else -> {
                // Eğer görsel eklenmemişse doğrudan firestore kayıt fonksiyonunu çağırın
                saveMessage(null)
            }
        }

    }

    // Firestore kayıt fonksiyonu
    private fun saveMessage(photoUrl : String?) {
        val currentUserID = FirebaseAuth.getInstance().currentUser?.uid ?: return

        messageContent = binding.messageBox.text.toString()
        val time = Timestamp.now()
        val messageID = UUID.randomUUID().toString()
        val messageMap = HashMap<String , Any>()
        messageMap["messageID"] = messageID
        messageMap["senderID"] = currentUserID
        messageMap["messageTime"] = time
        if (latLng != null) {
            messageMap["messageLocation"] = latLng !!
            messageMap["locationName"] = locationName !!
        }
        //Mesaj içeriğini kontrol et. Bir mesaj yazıldıysa mesaj içeriğini kaydedin
        if (! messageContent.isNullOrBlank()) {
            messageMap["messageContent"] = messageContent.toString()
        }
        // Resim bağlantısını kontrol edin. Resim seçilmişse kaydedin. Resim boş ve giflerle doluysa tekrar kaydedin.
        if (photoUrl != null) {
            messageMap["messageImage"] = photoUrl
        } else if (selectedImage == null && selectedGif != null) {
            messageMap["messageGif"] = selectedGif.toString()
        }
        messageMap["readStatus"] = false

        senderRef.document(messageID).set(messageMap).addOnSuccessListener {
            receiverRef.document(messageID).set(messageMap).addOnSuccessListener {
                createNotification()
            }
        }
    }

    // Mesajları yükleme fonksiyonu
    @SuppressLint("NotifyDataSetChanged")
    private fun getMessage() {
       // Mesajları yüklerken yalnızca alıcı odasından yükleyin
        senderRef.orderBy("messageTime" , Query.Direction.ASCENDING)
            .addSnapshotListener { value , error ->
                if (error != null) {
                    return@addSnapshotListener
                } else {
                    msgList.clear() //Check document
                    if (value != null) {
                        if (! value.isEmpty) {
                            val documents = value.documents
                            for (msgDoc in documents) {
                                val message = msgDoc.toObject(Message::class.java)
                                if (message != null) {
                                    msgList.add(message)
                                }
                                updateReadStatus()
                                binding.charRecyclerView.scrollToPosition(recyclerViewAdapter.itemCount - 1)
                            }
                            recyclerViewAdapter.notifyDataSetChanged()
                        } else {
                           // Mesaj verisi boşsa tekrar dinleyiciye dön
                            return@addSnapshotListener
                        }
                    }
                }
            }

    }

    // Araç çubuğu menü tanımı
    override fun onCreateOptionsMenu(menu : Menu?) : Boolean {
        menuInflater.inflate(R.menu.menu_chat_page , menu)
        return true
    }

    // Araç çubuğu menüsü seçim eylemleri
    override fun onOptionsItemSelected(item : MenuItem) : Boolean = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }

        R.id.cp_go_profile -> {
            val otherUser = Intent(this , UserProfile::class.java)
            otherUser.putExtra("userID" , userID)
            startActivity(otherUser)
            true
        }

        R.id.cp_clear_message -> {
            showAlert()
            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }

    }

    // Mesaj silmek için açılır pencere fonksiyonu
    private fun showAlert() {
        val deleteAlert = AlertDialog.Builder(this)
        deleteAlert.setTitle(getString(R.string.delete_messages))
        deleteAlert.setMessage(getString(R.string.delete_messages_info))
        deleteAlert.setPositiveButton(getString(R.string.delete_all)) { _ , _ ->
            clearMessagesFromRoom(senderRef)
            clearMessagesFromRoom(receiverRef)
        }
        deleteAlert.setNegativeButton(getString(R.string.only_delete_me)) { _ , _ ->
            clearMessagesFromRoom(senderRef)
        }
        deleteAlert.setNeutralButton(getString(R.string.cancel)) { dialog , _ ->
            dialog.dismiss()
        }
        deleteAlert.show()
    }


    // Mesaj odalarındaki verileri sil
    @SuppressLint("NotifyDataSetChanged")
    private fun clearMessagesFromRoom(collectRef : CollectionReference) {
        collectRef.get().addOnSuccessListener { snapshot ->
            if (snapshot != null) {
                val batch = db.batch()
                for (document in snapshot.documents) {
                    batch.delete(document.reference)
                }

                batch.commit().addOnSuccessListener {
                    recyclerViewAdapter.notifyDataSetChanged()
                }.addOnFailureListener { e ->
                    Toast.makeText(this , "Hata: ${e.localizedMessage}" , Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this , getString(R.string.no_message) , Toast.LENGTH_LONG).show()
            }

        }.addOnFailureListener { e ->
            Toast.makeText(this ,
                getString(R.string.cant_load_message , e.localizedMessage) ,
                Toast.LENGTH_SHORT).show()
        }

    }


    // Zaman damgası verilerini okunabilir tarih verilerine dönüştürün
    private fun formatTimestamp(timestamp : Timestamp) : String {

        val now = Calendar.getInstance()
        val timestampDate = Calendar.getInstance()
        timestampDate.time = timestamp.toDate()

        val diffInMillis = now.timeInMillis - timestampDate.timeInMillis

        // Eğer öncelikle dakikalar içindeyse
        if (diffInMillis < 60 * 60 * 1000) {
            val minutes = (diffInMillis / (60 * 1000)).toInt()
            return if (minutes <= 0) getString(R.string.online)
            else getString(R.string.minute_ago , minutes.toString())
        }

        // Tarihi kontrol ederek bugün veya dün için farklı biçimlendirme
        val dateFormat : SimpleDateFormat = when {
            now.get(Calendar.DATE) == timestampDate.get(Calendar.DATE) -> SimpleDateFormat("HH:mm" ,
                Locale.getDefault())

            now.get(Calendar.DATE) - timestampDate.get(Calendar.DATE) == 1 -> SimpleDateFormat("'Dün' HH:mm" ,
                Locale.getDefault())

            else -> SimpleDateFormat("dd.MM.yyyy" , Locale.getDefault())
        }

        return dateFormat.format(timestampDate.time)
    }

    // Belirli bir mesajı sadece gönderenden silme fonksiyonu
    @SuppressLint("NotifyDataSetChanged")
    private fun deleteMyMessage(messageID : String , messageImage : String?) {
        val alert = AlertDialog.Builder(this@ChatPage)
        alert.setTitle(getString(R.string.alert_title_delete_message))
        alert.setMessage(getString(R.string.alert_message_delete_message))
        alert.setPositiveButton(R.string.approve) { _ , _ ->
            senderRef.document(messageID).delete().addOnSuccessListener {
                Toast.makeText(this ,
                    getString(R.string.message_deleted_successfully) ,
                    Toast.LENGTH_SHORT).show()
                // Mesajı yerel listeden ve RecyclerView'dan sil
                msgList.removeIf { it.messageID == messageID }
                recyclerViewAdapter.notifyDataSetChanged()
            }
            if (messageImage != null) {
                deletePostImage(messageImage)
            }
        }
        alert.setNegativeButton(R.string.cancel) { dialog , _ ->
            dialog.dismiss()
        }
        alert.show()
    }

    // Belirli bir mesajı alıcı ve gönderenden silme fonksiyonu
    @SuppressLint("NotifyDataSetChanged")
    private fun deleteThisMessage(messageID : String , messageImage : String?) {
        val alert = AlertDialog.Builder(this@ChatPage)
        alert.setTitle(getString(R.string.alert_title_delete_message))
        alert.setMessage(getString(R.string.alert_message_delete_message))
        alert.setPositiveButton(R.string.approve) { _ , _ ->
            senderRef.document(messageID).delete().addOnSuccessListener {
                receiverRef.document(messageID).delete().addOnSuccessListener {
                    Toast.makeText(this ,
                        getString(R.string.message_deleted_successfully) ,
                        Toast.LENGTH_SHORT)
                        .show() // Delete message from local list and RecyclerView
                    msgList.removeIf { it.messageID == messageID }
                    recyclerViewAdapter.notifyDataSetChanged()
                }
            }
            if (messageImage != null) {
                deletePostImage(messageImage)
            }
        }
        alert.setNegativeButton(R.string.cancel) { dialog , _ ->
            dialog.dismiss()
        }
        alert.show()
    }

    // Resim dosyasını silme fonksiyonu
    private fun deletePostImage(imageUrl : String?) {
        val storageReference = Firebase.storage.getReferenceFromUrl(imageUrl !!)
        storageReference.delete().addOnCompleteListener { storageTask ->
            if (storageTask.isSuccessful) {
                return@addOnCompleteListener
            } else {
                return@addOnCompleteListener
            }
        }
    }


    // Giphy actions
    override fun didSearchTerm(term : String) {
    // Arama çubuğuna arama terimi girildiğinde ne yapacağınızı buraya yazabilirsiniz.
    }

    override fun onDismissed(selectedContentType : GPHContentType) {
    // Giphy iletişim kutusu kapatıldığında ne yapacağınızı buraya yazabilirsiniz.
    }

    override fun onGifSelected(
        media : Media ,
        searchTerm : String? ,
        selectedContentType : GPHContentType ,
    ) {
        // Kullanıcı bir GIF seçtiğinde, gif'in uri'sini değişkene atayın
        selectedGif = Uri.parse(media.images.fixedWidth?.gifUrl)

       // Değişken boş değilse uri'yi işleyin ve mesaj öğelerini görünür yapın
        selectedGif?.let { imageUri ->
            Glide.with(this).load(imageUri).into(binding.messageImage)
            binding.messageImage.visibility = View.VISIBLE
        }

    }

    // Görsel seçim fragmentinden gelen verileri dinleyin
    override fun onBottomSheetResult(data : Uri) {
        selectedImage = data
        binding.messageImage.setImageURI(data)
        binding.messageImage.visibility = View.VISIBLE
    }

    // MessageAdapterda tanımladığımız dinleyici fonksiyonunu çağır ve popup menü aç
    override fun onMessageClick(message : Message , view : View) {
        val popupMenu = PopupMenu(this , view)
        popupMenu.inflate(R.menu.menu_message_action)
        popupMenu.setForceShowIcon(true)
        if (message.senderID != myID) {
            popupMenu.menu[2].setVisible(false)
            popupMenu.menu[3].setVisible(false)
        } else {
            popupMenu.menu[2].setVisible(true)
            popupMenu.menu[3].setVisible(true)
        }
        if (message.messageImage.isNullOrBlank()) {
            popupMenu.menu[1].setVisible(false)
        } else {
            popupMenu.menu[1].setVisible(true)
        }
        if (message.messageContent.isNullOrBlank()){
            popupMenu.menu[0].setVisible(false)
        }else{
            popupMenu.menu[0].setVisible(true)
        }

        popupMenu.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onMenuItemClick(item : MenuItem?) : Boolean {
                when (item?.itemId) {
                    R.id.mCopyMessage -> {
                        val clipboard = ContextCompat.getSystemService(this@ChatPage, ClipboardManager::class.java)
                        val clip = ClipData.newPlainText(getString(R.string.message),message.messageContent)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(applicationContext,"Mesaj Kopyalandı",Toast.LENGTH_LONG).show()
                        popupMenu.dismiss()
                        return true
                    }

                    R.id.mSaveImage -> {
                        Glide.with(this@ChatPage).asBitmap()
                            .load(message.messageImage) // Non-null Firebase'den gelen link
                            .into(object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(
                                    resource : Bitmap ,
                                    transition : Transition<in Bitmap>? ,
                                ) {
                                    val savedImageUri =
                                        saveImageToGallery(resource , message.messageID !!)

                                    // `Snackbar` göster
                                    val snackbar = Snackbar.make(view ,
                                        "Resim galeriye kaydedildi" ,
                                        Snackbar.LENGTH_LONG)
                                    snackbar.setAction("Göster") { // Kullanıcının galerisini aç
                                        showInGallery(savedImageUri)
                                    }
                                    snackbar.show()
                                }

                                override fun onLoadCleared(placeholder : Drawable?) { // Placeholder temizlendiğinde yapılacak işlemler
                                }
                            })
                        popupMenu.dismiss()
                        return true
                    }

                    R.id.mDelete -> {
                        if (message.messageImage != null) {
                            deleteMyMessage(message.messageID !! , message.messageImage)
                        } else {
                            deleteMyMessage(message.messageID !! , null)
                        }
                        popupMenu.dismiss()
                        return true
                    }

                    R.id.mDeleteAll -> {
                        if (message.messageImage != null) {
                            deleteThisMessage(message.messageID !! , message.messageImage)
                        } else {
                            deleteThisMessage(message.messageID !! , null)
                        }
                        popupMenu.dismiss()
                        return true
                    }

                    R.id.mCancel -> {
                        popupMenu.dismiss()
                        return true
                    }
                }
                return false
            }
        })
        popupMenu.show()
    }


    // Görseli galeriye kaydetme fonksiyonu
    fun saveImageToGallery(bitmap : Bitmap , displayName : String) : Uri? {
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME , "$displayName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE , "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING , 1)
            }
        }

        val contentResolver = this.contentResolver

        // Resmi MediaStore'a ekle
        val imageUri : Uri? = contentResolver.insert(imageCollection , contentValues)

        imageUri?.let { uri -> // Resmi kaydet
            contentResolver.openOutputStream(uri).use { outputStream ->
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG , 100 , outputStream)
                }
            }

            // Android Q ve üzeri için resmi "işlemde" olarak işaretleme işlemini tamamla
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING , 0)
                contentResolver.update(uri , contentValues , null , null)
            }
        }
        return imageUri
    }

    // Resim indirildikten sonra resmi görüntülemek için fonksiyon
    fun showInGallery(imageUri : Uri?) {
        imageUri?.let { uri ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri , "image/*")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(intent)
        }
    }
}