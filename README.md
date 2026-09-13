# ArenaStrike

Real-time multiplayer tactical 3D battleground web game.

## Project structure

- `backend/` - Spring Boot API, STOMP/WebSocket endpoint, and persistence layer.
- `frontend/` - HTML5/JavaScript client and Three.js rendering boundary.
- `assets/models/` - supplied GLB game models.

## Project Run Guide (কিভাবে রান করবেন)

প্রজেক্টটি রান করার জন্য আপনাকে ব্যাকএন্ড (Spring Boot) এবং ফ্রন্টএন্ড (HTML/JS) দুটোই রান করতে হবে। নিচে বিস্তারিত গাইডলাইন দেওয়া হলো:

### ১. ডেটাবেস সেটআপ (Database Setup)
- আপনার লোকাল মেশিনে MySQL চালু করুন (XAMPP বা অন্য কোনো মাধ্যমে)।
- `arenastrike` নামে একটি নতুন ডেটাবেস তৈরি করুন। (Create a MySQL database named `arenastrike`).
- যদি আপনার MySQL এর ইউজারনেম এবং পাসওয়ার্ড `root` এবং ফাঁকা না হয়, তবে এনভায়রনমেন্ট ভেরিয়েবল হিসেবে `DB_USERNAME` এবং `DB_PASSWORD` সেট করে নিন।

### ২. ব্যাকএন্ড রান করা (Run Backend)
- টার্মিনাল বা কমান্ড প্রম্পট ওপেন করে প্রজেক্টের `backend/` ফোল্ডারে যান।
- নিচের কমান্ডটি রান করুন:
  ```bash
  .\gradlew bootRun
  ```
- এটি Spring Boot অ্যাপ্লিকেশনটি চালু করবে।

### ৩. ফ্রন্টএন্ড রান করা (Run Frontend)
- ফ্রন্টএন্ড এর ফাইলগুলো ব্রাউজারে সরাসরি `file://` দিয়ে ওপেন করলে ঠিকমত কাজ করবে না। আপনাকে একটি লোকাল সার্ভার ব্যবহার করতে হবে।
- টার্মিনাল ওপেন করে প্রজেক্টের `frontend/` ফোল্ডারে যান।
- যদি আপনার `python` ইনস্টল করা থাকে তবে নিচের কমান্ডটি দিন:
  ```bash
  python -m http.server 3000
  ```
  অথবা `Node.js` থাকলে নিচের কমান্ডটি ব্যবহার করতে পারেন:
  ```bash
  npx http-server -p 3000
  ```
- এরপর ব্রাউজারে গিয়ে `http://localhost:3000` লিখে এন্টার দিন। গেইমটি চালু হয়ে যাবে!
