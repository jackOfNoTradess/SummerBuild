FROM mcr.microsoft.com/devcontainers/base:ubuntu

# Install dependencies
RUN sudo apt-get update && apt-get install -y \
    curl \
    unzip \
    gnupg2 \
    git \
    python3 \
    python3-pip \
    software-properties-common \
    && pip3 install pre-commit \
    && rm -rf /var/lib/apt/lists/*

# Install OpenJDK 21 based on architecture
RUN mkdir -p /usr/lib/jvm && \
    ARCH=$(uname -m) && \
    if [ "$ARCH" = "x86_64" ]; then \
        echo "Detected x86_64 architecture" && \
        curl -fsSL https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.tar.gz | \
        tar -xz -C /usr/lib/jvm; \
    elif [ "$ARCH" = "aarch64" ] || [ "$ARCH" = "arm64" ]; then \
        echo "Detected ARM64 architecture" && \
        curl -fsSL https://download.oracle.com/java/21/latest/jdk-21_linux-aarch64_bin.tar.gz | \
        tar -xz -C /usr/lib/jvm; \
    else \
        echo "Unsupported architecture: $ARCH" && exit 1; \
    fi

RUN actual_jdk_dir=$(find /usr/lib/jvm -maxdepth 1 -name "jdk-*" -type d | head -n 1) && \
    ln -sf "$actual_jdk_dir" /usr/lib/jvm/java-21-openjdk-amd64

# Add this to your .bashrc to ensure it's always available
RUN echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' >> /etc/bash.bashrc && \
    echo 'export PATH=$JAVA_HOME/bin:$PATH' >> /etc/bash.bashrc

ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH="$JAVA_HOME/bin:$PATH"

# Install Maven
RUN curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz | tar -xz -C /opt && \
    ln -s /opt/apache-maven-3.9.6 /opt/maven
ENV MAVEN_HOME=/opt/maven
ENV PATH="${MAVEN_HOME}/bin:${PATH}"

# Install Node.js and npm
RUN curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
    && apt-get install -y nodejs

# Set working directory
WORKDIR /workspaces
