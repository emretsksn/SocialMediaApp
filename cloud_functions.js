const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();


// Notification belgesi oluşturulduğunda tetiklenir
exports.sendUserNotification = functions.firestore
    .document('Users/{userID}/Notification/{notificationID}')
    .onCreate((snapshot, context) => {
        const notificationInfo = snapshot.data();
        const userID = context.params.userID;
        const notificationTitle = notificationInfo.notificationTitle;
        const notificationContent = notificationInfo.notificationContent;
        const notificationType = notificationInfo.notificationType;
        const trackID = notificationInfo.trackID
        // İkonu ayarla
        let type = '';
        switch (notificationType) {
            case 'commentNotification':
                type = 'cmmt';
                break;
            case 'likeNotification':
                type = 'like';
                break;
            case 'followNotification':
                type = 'flw';
                break;
            case 'tagNotification':
                type = 'tag'
                break;
        }

        return admin.firestore().collection('Users').doc(userID).get()
            .then(userDoc => {
                if (!userDoc.exists) {
                    throw new Error('User not found');
                }
                userFcmToken = userDoc.data().fcmToken;
                receiverUserName = userDoc.data().userName;

                if (!userFcmToken) {
                    throw new Error('No FCM token');
                }

                // Bildirim oluşturan kullanıcının bilgilerini al
                return admin.firestore().collection('Users').doc(trackID).get();
            })
            .then(trackUserDoc => {
                if (!trackUserDoc.exists) {
                    throw new Error('Track user not found');
                }

                const trackUserData = trackUserDoc.data();
                trackUserProfileImage = trackUserData.profileImage || null;
                senderUserName = trackUserData.userName;

                // Bildirim mesajını hazırla, profil resmi varsa eklemeyi yap
                const dataPayload = {
                    title: notificationTitle, // `data` içinde başlık
                    body: notificationContent, // `data` içinde içerik
                    userIDs: trackID,
                    messageType : type,
                    // Profil resmi varsa ekleyin, yoksa boş bırakın
                    profileImage: trackUserProfileImage ? trackUserProfileImage : ''
                };

                // 'notification' anahtarını payload'dan kaldırdık, yalnızca 'data' kullanıyoruz
                const payload = {
                    data: dataPayload
                };

                // FCM kullanarak kullanıcıya bildirim gönder
                return admin.messaging().sendToDevice(userFcmToken, payload);
            })
            .then(response => {
                console.log('Bildirim başarılı bir şekilde gönderildi:', response);
                return null;
            })
            .catch(error => {
                console.error('Hata:', error);
                return null;
            });
});


// Notification belgesi oluşturulduğunda tetiklenir
exports.sendMessageNotification = functions.firestore
    .document('Users/{userID}/MessageNotification/{notificationID}')
    .onCreate((snapshot, context) => {
        const notificationInfo = snapshot.data();
        const userID = context.params.userID;
        const trackID = notificationInfo.trackID;

        let userFcmToken;
        let trackUserProfileImage;
        let receiverUserName;
        let senderUserName;

        // Kullanıcının FCM tokenını ve bildirimi oluşturan kullanıcının profil resmini al
        return admin.firestore().collection('Users').doc(userID).get()
            .then(userDoc => {
                if (!userDoc.exists) {
                    throw new Error('User not found');
                }
                userFcmToken = userDoc.data().fcmToken;
                receiverUserName = userDoc.data().userName;

                if (!userFcmToken) {
                    throw new Error('No FCM token');
                }

                // Bildirim oluşturan kullanıcının bilgilerini al
                return admin.firestore().collection('Users').doc(trackID).get();
            })
            .then(trackUserDoc => {
                if (!trackUserDoc.exists) {
                    throw new Error('Track user not found');
                }

                const trackUserData = trackUserDoc.data();
                trackUserProfileImage = trackUserData.profileImage || null;
                senderUserName = trackUserData.userName;

                // Bildirim mesajını hazırla, profil resmi varsa eklemeyi yap
                const dataPayload = {
                    title: receiverUserName, // `data` içinde başlık
                    body: senderUserName, // `data` içinde içerik
                    userIDs: trackID,
                    message: notificationInfo.notificationContent,
                    messageType : "message",
                    // Profil resmi varsa ekleyin, yoksa boş bırakın
                    profileImage: trackUserProfileImage ? trackUserProfileImage : ''
                };

                // 'notification' anahtarını payload'dan kaldırdık, yalnızca 'data' kullanıyoruz
                const payload = {
                    data: dataPayload
                };

                // FCM kullanarak kullanıcıya bildirim gönder
                return admin.messaging().sendToDevice(userFcmToken, payload);
            })
            .then(response => {
                console.log('Bildirim başarılı bir şekilde gönderildi:', response);
                return null;
            })
            .catch(error => {
                console.error('Hata:', error);
                return null;
            });
});
