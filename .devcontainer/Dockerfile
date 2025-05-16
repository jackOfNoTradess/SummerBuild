FROM eclipse-temurin:24-jdk

# Install dependencies
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    gnupg2 \
    git \
    software-properties-common \
    && rm -rf /var/lib/apt/lists/*

# Install Maven
RUN curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz | tar -xz -C /opt && \
    ln -s /opt/apache-maven-3.9.6 /opt/maven
ENV MAVEN_HOME=/opt/maven
ENV PATH="${MAVEN_HOME}/bin:${PATH}"

# Set working directory
WORKDIR /workspace
