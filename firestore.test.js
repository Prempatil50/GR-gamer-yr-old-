const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_grgamer_123";
const BOB_UID = "bob_grgamer_456";
const DB_ID = "ai-studio-android-grgamera-e0c01577-93d4-4aa2-b8bb-9244a95881ce";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const DEFAULT_PROMPT =
  "You are GR GAMER AI, an intelligent gaming YouTube assistant. You manage comments for a Free Fire gaming YouTube channel. Reply naturally, politely, and enthusiastically. Understand every comment before replying. Use Hindi, Hinglish, or English according to the comment. Keep replies short, friendly, engaging, and relevant. Never generate spam, abusive content, fake promises, or misleading information. Avoid repeating the same reply.";

function aliceFirestore() {
  return testEnv.authenticatedContext(ALICE_UID).firestore(DB_ID);
}

function bobFirestore() {
  return testEnv.authenticatedContext(BOB_UID).firestore(DB_ID);
}

function unauthFirestore() {
  return testEnv.unauthenticatedContext().firestore(DB_ID);
}

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

test("Unauthenticated user cannot read or write any GR GAMER AI collection", async () => {
  const db = unauthFirestore();
  await assertFails(
    db.collection("users").doc(ALICE_UID).collection("channel_config").doc("active").get()
  );
  await assertFails(
    db.collection("users").doc(ALICE_UID).collection("ai_settings").doc("active").get()
  );
  await assertFails(
    db.collection("users").doc(ALICE_UID).collection("comments").get()
  );
  await assertFails(
    db.collection("users").doc(ALICE_UID).collection("reply_history").get()
  );
  await assertFails(
    db.collection("users").doc(ALICE_UID).collection("channel_config").doc("active").set({
      userId: ALICE_UID,
      connected: true,
      channelId: "UC123",
      channelTitle: "GR GAMER",
      channelHandle: "@grgamer",
      thumbnailUrl: "https://example.com/a.jpg",
      subscriberCount: 1000,
      videoCount: 50,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can create and update channel_config; cross-user and ghost-field writes fail", async () => {
  const aliceDb = aliceFirestore();
  const bobDb = bobFirestore();
  const now = new Date(Date.now() - 5000);

  const channelDoc = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("channel_config")
    .doc("active");

  await assertSucceeds(
    channelDoc.set({
      userId: ALICE_UID,
      connected: true,
      channelId: "UC_GR_GAMER_01",
      channelTitle: "GR GAMER LIVE",
      channelHandle: "@grgamerlive",
      thumbnailUrl: "https://yt3.ggpht.com/avatar.jpg",
      subscriberCount: 125000,
      videoCount: 340,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    channelDoc.update({
      connected: false,
      channelId: "",
      channelTitle: "",
      channelHandle: "",
      thumbnailUrl: "",
      subscriberCount: 0,
      videoCount: 0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user read/write denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("channel_config").doc("active").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("channel_config").doc("active").update({
      connected: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Ghost field / @DocumentId `id` injection denied
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("channel_config").doc("other").set({
      id: "other",
      userId: ALICE_UID,
      connected: true,
      channelId: "UC_X",
      channelTitle: "X",
      channelHandle: "@x",
      thumbnailUrl: "",
      subscriberCount: 0,
      videoCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Owner can create ai_settings and perform action-based partial updates", async () => {
  const aliceDb = aliceFirestore();
  const now = new Date(Date.now() - 5000);
  const settingsRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("ai_settings")
    .doc("active");

  await assertSucceeds(
    settingsRef.set({
      userId: ALICE_UID,
      autoReplyActive: false,
      ignoreSpam: true,
      ignoreAbusive: true,
      avoidDuplicates: true,
      autoDetectLanguage: true,
      preferredLanguage: "AUTO",
      enabledLanguages: ["ENGLISH", "HINDI", "HINGLISH"],
      replyTone: "HYPE_GAMER",
      maxReplyLength: 180,
      pollingIntervalSeconds: 30,
      customPrompt: DEFAULT_PROMPT,
      totalCommentsReceived: 0,
      totalRepliesSent: 0,
      pendingCommentsCount: 0,
      failedRepliesCount: 0,
      skippedSpamCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Toggle auto-reply
  await assertSucceeds(
    settingsRef.update({
      autoReplyActive: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Language settings update
  await assertSucceeds(
    settingsRef.update({
      autoDetectLanguage: false,
      preferredLanguage: "HINGLISH",
      enabledLanguages: ["HINGLISH", "ENGLISH"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Prompt settings update
  await assertSucceeds(
    settingsRef.update({
      customPrompt: DEFAULT_PROMPT + " Always wish viewers Booyah!",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Stats update
  await assertSucceeds(
    settingsRef.update({
      totalCommentsReceived: 5,
      totalRepliesSent: 3,
      pendingCommentsCount: 1,
      failedRepliesCount: 0,
      skippedSpamCount: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject future timestamp & invalid prompt length & immutable createdAt mutation
  await assertFails(
    settingsRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    settingsRef.update({
      customPrompt: "short",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    settingsRef.update({
      autoReplyActive: false,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Owner can manage comments and reply_history; cross-user access fails", async () => {
  const aliceDb = aliceFirestore();
  const bobDb = bobFirestore();
  const now = new Date(Date.now() - 5000);

  const commentRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("comments")
    .doc("UgxComment_123");

  await assertSucceeds(
    commentRef.set({
      userId: ALICE_UID,
      youtubeCommentId: "UgxComment_123",
      videoId: "vid_ff_01",
      videoTitle: "Free Fire Ranked Booyah Highlights",
      authorName: "ProSniperYT",
      authorAvatarUrl: "https://example.com/avatar.png",
      commentText: "Bhai OP headshot tha! Sensitivity batao please",
      detectedLanguage: "HINGLISH",
      status: "PENDING",
      generatedReply: "",
      failureReason: "",
      publishedAtIso: "2026-11-17T10:00:00Z",
      repliedAtIso: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    commentRef.update({
      detectedLanguage: "HINGLISH",
      status: "REPLIED",
      generatedReply: "Shukriya bhai! Red dot 95 aur general 100 rakho, Booyah pakka!",
      failureReason: "",
      repliedAtIso: "2026-11-17T10:01:00Z",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  const historyRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("reply_history")
    .doc("UgxComment_123");

  await assertSucceeds(
    historyRef.set({
      userId: ALICE_UID,
      youtubeCommentId: "UgxComment_123",
      videoId: "vid_ff_01",
      videoTitle: "Free Fire Ranked Booyah Highlights",
      authorName: "ProSniperYT",
      originalComment: "Bhai OP headshot tha! Sensitivity batao please",
      aiReply: "Shukriya bhai! Red dot 95 aur general 100 rakho, Booyah pakka!",
      languageUsed: "HINGLISH",
      status: "SENT",
      errorMessage: "",
      postedToYoutube: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Owner list queries succeed
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("comments").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reply_history").get()
  );

  // Cross-user list and read fail
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("comments").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("reply_history").get()
  );
});
