# Gemma Chat App

An Android chatbot app powered by the Google Gemma 3 1B-int4 model, supporting fully on-device AI conversations.

## Features

- **Fully On-Device**: The model runs locally on your device, ensuring privacy and enabling offline usage.
- **Streaming Response**: Displays AI-generated text in real-time, providing a more natural interactive experience.
- **Clean UI**: Intuitive chat interface design focused on the conversation experience.
- **Efficient Inference**: Utilizes the INT4 quantized Gemma 3 1B model, balancing performance and quality.

## System Requirements

- Android 7.0 (API 24) or higher
- At least 4GB RAM, 6GB+ recommended
- Approximately 600MB of storage space (for the app and model files)
- Internet connection required for the first run to download the model

## Installation Guide

1. Download the latest APK from the [Releases page](https://github.com/yourusername/gemma-chat-app/releases).
2. Enable "Install unknown apps" on your Android device (in Settings > Security).
3. Install the downloaded APK file.
4. Upon the first launch, the app will automatically download the model file (approximately 529MB). Please ensure a Wi-Fi connection.

## Usage Instructions

1. Open the app and wait for the model to download and initialize.
2. Enter your question or message in the input box at the bottom.
3. Tap the send button to view the AI's real-time response.
4. Enjoy your conversation with the AI!

## Developer Information

The app is built using the following technologies:
- Kotlin + Jetpack Compose
- MediaPipe LLM Inference API
- Google Gemma 3 1B-int4 Model

## License

This project is licensed under the [Apache 2.0 License](LICENSE).