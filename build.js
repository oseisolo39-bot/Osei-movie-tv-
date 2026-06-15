const fs = require('fs');
const path = require('path');

console.log('--- Osei TV Build Orchestrator ---');

// Skip Android build unless specifically requested via environment variable
const shouldBuildAndroid = process.env.BUILD_ANDROID === 'true';

if (shouldBuildAndroid) {
    try {
        const { execSync } = require('child_process');
        console.log('Attempting Android build via Gradle...');
        execSync('gradle assembleDebug', { stdio: 'inherit' });
        console.log('Android build complete!');
    } catch (e) {
        console.warn('Android build failed. Please check gradle installation:', e.message);
    }
} else {
    console.log('Skipping Android APK build (Fast Web Mode). To build APK, set BUILD_ANDROID=true.');
}

// Always try to copy the APK to root if it exists in the build folder
const buildApk = path.join(__dirname, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');
const rootApk = path.join(__dirname, 'app-debug.apk');

if (fs.existsSync(buildApk)) {
    try {
        fs.copyFileSync(buildApk, rootApk);
        console.log('APK successfully copied to root for serving.');
    } catch (e) {
        console.error('Failed to copy APK:', e.message);
    }
} else if (fs.existsSync(rootApk)) {
    console.log('Existing APK already in root.');
} else {
    console.log('No compiled APK found. Download feature will be unavailable.');
}

console.log('Build process completed.');
