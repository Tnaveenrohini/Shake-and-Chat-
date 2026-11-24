# 🫨 Shake and Chat

Phone ni shake chesthe — same language, same interests unna oka random person tho chat connect avutundi. Made in India 🇮🇳

## Features (v0.1)
- 📧 Email + password login 
- 👤 Profile: nickname, age (18+ only), gender, chat language, interests (1–5)
- 🫨 Shake to match — accelerometer, leda button tap
- 🎯 Matching: same language first, ekkuva common interests unna vaallu mundu
- 💬 Stranger chat — ⏭ Next, ➕ Add friend (iddaru nokkithene friends)
- 👥 Friends tab — friends tho permanent chat
- 🛡️ Safety: bad-word filter, strangers ki phone numbers / @IDs / links hide, Block, Report (report = auto block)

## Setup
Firebase project: **shake-and-chat-c45c8** (`app/google-services.json` already undi)
1. Authentication → Sign-in method → **Email/Password** enable
2. Firestore Database → Create database (Standard, asia-south1 Mumbai)
3. Firestore → **Rules** → ee repo lo unna `firestore.rules` motham copy-paste → Publish
4. Push chesthe GitHub Actions APK build chesi **Releases** lo pedutundi

## Firestore collections
`sc_users`, `sc_queue`, `sc_rooms/{id}/msgs`, `sc_reports`
