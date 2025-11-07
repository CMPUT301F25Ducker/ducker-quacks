/* eslint object-curly-spacing: ["error", "always"] */
// functions/src/index.ts
import * as admin from "firebase-admin";
import {
  onCall,
  HttpsError,
  CallableRequest,
} from "firebase-functions/v2/https";

admin.initializeApp();

interface DeleteUserData {
  email: string;
}

export const deleteUserByEmail = onCall<DeleteUserData>(
  { region: "us-central1" },
  async (request: CallableRequest<DeleteUserData>) => {
    const { data } = request;

    // Optional admin check
    // if (!auth?.token?.isAdmin) {
    //   throw new HttpsError("permission-denied", "Admins only.");
    // }

    const email = data?.email;
    if (!email || typeof email !== "string") {
      throw new HttpsError(
        "invalid-argument",
        "A valid email must be provided.",
      );
    }

    try {
      const user = await admin.auth().getUserByEmail(email);
      await admin.auth().deleteUser(user.uid);

      // Firestore cleanup (by email)
      const db = admin.firestore();
      const snap = await db
        .collection("users")
        .where("email", "==", email)
        .get();
      const batch = db.batch();
      snap.forEach((doc) => batch.delete(doc.ref));
      await batch.commit();

      return { success: true, message: `User ${email} deleted successfully.` };
    } catch (err: unknown) {
      // Narrow error type safely without `any`
      const e = err as { code?: string; message?: string } | undefined;

      if (e?.code === "auth/user-not-found") {
        throw new HttpsError(
          "not-found",
          `User with email ${email} not found.`,
        );
      }
      throw new HttpsError("internal", e?.message ?? "Failed to delete user.");
    }
  },
);
