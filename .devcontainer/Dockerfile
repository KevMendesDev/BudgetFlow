FROM mcr.microsoft.com/devcontainers/java:25-bookworm

ARG DEBIAN_FRONTEND=noninteractive

RUN rm -f /etc/apt/sources.list.d/yarn.list /etc/apt/sources.list.d/yarn.list.save \
    && find /etc/apt -type f -name "*.list" -exec sed -i '/dl\.yarnpkg\.com\/debian/d' {} + \
    && apt-get update \
    && apt-get install -y --no-install-recommends \
        curl \
        git \
        jq \
        unzip \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspaces/BudgetFlow
