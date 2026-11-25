FROM eclipse-temurin:21-jre-jammy AS builder
WORKDIR /builder
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone

# The server runs as an unprivileged user and keeps everything below its
# home directory: the H2 database in ce-merging/, the task files in
# ce-merging-server/ and the PowSyBl configuration in .itools/. Mount
# volumes on the two data directories to keep them across containers.
# Group 0 gets the owner's rights, so the container may also run under
# another uid with group 0 (e.g. "docker run --user <uid>:0", OpenShift).
ENV HOME=/home/ce
COPY .itools /tmp/.itools
RUN useradd --system --uid 10001 --gid 0 --home-dir $HOME --create-home ce \
    && mkdir -p $HOME/ce-merging $HOME/ce-merging-server \
    && mv /tmp/.itools $HOME/.itools \
    && chown -R 10001:0 $HOME \
    && chmod -R g=u $HOME

COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./

USER 10001:0
EXPOSE 8080
# user.home pins the PowSyBl configuration directory, also for a uid
# without a passwd entry
ENTRYPOINT ["java", "-Duser.home=/home/ce", "-Duser.language=en", "-Duser.country=US", "-jar", "app.jar"]