# ChatLogger

A modern Android messaging app with background SMS and WhatsApp logging capabilities.

## Features

- Real-time chat with text, image, and voice messages
- Background message logging for SMS and WhatsApp
- Firebase Authentication and Firestore integration
- Contact synchronization with phone contacts
- Dark mode support
- Modern Material Design UI

## Architecture

This application follows MVVM architecture with repository pattern:

- **Models**: Data classes for database entities
- **Views**: Activities and Fragments for UI
- **ViewModels**: Business logic and state management
- **Repositories**: Data access layer for Firebase and device

## Technologies

- Kotlin
- Firebase (Auth, Firestore, Storage)
- Coroutines for asynchronous operations
- LiveData and ViewModel for lifecycle management
- Navigation Component for fragment navigation
- Notification Listener Service for WhatsApp monitoring
- BroadcastReceiver for SMS processing
- Foreground Service for background operation

## Setup

1. Clone the repository
2. Create a Firebase project and add google-services.json to app/ directory
3. Enable Firebase Authentication (Email/Password), Firestore, and Storage
4. Build and run the application

## Permissions

The application requires several permissions:
- Internet access
- SMS reading
- Notification access
- Contacts access
- Foreground service
- Storage access (for media messages)

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Firebase for backend services
- Google's Material Design components
- Various open-source libraries used in this project
