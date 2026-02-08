# 🎭 Possessor: Ultimate Social Deduction
**A Minecraft Forge 1.20.1 Mod for Paranoia & Deception**

Inspired by the greats of the social deduction genre, **Possessor** transforms your Minecraft server into a psychological battlefield. Designed for **4 to 12 players**, this mod places an invisible threat in your midst. Trust is a luxury; survival is a choice.

---

## 🎮 The Core Concept

The game revolves around a deadly game of cat and mouse:

* **The Possessor:** A secretly designated player whose mission is to eliminate all Innocents. They don't just kill; they **infiltrate**. By taking control of an Innocent's body, they can frame others, move undetected, and sow chaos from within.
* **The Innocents:** Their goal is simple—survive and identify the threat. They must use logic, observation, and teamwork to deduce who among them is no longer who they claim to be.

---

## 🚀 Key Features & Mechanics

### 👥 Advanced Identity Theft
The Possessor doesn't just hide; they replace. Integration with **SkinRestorer** allows the Possessor to instantly swap their skin and username to match their victim. 
> **Note:** This is an "instant" disguise, making it nearly impossible to tell the difference without witnessing the transformation.

### 🕵️ Anti-Cheat & Anonymity
To ensure the integrity of the game, the mod enforces strict anonymity:
* **Hidden Nametags:** You cannot see names through walls or above heads from a distance.
* **Obfuscated Tab List:** The player list (Tab) is randomized and hidden during the match to prevent players from checking who is "online" or "active."

### 🔄 Automated Gameplay Loop
The mod handles the heavy lifting through four distinct phases:
1.  **The Revelation:** Roles are assigned. The screen fades, and your destiny is revealed.
2.  **The Selection:** A brief tactical window where the Possessor selects their prey.
3.  **The Hunt (Gameplay):** Players explore the map, complete tasks (optional), and interact. The Possessor strikes when the time is right.
4.  **The Council (Voting):** When a body is found or suspicion peaks, players gather. In a unique twist, **voting is physical**: you cast your vote by striking the player you suspect.

### 👻 Ghost Mode (Spectators)
Death isn't the end of the fun. Eliminated players are automatically sent to a designated spectator area. From there, they can watch the drama unfold without the ability to "ghost" or leak information to the living.

---

## 🛠️ Configuration & Admin Commands

The mod is designed to be "plug-and-play" with minimal setup required.

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/possessor start` | OP | Initializes the game and assigns roles. |
| `/possessor stop` | OP | Force-ends the match and resets player skins. |
| `/possessor skip` | OP | Skips the current timer/phase (useful for slow voters). |
| `/tag <name> add noPlay` | Admin | Flags a player (like a cameraman) to be ignored by game logic. |

---

## 📦 Installation & Dependencies

To run **Possessor**, ensure your server and clients have the following installed:

* **Platform:** [Minecraft Forge 1.20.1](https://files.minecraftforge.net/)
* **Mandatory Dependency:** [SkinRestorer (Forge)](https://skinrestorer.net/) — *Crucial for the identity theft mechanic.*
* **Recommended:** A dedicated map with plenty of hiding spots and narrow corridors to maximize the tension.

---

### 💡 Pro-Tip for Admins
Use the `/tag @s add noPlay` command if you want to record the game as a "Spectator-Cam" without being selected as the Possessor or an Innocent!
