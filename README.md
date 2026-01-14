# ZeroKeep - Android (Formerly VaultGuard)

> **Zero-Knowledge. Headless. Obsidian Hardened.**

**ZeroKeep** is a high-security Android password manager tailored for the paranoid. It acts as a "Cold Wallet" for your credentials, ensuring that your Master Key never leaves your device's Trusted Execution Environment (TEE).

## 🔒 Security Architecture (Obsidian Level)

### 1. Zero-Knowledge Cryptography
*   **Hardware-Backed Storage**: Uses `AndroidKeyStore` with **StrongBox** (Titan M/Secure Element) enforcement.
*   **AES-256-GCM**: All data is encrypted/decrypted locally. The backend only sees opaque blobs.
*   **BIP39 Recovery**: Master Key is derived from a 12-word mnemonic using `Argon2id`.

### 2. Ultimate Hardening (Phase 4)
*   **💀 Self-Destruct (Kill Switch)**:
    *   The Master Key is **permanently wiped** from hardware after **7 consecutive failed login attempts**.
    *   Data becomes cryptographically irretrievable.
*   **🧠 RAM-Only Secrets**:
    *   Decrypted passwords in the UI are **auto-purged** from memory after **30 seconds**.
*   **🕵️ Anti-Forensics**:
    *   `FLAG_SECURE` prevents screenshots and screen recording.
    *   `allowBackup="false"` prevents ADB backups.
    *   `data_extraction_rules.xml` blocks cloud sync/transfer.
*   **🛡️ Advanced Tamper Detection**:
    *   App refuses to run if Root (`su`), Magisk, Frida, or Debuggers are detected.

### 3. Network Security
*   **SSL Pinning**: Pins the backend certificate SHA-256 hash to prevent Man-in-the-Middle (MitM) attacks.
*   **Client-Side VPN Detection**: Checks for active VPN tunnels to prevent accidental geo-fencing triggers.
*   **Device-Bound HMAC**: Every request is signed: `HMAC(API_KEY + Timestamp + UserAgent + DeviceID + Body)`.

## 🛠️ Project Structure

*   `security/SecurityManager.kt`: KeyStore, Encryption, Self-Destruct logic.
*   `network/HmacInterceptor.kt`: Automatic request signing.
*   `network/NetworkClient.kt`: SSL Pinning & VPN check.
*   `ui/`: Jetpack Compose screens (Setup, Auth, Dashboard).

## 🚀 Getting Started

1.  Clone the repository.
2.  Open in **Android Studio** (Hedgehog or newer).
3.  Set your Backend URL in `NetworkClient.kt`.
4.  Build and Run on a physical device (Emulator may fail Root checks).

---
*Built for the ZeroKeep Ecosystem.*
