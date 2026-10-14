# Hill Climb Racing - 2D Physics Racing Game

A complete 2D physics-based side-scrolling hill climbing racing game featuring realistic suspension dynamics, collectible coins, fuel management, endless procedural terrain, and vehicle upgrades.

---

## 🎮 Play Directly Online (GitHub Pages)
If you enable GitHub Pages on this repository, you can play the game instantly in your browser on PC or Mobile!

### How to Fix GitHub 404 Error:
If you are seeing a **404 Not Found** error on your GitHub Pages URL, follow these simple steps to activate it:
1. Go to your repository on **GitHub.com**.
2. Click on **Settings** (tab at the top right of the repo).
3. In the left sidebar, click on **Pages**.
4. Under **Build and deployment**:
   - **Source**: Select `Deploy from a branch`.
   - **Branch**: Choose `main` (or `master`) and select `/ (root)`.
   - Click **Save**.
5. Wait 1–2 minutes for GitHub Pages to deploy. Once the green checkmark appears, your website link (`https://<username>.github.io/<repo>/`) will open the game directly!

---

## 🚀 Game Features

- **Stable 50-Meter Runway**: Flat starting terrain that ensures safe spawn and smooth acceleration without flipping.
- **Endless Procedural Driving**: Continuous infinite-runner terrain with hills, jumps, chasms, and obstacles.
- **8 Selectable Vehicles**:
  - **Classic Red Jeep**: Free starter 4x4 with balanced stability.
  - **Motocross Bike**: Agile two-wheeler for backflips and frontflips.
  - **Monster Truck**: Big bouncy tires that crush obstacles.
  - **Hippie Van**: Retro flower-power bus with bouncy shocks.
  - **One-wheeler**: Unicycle balance challenge with huge stunt multipliers.
  - **Turbo Rally Car**: Aerodynamic speedster with fast acceleration.
  - **Army Tank**: Heavy armored continuous-tread powerhouse.
  - **Luxury Supercar**: High-downforce twin-turbo hypercar.
- **6 Diverse Stages**:
  - **Countryside** (Lush green hills, wooden bridges, 1.0x grip)
  - **Desert** (Sandy dunes, pyramids, warm sun)
  - **Arctic** (Icy slippery glaciers, 0.65x grip)
  - **Moon** (Low gravity 3.6 m/s², massive high-flying jumps)
  - **Cave** (Underground caverns, bumpy rocks, glowing crystals)
  - **Highway** (Smooth asphalt flyovers, city skyline, 1.25x grip)
- **Economy & Upgrades**:
  - Collect coins in values of **5, 10, 50, 100, and 500**.
  - Red Fuel Jerrycans placed periodically along the road.
  - Upgrade **Engine**, **Suspension**, **Tires**, and **4WD/Fuel**.

---

## 🕹️ Controls

- **GAS (Accelerate / Backflip in air)**:
  - Touch: Right on-screen pedal
  - Keyboard: `Up Arrow`, `Right Arrow`, `W`, or `D`
- **BRAKE (Brake & Reverse / Frontflip in air)**:
  - Touch: Left on-screen pedal
  - Keyboard: `Down Arrow`, `Left Arrow`, `S`, or `A`

---

## 📱 Platforms Included

1. **Android App (`app/`)**: Full Jetpack Compose Android app built with Kotlin, custom Canvas renderer, and native multi-touch pedals.
2. **Web Edition (`index.html`)**: Instant browser-playable HTML5 Canvas version ready for GitHub Pages.
3. **Python Script (`hill_climb_racing.py`)**: Standalone Pygame script. Run with `python hill_climb_racing.py`.
