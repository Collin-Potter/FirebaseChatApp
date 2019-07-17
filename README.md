# FirebaseChatApp
Version 1.2.5

Application built to demonstrate simple chat functionality using Google Firestore

## Getting Started
These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. 

### Prerequisites
- Android Studio

### Installing and Running Application
- Install Android Studio

- Clone repository

- Open repositiory

- Click run with or without debug mode enabled

## Project Demo
### Log in through Google

Upon succussful Google login, user can navigate to recent activity list.
### Find a User

User can create a new message by selecting the option at the top right of the application. All existing users are displayed to choose from.

### Write a Message

Messages send data notifications via NodeJS through Google Firebase Messenger Services to keep users up to date with latest communication.
### Receive a Message

Upon recieving a message, user will be notified unless changed in Android Apps settings.
### Logout

Logout button navigates back to login page.
## Built With
- Android Studio's XML editor - For UI/UX
- Kotlin - The language involved
- Google Firebase - All essential database and notification functionality.

### NOTE: All users will persist upon creation. Application is currently in development with further functionality planned (i.e. group messaging, profile customization, etc...)

### Areas of the Application I'd like to approach differently next time:
#### - UI/UX:
  - Would like to attempt to tackle this challenge again with a pre-planned design in mind in order to have much more fluid UI and UX
#### - Notifications:
  - Notifications are currently utilizing topic subscriptions. Although it works, it is much more effective to communicate directly to registration tokens 
  
## Authors
- Collin Potter - Initial work

## Acknowledgments
- Built to improve understanding of Cloud-based communications and storage
