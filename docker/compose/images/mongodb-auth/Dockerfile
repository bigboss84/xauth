FROM mongo:4.0

MAINTAINER <Enrico Russo> enrico.russo.84@gmail.com

ADD run.sh /run.sh
ADD set_mongodb_password.sh /set_mongodb_password.sh

RUN chmod +x /run.sh
RUN chmod +x /set_mongodb_password.sh

CMD "/run.sh"
