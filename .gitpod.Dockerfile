# .gitpod.Dockerfile
FROM gitpod/workspace-full:latest

# Switch to the "gitpod" user (default in workspace-full)
USER gitpod

# 1) Install OpenJDK 17
RUN sudo apt-get update \
    && sudo apt-get install -y openjdk-17-jdk \
    && sudo apt-get clean

# 2) Set JAVA_HOME to the Java 17 install
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

# 3) Prepare Android SDK directories
ENV ANDROID_HOME=/home/gitpod/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools

RUN mkdir -p $ANDROID_HOME/cmdline-tools

# 4) Download & unzip the command line tools, then move them to "latest"
WORKDIR $ANDROID_HOME/cmdline-tools
RUN wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O cmdline-tools.zip \
    && unzip cmdline-tools.zip \
    && rm cmdline-tools.zip \
    && mv cmdline-tools $ANDROID_HOME/cmdline-tools/latest

# 5) Install required SDK packages + accept licenses
RUN yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_HOME \
    "platform-tools" \
    "platforms;android-33" \
    "build-tools;33.0.0" \
 && yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
