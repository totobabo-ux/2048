# Game 2048 - Android

A classic 2048 sliding puzzle game for Android, built with native Java.

## Features

- **다양한 난이도** — 4×4, 5×5, 6×6, 7×7, 8×8 보드 크기 선택
- **점수 기록** — 현재 점수 및 보드 크기별 최고 점수 저장
- **실행 취소** — 최대 10단계 되돌리기 지원
- **셔플** — 타일 위치를 무작위로 재배치
- **게임 상태 저장** — 앱 종료 후에도 게임 이어하기 가능
- **애니메이션** — 타일 이동·합치기 애니메이션

## Screenshots

> Coming soon

## Requirements

| 항목 | 버전 |
|------|------|
| Min SDK | Android 7.0 (API 24) |
| Target SDK | Android 15 (API 35) |
| Language | Java 8 |

## Getting Started

1. 저장소 클론

```bash
git clone https://github.com/totobabo-ux/2048.git
```

2. Android Studio에서 프로젝트 열기

3. Run 버튼 또는 아래 명령으로 빌드

```bash
./gradlew assembleDebug
```

## Project Structure

```
app/src/main/java/com/game2048/app/
├── MainActivity.java       # 난이도 선택 화면
├── GameActivity.java       # 게임 플레이 화면
├── GameModel.java          # 게임 로직 (이동, 합치기, 점수)
├── GameBoardView.java      # 보드 커스텀 뷰
├── BoardPreviewView.java   # 메인 화면 보드 미리보기
└── TileColors.java         # 타일 색상 정의
```

## License

MIT License
