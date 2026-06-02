FROM docker.io/library/eclipse-temurin:17-alpine
VOLUME /app/Answernator/db
VOLUME /app/Answernator/plugins
VOLUME /app/Answernator/cache
WORKDIR /app/Answernator
ENTRYPOINT ["/app/Answernator/bin/Answernator"]
ENV ANSWR4_COMMAND_HASH_CACHE=./cache/commands.cache \
    ANSWR4_DB_URL=jdbc:h2:./db/answernator \
    ANSWR4_DB_USER=root
ADD ./Answernator/build/distributions/Answernator.tgz /app
