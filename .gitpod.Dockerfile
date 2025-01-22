# Start from a standard Gitpod image that has most dev tools pre-installed
FROM gitpod/workspace-full:latest

# Switch to the "gitpod" user (already created in the base image),
# so we don't install things as root unnecessarily.
USER gitpod

# Install Java if not already present (some Gitpod base images have it)
RUN sudo apt-get update \
    && sudo apt-get install -y openjdk-17-jdk \
    && sudo apt-get clean

# Set environment variables for Android SDK location
ENV ANDROID_HOME=/home/gitpod/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools

# Create a directory for the command line tools and download them
RUN mkdir -p $ANDROID_HOME/cmdline-tools
WORKDIR $ANDROID_HOME/cmdline-tools
RUN wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O cmdline-tools.zip \
    && unzip cmdline-tools.zip -d latest \
    && rm cmdline-tools.zip

# Install required SDK packages. You can adjust versions as needed.
RUN yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_HOME \
    "platform-tools" \
    "platforms;android-33" \
    "build-tools;33.0.0"

# Accept all SDK licenses
RUN yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
