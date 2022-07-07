Quick start examples for integrating [Banuba SDK on Android](https://docs.banuba.com/face-ar-sdk/android/android_overview) in C++ apps.

# Getting Started

1. Clone the project to your computer:

    ```sh
        git clone --recursive https://github.com/Banuba/quickstart-android-c-api.git
    ```

2. Get the latest Banuba SDK archive for Android and the client token. Please fill in our form on [form on banuba.com](https://www.banuba.com/face-filters-sdk) website, or contact us via [info@banuba.com](mailto:info@banuba.com).
3. Copy `aar` and `include` files from the Banuba SDK archive into `libs` dir:
    `BNBEffectPlayer/include` => `quickstart-android-c-api/app/libs/include`
    `BNBEffectPlayer/banuba_effect_player_c_api-release.aar` => `quickstart-android-c-api/app/libs/banuba_effect_player_c_api-release.aar`
4. Copy and Paste your client token into appropriate section of `com/banuba/sdk/example/common/BanubaClientToken.kt`
5. Configure effect in the appropriate section of `com/banuba/sdk/example/quickstart_c_api/MainActivity.java`, e.g. effects/test_BG The effects folder should be located in the `app/src/main/assets/` folder. More effects can be found [here](https://docs.banuba.com/face-ar-sdk-v1/overview/demo_face_filters)
5. Open the project in Android Studio and run the necessary target using the usual steps.

# Contributing

Contributions are what make the open source community such an amazing place to be learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request
