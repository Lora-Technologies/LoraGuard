# LoraGuard

<div align="center">

## 🎉 FREE API KEY / ÜCRETSİZ API KEY 🎉

| | |
|---|---|
| **🌐 API Endpoint** | `api.loratech.dev` |
| **🔑 Free API Key** | `lora-free` |
| **💰 Price / Fiyat** | **FREE / ÜCRETSİZ** |

> Get started immediately with the free API key! No registration required.
> 
> Ücretsiz API key ile hemen başlayın! Kayıt gerekmez.

</div>

---

## 🤖 Available AI Models / Mevcut AI Modelleri

### 🆓 Free Models / Ücretsiz Modeller
| Provider | Model |
|----------|-------|
| **Google Gemini** | `gemini-2.5-flash-lite` |
| **xAI Grok** | `grok-4-1-fast-reasoning` |
| **OpenAI OSS** | `gpt-oss-120b` |

### 💎 Premium Models / Premium Modeller
| Provider | Models |
|----------|--------|
| **Google Gemini** | `gemini-3-pro`, `gemini-3-flash`, `gemini-2.5-pro`, `gemini-2.5-flash` |
| **xAI Grok** | `grok-4-1-fast-non-reasoning`, `grok-code-fast-1`, `grok-4-fast-reasoning`, `grok-4-fast-non-reasoning` |
| **Anthropic Claude** | `claude-4.5-sonnet`, `claude-4.5-haiku` |
| **Meta Llama** | `llama-4-maverick`, `llama-4-scout` |
| **OpenAI OSS** | `gpt-oss-20b` |
| **Kimi K2** | `kimi-k2` |

---

**Powered by Lora Technologies** - https://loratech.dev

---

# 🇬🇧 English

Advanced AI-powered chat moderation plugin for Minecraft servers.

## Features

- **AI Moderation**: Uses Lora Technologies API with 19 moderation categories
- **Local Filters**: Anti-spam, anti-flood, caps lock, link filter
- **Punishment System**: Escalating punishments (warn → mute → kick → ban)
- **GUI Admin Panel**: Easy-to-use graphical interface
- **Multi-language**: Turkish and English support
- **Discord Integration**: Webhook notifications for violations
- **PlaceholderAPI**: Full placeholder support
- **SQLite Database**: Violation history and player stats
- **Caching**: Reduce API costs with smart caching

## Requirements

- Java 21+
- Paper/Spigot 1.21+
- Lora Technologies API Key (Use `lora-free` for free access!)

## Installation

1. Download the latest release
2. Place `LoraGuard.jar` in your `plugins` folder
3. Restart your server
4. Edit `plugins/LoraGuard/config.yml` and add your API key (`lora-free`)
5. Run `/lg reload`

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/lg reload` | Reload configuration | `loraguard.admin` |
| `/lg toggle` | Enable/disable moderation | `loraguard.admin` |
| `/lg stats` | View statistics | `loraguard.admin` |
| `/lg history <player>` | View player violations | `loraguard.admin` |
| `/lg clear <player>` | Clear player history | `loraguard.admin` |
| `/lg whitelist <add/remove> <player>` | Manage whitelist | `loraguard.admin` |
| `/lg mute <player> [duration]` | Mute a player | `loraguard.admin` |
| `/lg unmute <player>` | Unmute a player | `loraguard.admin` |
| `/lg test <message>` | Test message moderation | `loraguard.admin` |
| `/lg setlang <tr/en>` | Change language | `loraguard.admin` |
| `/lg gui` | Open admin panel | `loraguard.gui` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `loraguard.admin` | Full admin access | op |
| `loraguard.bypass` | Bypass moderation | false |
| `loraguard.notify` | Receive violation alerts | op |
| `loraguard.gui` | Access GUI menu | op |

## PlaceholderAPI

| Placeholder | Description |
|-------------|-------------|
| `%loraguard_total_violations%` | Total violations |
| `%loraguard_today_violations%` | Today's violations |
| `%loraguard_status%` | Moderation status |
| `%loraguard_api_status%` | API status |
| `%loraguard_cache_size%` | Cache size |
| `%loraguard_player_violations%` | Player violation points |
| `%loraguard_player_muted%` | Player mute status |
| `%loraguard_player_mute_remaining%` | Mute remaining time |

## Moderation Categories

hate, violence, sexual, self_harm, harassment, profanity, spam, scam, toxicity, bullying, cheating, doxxing, advertising, threats, discrimination, illegal, inappropriate_username, griefing, irl_trading

---

# 🇹🇷 Türkçe

Minecraft sunucuları için gelişmiş yapay zeka destekli sohbet moderasyon eklentisi.

## Özellikler

- **AI Moderasyon**: 19 moderasyon kategorisi ile Lora Technologies API
- **Yerel Filtreler**: Anti-spam, anti-flood, büyük harf, link filtresi
- **Ceza Sistemi**: Kademeli cezalar (uyarı → susturma → atma → yasaklama)
- **GUI Yönetim Paneli**: Kullanımı kolay grafik arayüz
- **Çoklu Dil**: Türkçe ve İngilizce desteği
- **Discord Entegrasyonu**: İhlaller için webhook bildirimleri
- **PlaceholderAPI**: Tam placeholder desteği
- **SQLite Veritabanı**: İhlal geçmişi ve oyuncu istatistikleri
- **Önbellekleme**: Akıllı önbellekleme ile API maliyetlerini düşürün

## Gereksinimler

- Java 21+
- Paper/Spigot 1.21+
- Lora Technologies API Key (Ücretsiz erişim için `lora-free` kullanın!)

## Kurulum

1. Son sürümü indirin
2. `LoraGuard.jar` dosyasını `plugins` klasörüne koyun
3. Sunucunuzu yeniden başlatın
4. `plugins/LoraGuard/config.yml` dosyasını düzenleyin ve API key'inizi ekleyin (`lora-free`)
5. `/lg reload` komutunu çalıştırın

## Komutlar

| Komut | Açıklama | İzin |
|-------|----------|------|
| `/lg reload` | Yapılandırmayı yenile | `loraguard.admin` |
| `/lg toggle` | Moderasyonu aç/kapat | `loraguard.admin` |
| `/lg stats` | İstatistikleri görüntüle | `loraguard.admin` |
| `/lg history <oyuncu>` | Oyuncu ihlallerini görüntüle | `loraguard.admin` |
| `/lg clear <oyuncu>` | Oyuncu geçmişini temizle | `loraguard.admin` |
| `/lg whitelist <add/remove> <oyuncu>` | Beyaz listeyi yönet | `loraguard.admin` |
| `/lg mute <oyuncu> [süre]` | Oyuncuyu sustur | `loraguard.admin` |
| `/lg unmute <oyuncu>` | Oyuncunun susturmasını kaldır | `loraguard.admin` |
| `/lg test <mesaj>` | Mesaj moderasyonunu test et | `loraguard.admin` |
| `/lg setlang <tr/en>` | Dili değiştir | `loraguard.admin` |
| `/lg gui` | Yönetim panelini aç | `loraguard.gui` |

## İzinler

| İzin | Açıklama | Varsayılan |
|------|----------|------------|
| `loraguard.admin` | Tam yönetici erişimi | op |
| `loraguard.bypass` | Moderasyonu atla | false |
| `loraguard.notify` | İhlal uyarıları al | op |
| `loraguard.gui` | GUI menüsüne eriş | op |

## Moderasyon Kategorileri

nefret, şiddet, cinsel, kendine zarar, taciz, küfür, spam, dolandırıcılık, toksisite, zorbalık, hile, doxxing, reklam, tehdit, ayrımcılık, yasadışı, uygunsuz_kullanıcı_adı, griefing, gerçek_para_ticareti

---

## Building / Derleme

```bash
./gradlew build
```

Output / Çıktı: `build/libs/LoraGuard-1.0.0.jar`

## License / Lisans

Proprietary - Lora Technologies

## Support / Destek

- Discord: https://discord.gg/ak2wnTHaQq
- Website: https://loratech.dev
