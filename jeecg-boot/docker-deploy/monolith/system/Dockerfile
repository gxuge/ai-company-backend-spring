FROM eclipse-temurin:8-jre

MAINTAINER jeecgos@163.com

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

#RUN mkdir -p /jeecg-boot/config/jeecg/

WORKDIR /jeecg-boot

EXPOSE 8080

#ADD ./src/main/resources/jeecg ./config/jeecg
ADD ./jeecg-system-start.jar ./

CMD sleep 60;java -Djava.security.egd=file:/dev/./urandom -jar jeecg-system-start.jar


