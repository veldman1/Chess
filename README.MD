# Chess

A lightweight, extensible Java-based chess application featuring:

- **Multiple game modes**: Human vs Bot, Human vs Human, Bot vs Bot  
- **Unicode piece rendering** with crisp, scalable board graphics  
- **PGN/FEN import & export**, full logging of moves  
- **Swing-based GUI** with optional screenshot below  

![Chess GUI Screenshot](./chess.png)

---

## Features

- **Model-View-Controller** design for clear separation of game logic, UI and control.
- **Human and Bot players**: simply drop in new Bot implementations.
- **Configurable game clock** and move timers.
- **Save & Load** game logs in PGN, export current position as FEN.
- **JUnit test suite** covering core rules, move legality, check/checkmate, en-passant, and more.

---

## Extensibility & Overhauls

- **Swappable Bots**: drop your own `Player` implementation under `botIntelligence/`.  
- **Modular GUI**: view and controller components communicate via minimal interfaces—feel free to replace Swing with JavaFX or web front-end.  
- **Robust logging**: PGN/FEN support remains irrespective of underlying rule changes.

---

## Contributing

1. Fork the repo  
2. Create a feature branch  
3. Write tests under `unitTests/`  
4. Submit a pull request with clear description