FROM mcr.microsoft.com/devcontainers/java:1-17-bookworm

ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        curl \
        git \
        jq \
        unzip \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspaces/BudgetFlow
