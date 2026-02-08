package com.example.possessor.game;

public enum GamePhase {
    LOBBY,
    REVEAL,     // 15 seconds: Roles revealed, Possessor levitates
    SELECTION,  // 20 seconds: Possessor selects target
    PLAYING,    // 2 minutes 30 seconds: Gameplay
    VOTING,     // 1 minute 30 seconds: Voting
    ENDING      // Game over state
}
