FROM mcr.microsoft.com/devcontainers/base:ubuntu

# Install dependencies
RUN sudo apt-get update && apt-get install -y \
    curl \
    unzip \
    gnupg2 \
    git \
    software-properties-common \
    && rm -rf /var/lib/apt/lists/*

# Install OpenJDK 24
RUN mkdir -p /usr/lib/jvm && \
    curl -fsSL https://download.java.net/java/GA/jdk24/24.0.1/binaries/openjdk-24.0.1_linux-x64_bin.tar.gz | \
    tar -xz -C /usr/lib/jvm && \
    ln -s /usr/lib/jvm/jdk-24.0.1 /usr/lib/jvm/java-24-openjdk-amd64

ENV JAVA_HOME=/usr/lib/jvm/java-24-openjdk-amd64
ENV PATH="$JAVA_HOME/bin:$PATH"


# Install Maven
RUN curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz | tar -xz -C /opt && \
    ln -s /opt/apache-maven-3.9.6 /opt/maven
ENV MAVEN_HOME=/opt/maven
ENV PATH="${MAVEN_HOME}/bin:${PATH}"

# Set working directory
WORKDIR /workspaces