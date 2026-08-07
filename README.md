# 🏠 Keyes IoT Home Enhanced

> A modern Android reimplementation of the **Keyes IoT Home** application built with **Kotlin**, **Jetpack Compose**, and **MVVM** architecture.

Keyes IoT Home Enhanced allows users to monitor sensors and control actuators connected to a **Keyestudio ESP32 Smart Home Kit** over a local Wi-Fi network using a TCP socket connection.

This project was developed as a complete rewrite from scratch using modern Android development practices.

---

## ✨ Features

### Device Control

Control multiple smart home devices directly from your phone:

- 💡 LED
- 🪟 Window
- 🎵 Music Module
- 🔔 Whistle / Buzzer
- 🚪 Door
- 🌀 Fan

Each device can be turned on or off through an intuitive switch-based interface.

---

### Real-Time Sensor Monitoring

Display live data streamed from the ESP32:

- 🌧 Rainwater Sensor
- ☣ Harmful Gas Sensor
- 👤 PIR Motion Sensor
- 🌡 Temperature
- 💧 Humidity

Sensor values are updated continuously while connected.

---

### Connection Management

- Configurable ESP32 IP address
- Connect / Disconnect button
- Real-time connection status indicator
- Automatic cleanup on disconnection
- Network timeout handling

---

## 📱 User Interface

The application is built entirely with **Jetpack Compose** and **Material Design 3**.

Features include:

- Modern declarative UI
- Responsive layout
- Live state updates using StateFlow
- Visual connection indicator
- Smart device grid layout
- Real-time sensor dashboard

---

## 🔧 Technologies

### Android

- Kotlin
- Jetpack Compose
- Material 3
- ViewModel
- StateFlow
- Kotlin Coroutines

### Networking

- TCP Socket Communication
- Background IO Processing
- Automatic Connection Recovery

### IoT Hardware

- ESP32
- Keyestudio Smart Home Kit
- Environmental Sensors
- Smart Home Actuators

---

## 📡 ESP32 Communication

The application communicates directly with the ESP32 using a TCP socket.

### Default Port

```text
80
```

## 📥 Data Format

The ESP32 periodically sends sensor data in the following format:

```text
Rainwater,Gas,PIR
