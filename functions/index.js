/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const {setGlobalOptions} = require("firebase-functions");


// For cost control, you can set the maximum number of containers that can be
// running at the same time. This helps mitigate the impact of unexpected
// traffic spikes by instead downgrading performance. This limit is a
// per-function limit. You can override the limit for each function using the
// `maxInstances` option in the function's options, e.g.
// `onRequest({ maxInstances: 5 }, (req, res) => { ... })`.
// NOTE: setGlobalOptions does not apply to functions using the v1 API. V1
// functions should each use functions.runWith({ maxInstances: 10 }) instead.
// In the v1 API, each function can only serve one request per container, so
// this will be the maximum concurrent request count.
setGlobalOptions({maxInstances: 10});

const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.notifyAdminsOnNewUser = functions.firestore
    .document("Users/{userId}")
    .onCreate(async (snap, context) => {
      const newUser = snap.data();

      // Only notify if approval is pending
      if (newUser.isApproved === "pending") {
        const payload = {
          notification: {
            title: "New Account Request",
            body: `${newUser.FullName} has requested an account.`,
          },
        };

        // Get all admins
        const adminQuery = await admin
            .firestore()
            .collection("Users")
            .where("isAdmin", "==", true)
            .get();

        const tokens = [];
        adminQuery.forEach((doc) => {
          const token = doc.data().fcmToken;
          if (token) tokens.push(token);
        });

        if (tokens.length > 0) {
          return admin.messaging().sendToDevice(tokens, payload);
        } else {
          console.log("No admin tokens available.");
          return null;
        }
      }

      return null;
    });
