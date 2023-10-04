# Name: user-service
# Desc: Creates a custom JRE image hosted on alpine for the backend JRE
# Author: Chris Forbes
# Version: 1.0
# Change Log:
# | Author       | Description of change | Version |
# | Chris Forbes | Initial Version       | 1.0     |

# ====================================================
# Base image splits the jar into layers
# ====================================================

FROM --platform=linux/amd64 public.ecr.aws/amazoncorretto/amazoncorretto:17 AS builder
WORKDIR application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

# ====================================================
# Create a copy of the corretto JRE using link
# ====================================================

FROM --platform=linux/amd64 public.ecr.aws/docker/library/alpine:latest AS jre-builder
RUN apk add --no-cache binutils openjdk17
RUN jlink \
    --verbose \
    --add-modules ALL-MODULE-PATH \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /customjre

# ====================================================
# Create our base image with the custom JRE
# ====================================================

FROM --platform=linux/amd64 public.ecr.aws/docker/library/alpine:latest
WORKDIR application
RUN apk add --no-cache freetype-dev fontconfig ttf-dejavu
ENV JAVA_HOME=/jre
ENV PATH=${JAVA_HOME}/bin:${PATH}
COPY --from=jre-builder /customjre $JAVA_HOME
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
# Necessary to prevent a layer not found exception, similar to issue described here - https://github.com/moby/moby/issues/33974
RUN true
COPY --from=builder application/application/ ./
EXPOSE 8080
ENTRYPOINT ["/jre/bin/java", "org.springframework.boot.loader.JarLauncher"]