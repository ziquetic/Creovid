# Creovid

Phone ke liye cinematic colour grading app. Free, bina watermark, bina login.

## Features
- 12 cinematic looks (Teal & Orange, Blockbuster, Moody Blue, Golden Hour, Vintage Film, Noir B&W, Cyberpunk, Bleach Bypass, Matte Cinema, Emerald Forest, Day for Night, Dream Glow)
- Apni LUT import (.cube, 3D) + strength slider
- Basic: exposure, contrast, highlights, shadows, temperature, tint, saturation, vibrance, fade, film curve
- Colour wheels: shadows / midtones / highlights
- FX: film grain, bloom, halation, vignette, letterbox (2.39:1, 2:1, 1.85:1)
- "Dabake dekho" button: dabaye rakho to original video dikhti hai
- Export: 720p / 1080p / Original, gallery me Movies/Creovid folder me save

## APK kaise banaye (GitHub Actions)
1. Is poore folder ko apni GitHub repo me daalo (main branch).
2. Repo me **Actions** tab kholo, **Build Creovid APK** chunke **Run workflow** dabao (push par apne aap bhi chalta hai).
3. 5-8 minute baad run ke andar **Artifacts** me **Creovid-APK** milega. Download karke zip kholo aur `app-release.apk` install karo.

Agar phone se upload karte waqt `.github` folder na jaye, to repo me **Add file > Create new file** karo, naam likho `.github/workflows/build-apk.yml` aur `build-apk.yml` ka text paste karo.

## Technical
- Kotlin + Jetpack Compose
- Media3 (ExoPlayer preview + Transformer export)
- Grading poora GPU shader me (OpenGL ES): preview aur export dono me wahi shader chalta hai
- minSdk 29 (Android 10+)
