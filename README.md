<div align="center">

<img width="512" height="512" alt="icont" src="https://github.com/user-attachments/assets/b5989583-56f1-41d7-b760-074e6f9c1053" />


# OpenUI Clock

**A modern, lightweight, and strictly private clock application for Android.**  
*Inspired by Samsung One UI aesthetics. Built with Kotlin and Jetpack Compose.*

---

<p>
  <a href="#english">English</a> •
  <a href="#русский">Русский</a>
</p>

---

</div>

<a name="english"></a>






<img width="1080" height="2226" alt="IMG_20260821_193139" src="https://github.com/user-attachments/assets/aa1e1efe-61d7-49b0-9248-b19a88c83482" />  <img width="1080" height="2228" alt="IMG_20260821_193153" src="https://github.com/user-attachments/assets/c353b681-b0fd-4700-820c-9c92e2b4fc95" />

<img width="1080" height="2220" alt="IMG_20260821_193203" src="https://github.com/user-attachments/assets/a2167b4e-f723-4e4a-ac5e-3dc07ce0f72b" /> <img width="1080" height="2232" alt="IMG_20260821_193216" src="https://github.com/user-attachments/assets/ede971b9-8184-4d48-bbd8-a2e625ca5aaf" />




## English

### Overview
**OpenUI Clock** is an open-source clock utility engineered for modern Android devices. Designed with a focus on speed, low resource consumption, and ergonomic single-handed operation, it provides alarms, world clock, stopwatch, and timer functionalities without bloat.

### Privacy Policy & Data Architecture
> **We do not collect any data. We have no interest in your data. Your data belongs to you.**

* **Zero Telemetry:** No analytics SDKs (Google Firebase, Google Analytics, AppMetrica, etc.).
* **Zero Advertising:** No ad frameworks or tracking identifiers.
* **100% Offline:** The application does not transmit data over the network. All alarms, preferences, and custom labels remain strictly on your device inside an encrypted local SQLite/Room database.
* **Hardware Efficiency:** Consumes 0 MB of RAM while running in the background. Alarm waking is handled via native hardware-assisted `AlarmManager` triggers.

### Key Features
* **Alarm Clock:** Repeating multi-day alarms, customizable snooze intervals, gradual volume ramp-up, and batch management with multi-select deletion.
* **World Clock:** Instant global time zone lookup with local time differences.
* **Stopwatch:** High-precision lap recording with split-time analysis.
* **Timer:** Background timers with customizable presets and sound alerts.
* **One UI Ergonomics:** Floating navigation controls positioned within thumb reach.

### Tech Stack
* **Language:** Kotlin
* **UI Framework:** Jetpack Compose & Material 3
* **Architecture:** MVVM / Clean Architecture with StateFlow
* **Database:** Room (SQLite)
* **Minimum SDK:** Android 8.0 (API 26)
* **Target SDK:** Android 15 (API 35)

---

<a name="русский"></a>
## Русский

### О проекте
**OpenUI Clock** — открытое, легковесное и конфиденциальное приложение часов для Android. Интерфейс выполнен в эргономичном стиле Samsung One UI с оптимизацией для удобного управления одной рукой.

### Конфиденциальность и безопасность
> **Мы вообще ничего о вас не собираем. Нам не нужны ваши данные. Ваши данные нужны вам.**

* **Полное отсутствие телеметрии:** Никаких трекеров (Firebase, AppMetrica, Яндекс, Google Analytics).
* **Без рекламы:** Никаких рекламных модулей или сбора рекламных идентификаторов.
* **Автономность:** Приложение не отправляет сетевых запросов. Все настройки, будильники и таймеры хранятся исключительно локально в защищенной базе данных Room на вашем устройстве.
* **Энергоэффективность:** 0 МБ оперативной памяти в фоновом режиме. Срабатывание будильников выполняется аппаратно через системный `AlarmManager`.

### Основные возможности
* **Будильник:** Гибкая настройка дней недели, интервалов повтора, нарастания громкости и режим массового удаления через мультивыбор.
* **Мировое время:** Поиск городов и часовых поясов с расчетом разницы во времени.
* **Секундомер:** Высокоточный замер времени с фиксацией кругов.
* **Таймер:** Фоновые таймеры с предустановленными пресетами.
* **Эргономика One UI:** Все важные элементы управления расположены в нижней части экрана.

### Технический стек
* **Язык разработки:** Kotlin
* **Интерфейс:** Jetpack Compose & Material 3
* **Архитектура:** MVVM с использованием Coroutines и Flow
* **Локальное хранилище:** Room (SQLite)
* **Минимальная версия:** Android 8.0 (API 26)
* **Целевая версия:** Android 15 (API 35)

---

### License
Distributed under the MIT License.
