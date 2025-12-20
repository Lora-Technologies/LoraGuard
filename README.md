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

### 🤖 AI Moderation
- **AI-Powered Analysis**: Uses Lora Technologies API with 19 moderation categories
- **Smart Caching**: Reduce API costs with intelligent message caching
- **Anti-Bypass Protection**: Leet speak (4=a, 3=e, 0=o) and special character normalization

### 🛡️ Content Filters
- **Chat Filters**: Anti-spam, anti-flood, caps lock filter
- **Link Filter**: Block URLs and domains with whitelist support
- **IP Filter**: Prevent IP address sharing (server advertisements)
- **Sign Moderation**: Monitor and filter sign text
- **Book Moderation**: Filter book content and titles
- **Anvil Moderation**: Block inappropriate item names
- **Command Spy**: Monitor private messages (/msg, /tell, /w, /r)

### ⚖️ Punishment System
- **Escalating Punishments**: warn → mute → kick → ban
- **Warning Decay**: Automatic warning point reduction over time
- **Appeal System**: Players can appeal their punishments

### 👮 Staff Tools
- **Staff Chat**: Private communication channel for staff members
- **Slowmode**: Control message frequency in chat
- **Bulk Operations**: Mass mute/unmute players
- **Export Data**: Export violations to JSON/CSV

### 🎛️ Management
- **GUI Admin Panel**: Easy-to-use graphical interface
- **Multi-language**: Turkish and English support (commands too!)
- **Discord Integration**: Webhook notifications for violations
- **PlaceholderAPI**: Full placeholder support
- **MySQL/SQLite**: Flexible database options
- **bStats Metrics**: Anonymous usage statistics

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

### Admin Commands (`/loraguard`, `/lg`)
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
| `/lg slowmode <on/off/set> [seconds]` | Manage slowmode | `loraguard.admin` |
| `/lg appeal <list/approve/deny>` | Manage appeals | `loraguard.admin` |
| `/lg export <all/player/stats>` | Export data | `loraguard.admin` |
| `/lg bulkmute [duration]` | Mass mute players | `loraguard.admin` |
| `/lg bulkunmute` | Mass unmute players | `loraguard.admin` |

### Player Commands
| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/report <player> [reason]` | `/raporla`, `/sikayet` | Report a player | `loraguard.report` |
| `/appeal <create/status/list>` | `/itiraz` | Appeal punishments | `loraguard.appeal` |
| `/clearchat` | `/cc`, `/temizle` | Clear global chat | `loraguard.clearchat` |

### Staff Commands
| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/staffchat [message]` | `/sc`, `/yetkili` | Staff chat channel | `loraguard.staffchat` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `loraguard.admin` | Full admin access | op |
| `loraguard.bypass` | Bypass moderation | false |
| `loraguard.notify` | Receive violation alerts | op |
| `loraguard.gui` | Access GUI menu | op |
| `loraguard.staffchat` | Access staff chat | op |
| `loraguard.appeal` | Appeal punishments | true |
| `loraguard.report` | Report players | true |
| `loraguard.clearchat` | Clear chat | op |
| `loraguard.commandspy` | See flagged private messages | op |

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

### 🤖 AI Moderasyon
- **AI Destekli Analiz**: 19 moderasyon kategorisi ile Lora Technologies API
- **Akıllı Önbellekleme**: Zeki mesaj önbellekleme ile API maliyetlerini düşürün
- **Anti-Bypass Koruması**: Leet speak (4=a, 3=e, 0=o) ve özel karakter normalizasyonu

### 🛡️ İçerik Filtreleri
- **Sohbet Filtreleri**: Anti-spam, anti-flood, büyük harf filtresi
- **Link Filtresi**: URL ve domain engelleme (whitelist desteği)
- **IP Filtresi**: IP adresi paylaşımını engelle (sunucu reklamları)
- **Tabela Moderasyonu**: Tabela metinlerini izle ve filtrele
- **Kitap Moderasyonu**: Kitap içeriği ve başlıklarını filtrele
- **Örs Moderasyonu**: Uygunsuz eşya isimlerini engelle
- **Komut İzleme**: Özel mesajları izle (/msg, /tell, /w, /r)

### ⚖️ Ceza Sistemi
- **Kademeli Cezalar**: uyarı → susturma → atma → yasaklama
- **Uyarı Azalması**: Zamanla otomatik uyarı puanı azaltma
- **İtiraz Sistemi**: Oyuncular cezalarına itiraz edebilir

### 👮 Yetkili Araçları
- **Yetkili Sohbeti**: Yetkili üyeler için özel iletişim kanalı
- **Yavaş Mod**: Sohbetteki mesaj sıklığını kontrol et
- **Toplu İşlemler**: Toplu susturma/susturmayı kaldırma
- **Veri Dışa Aktarma**: İhlalleri JSON/CSV olarak dışa aktar

### 🎛️ Yönetim
- **GUI Yönetim Paneli**: Kullanımı kolay grafik arayüz
- **Çoklu Dil**: Türkçe ve İngilizce desteği (komutlar dahil!)
- **Discord Entegrasyonu**: İhlaller için webhook bildirimleri
- **PlaceholderAPI**: Tam placeholder desteği
- **MySQL/SQLite**: Esnek veritabanı seçenekleri
- **bStats Metrikleri**: Anonim kullanım istatistikleri

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

### Yönetici Komutları (`/loraguard`, `/lg`, `/moderasyon`)
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
| `/lg slowmode <on/off/set> [saniye]` | Yavaş modu yönet | `loraguard.admin` |
| `/lg appeal <list/approve/deny>` | İtirazları yönet | `loraguard.admin` |
| `/lg export <all/player/stats>` | Verileri dışa aktar | `loraguard.admin` |
| `/lg bulkmute [süre]` | Toplu susturma | `loraguard.admin` |
| `/lg bulkunmute` | Toplu susturma kaldırma | `loraguard.admin` |

### Oyuncu Komutları
| Komut | Alternatifler | Açıklama | İzin |
|-------|---------------|----------|------|
| `/report <oyuncu> [sebep]` | `/raporla`, `/sikayet` | Oyuncu raporla | `loraguard.report` |
| `/appeal <create/status/list>` | `/itiraz` | Cezaya itiraz et | `loraguard.appeal` |
| `/clearchat` | `/cc`, `/temizle` | Sohbeti temizle | `loraguard.clearchat` |

### Yetkili Komutları
| Komut | Alternatifler | Açıklama | İzin |
|-------|---------------|----------|------|
| `/staffchat [mesaj]` | `/sc`, `/yetkili`, `/yetkilisohbet` | Yetkili sohbeti | `loraguard.staffchat` |

## İzinler

| İzin | Açıklama | Varsayılan |
|------|----------|------------|
| `loraguard.admin` | Tam yönetici erişimi | op |
| `loraguard.bypass` | Moderasyonu atla | false |
| `loraguard.notify` | İhlal uyarıları al | op |
| `loraguard.gui` | GUI menüsüne eriş | op |
| `loraguard.staffchat` | Yetkili sohbetine eriş | op |
| `loraguard.appeal` | Cezalara itiraz et | true |
| `loraguard.report` | Oyuncu raporla | true |
| `loraguard.clearchat` | Sohbeti temizle | op |
| `loraguard.commandspy` | Engellenen özel mesajları gör | op |

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
