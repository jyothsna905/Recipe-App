# Recipe Finder Android App

An Android Studio starter project in Kotlin. It includes:
- Firebase Email/Password authentication (requires your Firebase project values)
- Camera photo capture and gallery picker
- Google ML Kit Image Labeling (food/ingredient label suggestions)
- TheMealDB ingredient search, name search, and recipe details
- RecyclerView recipe grid
- YouTube recipe link
- Local favourites saved on the device
- Basic no-results and network error screens

## Open the project
1. Extract `RecipeFinderAndroid.zip`.
2. Open Android Studio.
3. Choose **Open** and select the extracted `RecipeFinderAndroid` folder.
4. Use JDK 17 when prompted and allow Gradle sync.
5. Run on an Android device or emulator with internet access.

## Firebase setup (required for account login)
Firebase credentials are intentionally not included in this ZIP.
1. Create a project at https://console.firebase.google.com/
2. Add an Android app with package name `com.example.recipefinder`.
3. In Firebase Authentication > Sign-in method, enable **Email/Password**.
4. In Project settings > General, copy the Android app's:
   - API key
   - App ID
   - Project ID
   - Project number / sender ID
5. Replace the four placeholder values in:
   `app/src/main/res/values/strings.xml`
6. Rebuild and run.

This project initializes Firebase using those values directly, so a `google-services.json` file is not required.

## Important notes
- Ingredient detection uses **ML Kit Image Labeling**, which identifies objects/food labels. ML Kit Text Recognition reads text and is not suitable for recognizing food objects.
- TheMealDB free test key `1` is already configured in `RecipeApi.kt`.
- Favourites are currently stored locally with SharedPreferences. They are not synced between devices and are not stored in Firestore. Firebase Firestore is included as a dependency if you want to extend this.
- The app needs an internet connection to call TheMealDB and download recipe images.
- The camera preview flow uses Android's system camera intent. Gallery selection uses Android's system photo picker.
- This is a starter project; Android Studio/Gradle may download dependencies on first sync. It is not a prebuilt APK.

## Project details
- Package: `com.example.recipefinder`
- Language: Kotlin
- Min SDK: 23
- Compile SDK: 35
- Java/Kotlin target: 17
