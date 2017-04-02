FROM gradle:jdk8-alpine

# Dependencies
USER root
RUN apk update && apk add libstdc++ && rm -rf /var/cache/apk/*

# Copy project files
COPY . .

CMD gradle assemble

