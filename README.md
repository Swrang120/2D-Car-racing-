# Hill Climb Racing - 2D Physics Racing Game (Android & Web)

A complete 2D physics-based side-scrolling hill climbing racing game featuring realistic suspension dynamics, collectible coins, fuel management, endless procedural terrain, garage upgrades, and multiple vehicles & stages.

---

## 🌐 GitHub Pages Live Website (404 Error Fix / कैसे ठीक करें)

यदि GitHub Pages पर **404 Not Found** आ रहा है, तो अब यह पूरी तरह हल (fix) हो चुका है:

### तरीका 1 (Automatic via GitHub Actions):
1. अपने GitHub Repository में जाएँ।
2. **Settings** -> **Pages** में जाएँ।
3. **Build and deployment** -> **Source** में **GitHub Actions** चुनें।
4. बस! GitHub Actions अपने आप `index.html` को डिप्लॉय कर देगा और आपकी वेबसाइट बिना किसी 404 के तुरंत खुल जाएगी।

### तरीका 2 (Deploy from a branch):
1. अपने GitHub Repository में **Settings** -> **Pages** में जाएँ।
2. **Build and deployment** -> **Source** में `Deploy from a branch` चुनें।
3. **Branch** में `main` (या `master`) चुनें, और फोल्डर में `/ (root)` या `/docs` दोनों में से कोई भी चुनें (दोनों जगह `index.html` और `.nojekyll` मौजूद है)।
4. **Save** पर क्लिक करें। 1-2 मिनट में आपकी वेबसाइट लाइव हो जाएगी!

---

## 🚗 Features (App और Web दोनों में 100% Same)

1. **50-Meter Flat Runway**:
   - शुरुआत में गाड़ी कभी नहीं पलटेगी (Zero flip at spawn). 50 मीटर तक बिल्कुल सपाट रोड।
2. **Endless Infinite Runner Mode**:
   - कोई 600m की लिमिट नहीं — जब तक आप ड्राइव करेंगे, पहाड़ियां, रैम्प, और पुल लगातार जनरेट होते रहेंगे।
3. **8 Vehicles**:
   - **Classic Red Jeep** (Free Starter)
   - **Motocross Dirt Bike** (Agile flipper)
   - **Monster Truck** (Bouncy giant tires)
   - **Hippie Van** (Flower-power camper)
   - **One-wheeler** (Unicycle master)
   - **Turbo Rally Car** (Low gravity aerodynamic speed)
   - **Army Tank** (Heavy powerhouse)
   - **Luxury Supercar** (Hypercar downforce)
4. **6 Stages**:
   - **Countryside** (हरी-भरी पहाड़ियां और पुल)
   - **Desert** (रेगिस्तानी रेत और पिरामिड)
   - **Arctic** (बर्फ़ीली फिसलन भरी चट्टानें)
   - **Moon** (कम गुरुत्वाकर्षण - 3.6 m/s² में ऊंची छलांग)
   - **Cave** (भूमिगत गुफा और क्रिस्टल)
   - **Highway** (स्मूथ हाईवे और फ़्लाईओवर)
5. **Coin Economy & Upgrades**:
   - सिक्के: **5, 10, 50, 100, और 500** कॉइन्स।
   - लाल फ़्यूल जेरीकैन (Fuel canisters).
   - वाहन अपग्रेड: Engine, Suspension, Tires, 4WD/Fuel.

---

## 🕹️ Controls (नियंत्रण)

- **Mobile**: स्क्रीन पर दाएं तरफ **GAS** और बाएं तरफ **BRAKE** पेडल।
- **Keyboard (PC Web)**:
  - **GAS**: `Arrow Right`, `Arrow Up`, `W`, या `D`
  - **BRAKE / REVERSE**: `Arrow Left`, `Arrow Down`, `A`, या `S`
  - हवा में स्टंट: Gas दबाने से Backflip, Brake दबाने से Frontflip.
