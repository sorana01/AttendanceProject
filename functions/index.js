const functions = require('firebase-functions/v1');
const admin = require('firebase-admin');
admin.initializeApp();

const db = admin.firestore();

exports.sendNotificationOnPendingUser = functions.firestore
  .document('Users/{userId}')
  .onCreate(async (snap, context) => {
    const newUser = snap.data();

    if (newUser.isApproved === 'pending') {
      // Fetch all admins
      const adminsSnapshot = await db.collection('Users')
        .where('isAdmin', '==', true)
        .get();

      const tokens = [];

      adminsSnapshot.forEach(adminDoc => {
        const token = adminDoc.data().fcmToken;
        if (token) tokens.push(token);
      });

      if (tokens.length === 0) {
        console.log('No admin tokens found.');
        return;
      }

      const message = {
        notification: {
          title: 'New User Registration',
          body: `${newUser.FullName} has requested an account.`,
        },
        tokens: tokens,
      };

      try {
        const response = await admin.messaging().sendMulticast(message);
        console.log('Notification sent successfully:', response);
      } catch (error) {
        console.error('Error sending notification:', error);
      }
    }

    return null;
  });
