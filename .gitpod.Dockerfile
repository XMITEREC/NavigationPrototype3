FROM gitpod/workspace-full:latest

# Use the "gitpod" user so we don't run everything as root
USER gitpod

# 1) Install Java if needed
RUN sudo apt-get update \
    && sudo apt-get install -y openjdk-17-jdk \
    && sudo apt-get clean

# 2) Environment variables for the Android SDK
ENV ANDROID_HOME=/home/gitpod/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools

# 3) Prepare a directory for the command line tools
RUN mkdir -p $ANDROID_HOME/cmdline-tools

# 4) Download & unzip the command line tools, then move them to "latest"
RUN wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O cmdline-tools.zip \
    && unzip cmdline-tools.zip -d $ANDROID_HOME/cmdline-tools \
    && rm cmdline-tools.zip \
    && mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest \
    \
    # 5) Now install core SDK packages
    && yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_HOME \
        "platform-tools" \
        "platforms;android-33" \
        "build-tools;33.0.0" \
    \
    # 6) Accept all licenses
    && yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
