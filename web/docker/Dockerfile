FROM openjdk:10-jre

RUN mkdir /app && mkdir /config

ADD ./*.jar /app/Homedash.jar

ADD run.sh /run.sh

EXPOSE 4567

VOLUME "/config"

RUN chmod +x /run.sh

CMD "./run.sh"