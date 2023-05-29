FROM debian:sid-slim

RUN apt-get update -qq && \
    echo 'Installing OS dependencies' && \
    apt-get install -qq -y --fix-missing unzip openjdk-17-jdk wget gradle zip && \
    echo 'Cleaning up' && \
    apt-get clean -qq -y && \
    apt-get autoclean -qq -y && \
    apt-get autoremove -qq -y &&  \
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /tmp/* \

RUN echo 'Downloading IntelliJ IDEA' && \
    wget https://download-cdn.jetbrains.com/idea/ideaIC-2023.1.2.tar.gz -O /tmp/intellij.tar.gz -q && \
    echo 'Installing IntelliJ IDEA' && \
    mkdir -p /opt/intellij && \
    tar -xf /tmp/intellij.tar.gz --strip-components=1 -C /opt/intellij && \
    rm /tmp/intellij.tar.gz