FROM mcr.microsoft.com/devcontainers/base:ubuntu

# Install dependencies
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    gnupg2 \
    git \
    software-properties-common \
    && rm -rf /var/lib/apt/lists/*

# Install OpenJDK 24
RUN add-apt-repository ppa:openjdk-r/ppa && \
    apt-get update && \
    apt-get install -y openjdk-24-jdk

ENV JAVA_HOME=/usr/lib/jvm/java-24-openjdk-amd64
ENV PATH="$JAVA_HOME/bin:$PATH"

# Install Maven
RUN curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz | tar -xz -C /opt && \
    ln -s /opt/apache-maven-3.9.6 /opt/maven
ENV MAVEN_HOME=/opt/maven
ENV PATH="${MAVEN_HOME}/bin:${PATH}"

# Set working directory
WORKDIR /workspaces