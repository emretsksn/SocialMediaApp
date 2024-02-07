# SocialApp
Sosyal medya uygulaması

# Uygulama içinden görüntüler
![SocialMediaApp](https://github.com/emretsksn/SocialMediaApp/assets/58102146/8a7e265e-6e4c-4135-8823-efeb64a2f433)

# Proje Hakkında
Proje bir sosyal medya uygulaması olarak oluşturulmuştur.
Yazılı ve görsel paylaşım yapabilmektedir.
Uygulama içi bireysel mesajlaşma ve grup mesajlaşma özelliği mevcuttur.
Mesajlarda görsel paylaşım, yer bildirimi ve gif paylaşımı yapılabilmektedir.
Paylaşımlarda yer bildirimi, kullanıcı etiketleme yapılabilmektedir.
Kamera aksiyonu uygulama içinde oluşturulan aktivite ile sağlanmaktadır.
Projede maps api ve places api kullanılarak harita üzerinden lokasyon seçimi ve paylaşımı yapılabilmektedir.
Cloud Functions ile mesaj, etiketleme, beğeni, yorum gibi işlemlerde bildirim gönderilmektedir.
Arayüz tasarımı ve özellikler [Twitter](https://play.google.com/store/apps/details?id=com.twitter.android&hl=tr&gl=US), [Instagram](https://play.google.com/store/apps/details?id=com.instagram.android), [Threads](https://play.google.com/store/apps/details?id=com.instagram.barcelona) gibi sosyal medya uygulamalarından esinlenerek kodlanmıştır.
Ayarlar menüsü eklenmiş ve ayarlarda tema, bildirim, hesap gizliliği, aktivite durumu gibi eylemler kontrol edilebilmektedir.

Projeyi indirip kullanmak isterseniz;
[Firebase](https://console.firebase.google.com/) üzerinden bir proje oluşturun.
Projenizde Authentication, Cloud Firestore, Storage özelliklerini aktif ediniz.
console.cloud.google.com linki üzerinden Maps SDK for Android ve Places API apilerini aktif ediniz.
Firebase üzerinden indirmiş olduğunuz google-services.json dosyasını projede [app](https://github.com/emretsksn/SocialMediaApp/tree/master/app) klasörüne kopyalayınız.

Bildirimler için firebase üzerinden Cloud Functions aktif edebilir ve [Cloud Functions](https://github.com/emretsksn/SocialMediaApp/blob/master/cloud_functions.js) linkindeki kodları deploy ederek kullanabilirsiniz.

# Kullanılan servis ve yapılar
- Firebase
- Google Maps API
- Google Places API

# Kullanılan kütüphaneler
- [uCrop](https://github.com/Yalantis/uCrop)
- [android-gif-drawable](https://github.com/koral--/android-gif-drawable)
- [CircleImageView](https://github.com/hdodenhof/CircleImageView)
- [Glide](https://github.com/bumptech/glide)
- [KameraX](https://developer.android.com/jetpack/androidx/releases/camera?hl=tr)

# Kullanılan kaynaklar
- [GeeksforGeeks](www.geeksforgeeks.org)
- [Android Mobil Uygulama Geliştirme Eğitimi](https://www.udemy.com/course/android-mobil-uygulama-gelistirme-egitimi-kotlin)
- [Geliştiriciler için Android](https://developer.android.com/?hl=tr)
- [Kotlin İle Android Mobil Uygulama Geliştirme Temel Seviye](https://www.btkakademi.gov.tr/portal/course/kotlin-ile-android-mobil-uygulama-gelistirme-temel-seviye-10274)
- [Firebase ile Proje Geliştirme](https://www.btkakademi.gov.tr/portal/course/firebase-ile-proje-gelistirme-15059)
- [ChatGPT 3.5](https://chat.openai.com/)
- [back4app Agent](https://www.back4app.com/agent)

# Not
- Projede revize edilmesi gereken alanlar mevcut olup, proje ham haliyle paylaşılmıştır.
- MVVM yapısı kullanılmamış olup, geleneksel yöntemlerle yazılmıştır.
- Projeye 4 ay önce sıfır bilgi ile başladığım için eksiklerim çok fazla.
- Güncel bilgilerim ve tecrübelerimle yeni bir projeye başladım ve MVVM mimarisi ile ilerliyorum.

# Teşekkür
[Kasım Adalan](https://www.linkedin.com/in/kas%C4%B1m-adalan/) ve [Atıl Samancıoğlu](https://www.linkedin.com/in/at%C4%B1l-samanc%C4%B1o%C4%9Flu-96028871/) beye özellikle teşekkürlerimi iletiyorum. Android alanında kendimi geliştirmem adına yayınladıkları kursların çok faydasını gördüm.
