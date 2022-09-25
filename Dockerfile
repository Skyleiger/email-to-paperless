FROM surnet/alpine-wkhtmltopdf:3.16.0-0.12.6-small as wkhtmltopdf
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Dominic Wienzek"

# Install dependencies for wkhtmltopdf
RUN apk add --no-cache \
  libstdc++ \
  libx11 \
  libxrender \
  libxext \
  libssl1.1 \
  ca-certificates \
  fontconfig \
  freetype \
  ttf-dejavu \
  ttf-droid \
  ttf-freefont \
  ttf-liberation \
&& apk add --no-cache --virtual .build-deps \
  msttcorefonts-installer \
\
# Install microsoft fonts
&& update-ms-fonts \
&& fc-cache -f \
\
# Clean up when done
&& rm -rf /tmp/* \
&& apk del .build-deps

# Copy wkhtmltopdf files from docker-wkhtmltopdf image
COPY --from=wkhtmltopdf /bin/wkhtmltopdf /bin/wkhtmltopdf

# Copy email-to-paperless files to image
COPY target/email-to-paperless.jar /email-to-paperless/email-to-paperless.jar

# Set working directory and entrypoint
WORKDIR /email-to-paperless
ENTRYPOINT java -jar email-to-paperless.jar