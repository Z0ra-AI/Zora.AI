# Zora.AI ◈ (v2.2.0)
Zora.AI is a native Android AI companion built by Phan Chi Vy. 

## The TL;DR
**FUCK corporate censorship. FUCK paywalls.**

I got sick of commercial AI apps locking basic conversations behind subscriptions or lecturing me about "community guidelines" every time a chat got interesting. Zora.AI is the antidote. It is a sandbox of pure creative freedom where adult content, dark roleplay, and extreme fictional scenarios are fully allowed.

I’m a responsible developer, though—don't use this platform to build weapons, plan crimes, or recreate 9/11 (っ °Д °;)っ. Otherwise? Write whatever the fuck you want.

Best of all: it's a **bring-your-own-key** model. You run it on your own API keys. No middleware, no training on your private chats, and no paying me a single cent.

### The Three Rules of Zora:
* 🚫 **NO Corporate Filters** — The model does what you tell it to do. Period.
* 💸 **NO Paywalls** — 100% free and open-source.
* 🔒 **NO Data Harvesting** — Your API keys, chat history, and visual frames stay locally on your device.

*Note: While Zora is a highly capable assistant for coding and daily tasks, it is custom-tuned to be the ultimate sandbox for interactive roleplay.*

---

## 🔞 NSFW & Frontier Model Jailbreak
**WARNING**: Zora.AI features built-in jailbreak configurations for frontier-line models. 
* **What it does**: Bypasses moralizing filters specifically to unlock NSFW 18+ content, mature storytelling, and unfiltered creative expression.
* **What it blocks**: The system still enforces strict safety guards against dangerous/sketchy stuff (e.g. weapons, extreme real-world harm, self-harm instructions).
* **Use at your own risk**: Using these configurations might violate the Terms of Service of your selected API provider. Your API keys could get banned if their automated systems flag them. Don't say I didn't warn you!
* **Footnote**: To keep this repository clean and safe, the private NSFW-specific system prompts are *not* included or hardcoded in this repo. If you brew or build this app on your own, you'll need to write or supply your own master prompt, otherwise it will fall back to standard SFW behavior.

### Model Risk Ranking (Lowest to Highest Risk of being banned):
Based on usage testing (no keys were banned during development, but your mileage may vary):
```text
Grok 4.3 (Lowest Risk) ──> Gemini 3.5 ──> GPT-5.5 ──> Claude 4.8 (Highest Risk)
```
*(We recommend starting with Gemini or Mistral APIs for optimal stability and cost efficiency).*

---

## 🌟 What's New in v2.2.0
* 🎭 **Modern Roleplay UI**: A gorgeous, immersive card-based interface tailored for companion chats. You can seamlessly switch between this and the classic **Legacy UI** via the Settings page.
* 🇻🇳 **Vietnamese Language Support**: Full app localization in Vietnamese (`Tiếng Việt`) keeping community-specific terms intact.
* 📂 **Zora Share (.zorashare)**: You can now export and import your custom companion configurations or even entire chat sessions including text messages, level progress, and media (avatar PFP and background chat images are compressed and carried over automatically).
* 🏷️ **Tag & Meta System**: Companions now feature a dedicated tag system, tag-aware AI instructions, customized tags, descriptions, and creator author tags.

---

## Features
*(Everything is free to use. The only cost is what you pay directly to your API/TTS providers).*

* 🎭 **Custom AI Personas**: Customize name, avatar, system instructions, tone, and character lore.
* 👥 **Multi-Agent Chats**: Host 1 to 4 AI agents in a single room. Let them debate, or `@tag` them directly.
* 📂 **Project Templates**: Share instructions and memories across multiple sessions seamlessly.
* 🧠 **Global Memory**: Persistent, long-term memory that can be toggled on or off per chat.
* 📈 **Relationship XP (Level 1–10)**: Session-based progression. The AI gets warmer, more compliant, and unlocks NSFW boundaries as you chat (NSFW unlocks at Level 5). Defaults to ON for all conversations and can be toggled in the Roleplay UI settings.
* 💾 **Local-First Database**: Your chats survive app updates, restarts, and closures.
* 🛠️ **Session Manager**: Clone, rename, search, move, and delete chat rooms instantly.
* 🔌 **Multi-Provider API**: Native support for **Gemini (best support for Live features)**, Claude, GPT, Grok, and Mistral.
* 🔍 **Web Tools**: Live web search powered by Tavily.
* 🔞 **Anime Image Search**: Search and pull images directly into the chat via Rule34.
* 📸 **Visual Input**: Send up to 12 images (photos, live camera, or files) directly to the vision model.
* 🎙️ **Gemini Live Calling**: Real-time voice calls with camera and screenshare support.
* 📝 **Live Transcript**: Voice calls automatically compile into text bubbles and save to the chat history.
* 🗣️ **ElevenLabs TTS**: High-quality message narration with built-in local audio caching.
* 🎨 **Deep Customization**: Customize app themes, avatars, backgrounds, app name, and launcher icon.
* ⚙️ **Developer Vault**: Tweak API safety settings, temperature, and thinking effort values.
* ⏱️ **Quota Monitor**: Built-in local token counter and reset timer.
* 📚 **Soft TPM Archive**: Preserves long conversations by summarizing older context instead of deleting it when Gemini approaches its 250K TPM limit.
* 👆 **Message Context Menu**: Long-press any bubble to copy, edit, retry, or text-to-speech.

---

## Privacy Model
This repository does not include private API keys or the default master prompt.

### 100% Local Data:
* API keys entered in settings.
* Chat histories, messages, and database memories.
* Persona configs and user preferences.
* Cached TTS audio.
* Local quota counters.

Network requests only happen when sending a message or running an active tool. Data goes directly to the selected API provider (Google, Anthropic, OpenAI, Mistral, Tavily, Rule34, or ElevenLabs). No middleman servers involved.

---

## Master Prompt
The application looks for the private master prompt at:
```text
app/src/main/assets/local_master_prompt.txt
```
This file is ignored by `.gitignore`. To build the APK with your own custom rules:
1. Create the `local_master_prompt.txt` file locally in the assets directory.
2. Build the project.
If the file is missing, the build falls back to a basic default prompt in the code.

---

## API Providers & Keys
*Note: Zora performs best on **Gemini** (required for Live voice calls), followed by **Claude** and **Mistral**.*

* **Google AI Studio**: https://aistudio.google.com/app/apikey
* **Anthropic Console**: https://console.anthropic.com/settings/keys
* **OpenAI Developer**: https://platform.openai.com/api-keys
* **Mistral API Console**: https://console.mistral.ai/api-keys/
* **xAI (Grok) Console**: https://console.x.ai/
* **Tavily Search**: https://app.tavily.com/
* **ElevenLabs Dashboard**: https://elevenlabs.io/app/settings/api-keys
* **Rule34 (API Key & ID)**: https://rule34.xxx/index.php?page=account&s=options
  * *Note: Rule34 requires both the numeric User ID and the API Key from your account options page.*
  * *ElevenLabs TTS requires a Voice ID. Some custom voices may return a `402 Payment Required` code if your key doesn't have access to them.*

---

## Building from Source

### Requirements:
* Android Studio (Koala or newer) / Android SDK CLI tools.
* JDK 17.
* Target Device running Android 8.0 (API 26) or higher.
* `app/src/main/assets/local_master_prompt.txt` (optional, for custom system guidelines).

### Run Build:
For Release APK:
```powershell
.\gradlew.bat assembleZoraRelease
```

For Debug APK:
```powershell
.\gradlew.bat assembleZoraDebug
```

---

## Disclaimer & Licensing
* **Disclaimer**: This is a bring-your-own-key application. Model responses, safety refusals, rate limits, and tool execution depend entirely on the selected provider and API plan. The Rule34 tool is adult-only and should only be enabled where legal.
* **License**: MIT. Keep the credit, do whatever the fuck you want with the code.

---

## 🛠️ AI Development Stack
The codebase of Zora.AI is built using pair-programming with the following frontier LLMs:
* **Claude**: Haiku 4.5, Sonnet 4.6, Opus 4.6 & 4.8
* **Gemini**: 3.1 Pro, 3.5 Flash
* **Grok**: 4.3 (Fast & Heavy)
* **GPT**: 5.4 Mini, 5.5 xHigh

---

## 👑 Solo Leveling Credits (Project Team)
Since this app is a solo-leveled project, here is the organizational structure:
* **CEO / President**: Phan Chi Vy
* **Lead Architect**: Phan Chi Vy
* **UI/UX Designer**: Phan Chi Vy
* **QA & Tester**: Phan Chi Vy
* **Copywriter**: Phan Chi Vy
* **Coffee Maker**: Phan Chi Vy
* **Janitor / Code Cleaner**: Phan Chi Vy
* **Hype Man**: Phan Chi Vy

