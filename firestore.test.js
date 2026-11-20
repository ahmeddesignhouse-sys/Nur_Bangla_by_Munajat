const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write bookmarks or preferences", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("bookmarks").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("preferences").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("prayer_logs").get()
  );
});

test("Removed prayer_logs collection is denied even for authenticated owner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("prayer_logs").get()
  );
});

test("Cross-user access to bookmarks and preferences is denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const now = new Date(Date.now() - 5000);
    await ctx
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("bookmarks")
      .doc("1_1")
      .set({
        userId: BOB_UID,
        surahNumber: 1,
        ayahNumber: 1,
        surahNameBn: "আল-ফাতিহা",
        arabicText: "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        banglaText: "পরম করুণাময় অসীম দয়ালু আল্লাহর নামে।",
        createdAt: now,
        updatedAt: now,
      });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("bookmarks").get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("bookmarks")
      .doc("1_1")
      .delete()
  );
});

test("Owner can manage bookmarks and user preferences including languageCode", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bookmarks")
      .doc("112_1")
      .set({
        userId: ALICE_UID,
        surahNumber: 112,
        ayahNumber: 1,
        surahNameBn: "আল-ইখলাস",
        arabicText: "قُلْ هُوَ اللَّهُ أَحَدٌ",
        banglaText: "বলুন, তিনি আল্লাহ, এক ও অদ্বিতীয়।",
        createdAt: now,
        updatedAt: now,
      })
  );

  const prefRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("preferences")
    .doc("user_settings");

  await assertSucceeds(
    prefRef.set({
      userId: ALICE_UID,
      districtId: "dhaka",
      asrMethod: "HANAFI",
      themeMode: "DARK",
      accentTheme: "ISLAMIC_GREEN",
      quranDarkMode: true,
      quranFontSize: 20,
      prePrayerMinutes: 15,
      languageCode: "bn",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    prefRef.update({
      languageCode: "en",
      accentTheme: "MEDINA_TEAL",
      prePrayerMinutes: 10,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Invalid languageCode rejected
  await assertFails(
    prefRef.update({
      languageCode: "fr",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Ghost field rejected
  await assertFails(
    prefRef.update({
      unknownField: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
